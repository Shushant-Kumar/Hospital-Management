package com.shushant.hospital_management.dao;

import com.shushant.hospital_management.db.DatabaseConnection;
import com.shushant.hospital_management.util.AuditLogger;
import com.shushant.hospital_management.util.ValidationUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DoctorDao {

    public int create(String firstName, String lastName, String email, String phone,
                      String specialization, String licenseNumber, int departmentId,
                      double consultationFee, int consultationDurationMin) {
        ValidationUtils.requireNonEmpty(firstName, "First Name");
        ValidationUtils.requireNonEmpty(lastName, "Last Name");
        ValidationUtils.requireNonEmpty(email, "Email");
        ValidationUtils.requireNonEmpty(phone, "Phone");
        ValidationUtils.requireNonEmpty(specialization, "Specialization");
        ValidationUtils.requireNonEmpty(licenseNumber, "License Number");
        ValidationUtils.requireNonNegative(consultationFee, "Consultation Fee");
        ValidationUtils.requirePositiveInt(consultationDurationMin, "Consultation Duration");

        String sql = """
            INSERT INTO doctors (first_name, last_name, email, phone, specialization,
                license_number, department_id, consultation_fee, consultation_duration_min)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, firstName); ps.setString(2, lastName); ps.setString(3, email);
            ps.setString(4, phone); ps.setString(5, specialization);
            ps.setString(6, licenseNumber); ps.setInt(7, departmentId);
            ps.setDouble(8, consultationFee); ps.setInt(9, consultationDurationMin);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int id = rs.getInt(1);
                AuditLogger.log("CREATE", "doctors", id, "Dr. " + firstName + " " + lastName);
                return id;
            }
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return -1;
    }

    public void update(int id, String firstName, String lastName, String email, String phone,
                       String specialization, int departmentId, double consultationFee,
                       int consultationDurationMin) {
        ValidationUtils.requireNonEmpty(firstName, "First Name");
        ValidationUtils.requireNonEmpty(lastName, "Last Name");

        String sql = """
            UPDATE doctors SET first_name=?, last_name=?, email=?, phone=?, specialization=?,
                department_id=?, consultation_fee=?, consultation_duration_min=?, updated_at=CURRENT_TIMESTAMP WHERE id=?
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, firstName); ps.setString(2, lastName); ps.setString(3, email);
            ps.setString(4, phone); ps.setString(5, specialization);
            ps.setInt(6, departmentId); ps.setDouble(7, consultationFee);
            ps.setInt(8, consultationDurationMin); ps.setInt(9, id);
            ps.executeUpdate();
            AuditLogger.log("UPDATE", "doctors", id, "Doctor details updated");
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
    }

    /**
     * Cascading soft delete — deactivates doctor AND:
     * 1. Cancels all future/pending appointments
     * 2. Cancels BOOKED lab tests ordered by this doctor
     */
    public void delete(int id) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Soft-delete the doctor
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE doctors SET active = FALSE, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            // 2. Cancel pending/future appointments
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE appointments SET status = 'CANCELLED', cancel_reason = 'Doctor deactivated' WHERE doctor_id = ? AND status IN ('BOOKED','CHECKED_IN')")) {
                ps.setInt(1, id);
                int cancelled = ps.executeUpdate();
                if (cancelled > 0) AuditLogger.log("CASCADE_CANCEL", "appointments", id, cancelled + " appointment(s) cancelled for deactivated doctor");
            }

            // 3. Cancel BOOKED lab tests
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE lab_tests SET status = 'CANCELLED' WHERE doctor_id = ? AND status IN ('BOOKED','SAMPLE_COLLECTED')")) {
                ps.setInt(1, id);
                int cancelled = ps.executeUpdate();
                if (cancelled > 0) AuditLogger.log("CASCADE_CANCEL", "lab_tests", id, cancelled + " lab test(s) cancelled for deactivated doctor");
            }

            conn.commit();
            AuditLogger.log("DELETE", "doctors", id, "Doctor deactivated with cascading cleanup");

        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            throw new RuntimeException("Database error during doctor deletion", e);
        } finally {
            if (conn != null) { try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {} }
        }
    }

    public List<Object[]> findAll() {
        List<Object[]> list = new ArrayList<>();
        String sql = """
            SELECT d.id, d.first_name, d.last_name, d.specialization, d.phone,
                   dep.name AS dept, d.consultation_fee
            FROM doctors d LEFT JOIN departments dep ON d.department_id = dep.id
            WHERE d.active = TRUE ORDER BY d.id DESC
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("id"), rs.getString("first_name"), rs.getString("last_name"),
                    rs.getString("specialization"), rs.getString("phone"),
                    rs.getString("dept"), rs.getDouble("consultation_fee")
                });
            }
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return list;
    }

    public Object[] findById(int id) {
        String sql = "SELECT * FROM doctors WHERE id = ? AND active = TRUE";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Object[]{
                    rs.getInt("id"), rs.getString("first_name"), rs.getString("last_name"),
                    rs.getString("email"), rs.getString("phone"), rs.getString("specialization"),
                    rs.getString("license_number"), rs.getInt("department_id"),
                    rs.getDouble("consultation_fee"), rs.getInt("consultation_duration_min")
                };
            }
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return null;
    }

    public List<String[]> findAllForCombo() {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT id, first_name || ' ' || last_name AS name FROM doctors WHERE active = TRUE ORDER BY first_name";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new String[]{ String.valueOf(rs.getInt("id")), rs.getString("name") });
            }
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return list;
    }

    public int count() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM doctors WHERE active = TRUE")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return 0;
    }

    public int findIdByUserId(int userId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id FROM doctors WHERE user_id = ? AND active = TRUE")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return 0;
    }
}
