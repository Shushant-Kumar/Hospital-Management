package com.shushant.hospital_management.dao;

import com.shushant.hospital_management.db.DatabaseConnection;
import com.shushant.hospital_management.util.AuditLogger;
import com.shushant.hospital_management.util.ValidationUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BillingDao {

    public int createBill(String billNumber, int patientId, double totalAmount, double discount,
                          double taxAmount, double netAmount, String billType, int createdBy) {
        ValidationUtils.requireNonEmpty(billNumber, "Bill Number");
        ValidationUtils.requirePositiveInt(patientId, "Patient ID");
        ValidationUtils.requireNonNegative(totalAmount, "Total Amount");
        ValidationUtils.requireNonNegative(discount, "Discount");
        ValidationUtils.requireNonNegative(taxAmount, "Tax Amount");
        ValidationUtils.requireNonNegative(netAmount, "Net Amount");

        // Validate net amount consistency
        double expectedNet = totalAmount - discount + taxAmount;
        if (Math.abs(netAmount - expectedNet) > 0.01) {
            throw new IllegalArgumentException(
                "Net amount (%.2f) does not match total - discount + tax (%.2f).".formatted(netAmount, expectedNet));
        }

        String sql = """
            INSERT INTO bills (bill_number, patient_id, total_amount, discount, tax_amount, net_amount, bill_type, created_by)
            VALUES (?,?,?,?,?,?,?,?) RETURNING id
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, billNumber); ps.setInt(2, patientId);
            ps.setDouble(3, totalAmount); ps.setDouble(4, discount);
            ps.setDouble(5, taxAmount); ps.setDouble(6, netAmount);
            ps.setString(7, billType); ps.setInt(8, createdBy);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int id = rs.getInt(1);
                AuditLogger.log("CREATE", "bills", id, "Bill " + billNumber + " for patient " + patientId + " net=" + netAmount);
                return id;
            }
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return -1;
    }

    public void recordPayment(int billId, double amount, String method, String transactionRef) {
        ValidationUtils.requirePositiveInt(billId, "Bill ID");
        ValidationUtils.requirePositive(amount, "Payment Amount");
        ValidationUtils.requireNonEmpty(method, "Payment Method");

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Verify bill exists and check overpayment
            double netAmount, paidAmount;
            String currentStatus;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT net_amount, paid_amount, status FROM bills WHERE id = ? FOR UPDATE")) {
                ps.setInt(1, billId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) throw new IllegalStateException("Bill ID " + billId + " not found.");
                netAmount = rs.getDouble("net_amount");
                paidAmount = rs.getDouble("paid_amount");
                currentStatus = rs.getString("status");
            }

            if ("PAID".equals(currentStatus)) {
                throw new IllegalStateException("Bill is already fully paid.");
            }
            if ("CANCELLED".equals(currentStatus)) {
                throw new IllegalStateException("Cannot record payment on a cancelled bill.");
            }
            if (paidAmount + amount > netAmount) {
                throw new IllegalStateException("Payment of %.2f would exceed net amount. Remaining: %.2f".formatted(
                        amount, netAmount - paidAmount));
            }

            try (PreparedStatement ps1 = conn.prepareStatement(
                    "INSERT INTO payments (bill_id, amount, payment_method, transaction_ref) VALUES (?,?,?,?)")) {
                ps1.setInt(1, billId); ps1.setDouble(2, amount);
                ps1.setString(3, method); ps1.setString(4, transactionRef);
                ps1.executeUpdate();
            }
            try (PreparedStatement ps2 = conn.prepareStatement(
                    "UPDATE bills SET paid_amount = paid_amount + ?, status = CASE WHEN paid_amount + ? >= net_amount THEN 'PAID' ELSE 'PARTIAL' END WHERE id = ?")) {
                ps2.setDouble(1, amount); ps2.setDouble(2, amount); ps2.setInt(3, billId);
                ps2.executeUpdate();
            }
            conn.commit();
            AuditLogger.log("RECORD_PAYMENT", "bills", billId, "Amount=" + amount + " method=" + method);

        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException re) { System.err.println("Rollback failed: " + re.getMessage()); } }
            throw new RuntimeException("Database error during payment recording", e);
        } catch (IllegalStateException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            throw e;
        } finally {
            if (conn != null) { try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {} }
        }
    }

    public List<Object[]> findAll() {
        List<Object[]> list = new ArrayList<>();
        String sql = """
            SELECT b.id, b.bill_number, p.first_name || ' ' || p.last_name AS patient,
                   b.net_amount, b.paid_amount, b.status, b.bill_type, b.created_at
            FROM bills b JOIN patients p ON b.patient_id = p.id
            ORDER BY b.created_at DESC
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("id"), rs.getString("bill_number"), rs.getString("patient"),
                    rs.getDouble("net_amount"), rs.getDouble("paid_amount"),
                    rs.getString("status"), rs.getString("bill_type"), rs.getTimestamp("created_at")
                });
            }
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return list;
    }

    /** Sequence-based, collision-free bill number generation. */
    public String generateBillNumber() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT nextval('bill_number_seq')")) {
            if (rs.next()) return "BILL-" + rs.getLong(1);
        } catch (SQLException e) { throw new RuntimeException("Database error generating bill number", e); }
        return "BILL-" + System.currentTimeMillis(); // fallback
    }

    public double getTotalRevenue() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COALESCE(SUM(paid_amount), 0) FROM bills")) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return 0;
    }

    public double getPendingPayments() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COALESCE(SUM(net_amount - paid_amount), 0) FROM bills WHERE status IN ('PENDING','PARTIAL')")) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return 0;
    }

    /** Find bills for a specific patient */
    public List<Object[]> findByPatientId(int patientId) {
        List<Object[]> list = new ArrayList<>();
        String sql = """
            SELECT b.id, b.bill_number, p.first_name || ' ' || p.last_name AS patient,
                   b.net_amount, b.paid_amount, b.status, b.bill_type, b.created_at
            FROM bills b JOIN patients p ON b.patient_id = p.id
            WHERE b.patient_id = ?
            ORDER BY b.created_at DESC
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("id"), rs.getString("bill_number"), rs.getString("patient"),
                    rs.getDouble("net_amount"), rs.getDouble("paid_amount"),
                    rs.getString("status"), rs.getString("bill_type"), rs.getTimestamp("created_at")
                });
            }
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return list;
    }
}
