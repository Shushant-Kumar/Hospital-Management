package com.shushant.hospital_management.dao;

import com.shushant.hospital_management.db.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BillingDao {

    public int createBill(String billNumber, int patientId, double totalAmount, double discount,
                          double taxAmount, double netAmount, String billType) {
        String sql = """
            INSERT INTO bills (bill_number, patient_id, total_amount, discount, tax_amount, net_amount, bill_type)
            VALUES (?,?,?,?,?,?,?) RETURNING id
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, billNumber); ps.setInt(2, patientId);
            ps.setDouble(3, totalAmount); ps.setDouble(4, discount);
            ps.setDouble(5, taxAmount); ps.setDouble(6, netAmount);
            ps.setString(7, billType);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public void addBillItem(int billId, String description, int quantity, double unitPrice, double amount, String category) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO bill_items (bill_id, description, quantity, unit_price, amount, category) VALUES (?,?,?,?,?,?)")) {
            ps.setInt(1, billId); ps.setString(2, description); ps.setInt(3, quantity);
            ps.setDouble(4, unitPrice); ps.setDouble(5, amount); ps.setString(6, category);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void recordPayment(int billId, double amount, String method, String transactionRef) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
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
        } catch (SQLException e) { e.printStackTrace(); }
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
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public String generateBillNumber() {
        return "BILL-" + System.currentTimeMillis() % 1000000;
    }

    public double getTotalRevenue() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COALESCE(SUM(paid_amount), 0) FROM bills")) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public double getPendingPayments() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COALESCE(SUM(net_amount - paid_amount), 0) FROM bills WHERE status IN ('PENDING','PARTIAL')")) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }
}
