package com.shushant.hospital_management.dao;

import com.shushant.hospital_management.db.DatabaseConnection;
import com.shushant.hospital_management.util.AuditLogger;
import com.shushant.hospital_management.util.ValidationUtils;
import org.mindrot.jbcrypt.BCrypt;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public int create(String username, String password, String fullName, String email, String role) {
        ValidationUtils.requireNonEmpty(username, "Username");
        ValidationUtils.requireNonEmpty(password, "Password");
        ValidationUtils.requireNonEmpty(fullName, "Full Name");
        ValidationUtils.requireNonEmpty(email, "Email");
        ValidationUtils.requireNonEmpty(role, "Role");

        String sql = "INSERT INTO users (username, password_hash, full_name, email, role) VALUES (?,?,?,?,?) RETURNING id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, BCrypt.hashpw(password, BCrypt.gensalt()));
            ps.setString(3, fullName); ps.setString(4, email); ps.setString(5, role);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return -1;
    }

    /**
     * Transactional patient user creation — creates user + patient + links them
     * all within one transaction. Rolls back fully on any failure.
     *
     * @return int array: [userId, patientId]
     */
    public int[] createPatientUser(String username, String password, String firstName, String lastName,
                                    String email, String phone, Date dob, String gender) {
        ValidationUtils.requireNonEmpty(username, "Username");
        ValidationUtils.requireNonEmpty(password, "Password");
        ValidationUtils.requireNonEmpty(firstName, "First Name");
        ValidationUtils.requireNonEmpty(lastName, "Last Name");
        ValidationUtils.requireNonEmpty(email, "Email");
        ValidationUtils.requireNonEmpty(phone, "Phone");

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Check for duplicate username/email
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT 1 FROM users WHERE username = ? OR email = ?")) {
                ps.setString(1, username); ps.setString(2, email);
                if (ps.executeQuery().next()) {
                    throw new IllegalStateException("Username or email is already taken.");
                }
            }

            // 2. Generate patient UID from sequence
            String patientUid;
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT nextval('patient_uid_seq')")) {
                patientUid = rs.next() ? "PAT-" + rs.getLong(1) : "PAT-" + System.currentTimeMillis();
            }

            // 3. Create user
            int userId;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO users (username, password_hash, full_name, email, role) VALUES (?,?,?,?,?) RETURNING id")) {
                ps.setString(1, username);
                ps.setString(2, BCrypt.hashpw(password, BCrypt.gensalt()));
                ps.setString(3, firstName + " " + lastName);
                ps.setString(4, email); ps.setString(5, "PATIENT");
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) throw new RuntimeException("Failed to create user account.");
                userId = rs.getInt(1);
            }

            // 4. Create patient linked to user
            int patientId;
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO patients (patient_uid, first_name, last_name, email, phone, date_of_birth,
                        gender, patient_type, user_id, created_by) VALUES (?,?,?,?,?,?,?,?,?,?) RETURNING id
                """)) {
                ps.setString(1, patientUid); ps.setString(2, firstName); ps.setString(3, lastName);
                ps.setString(4, email); ps.setString(5, phone); ps.setDate(6, dob);
                ps.setString(7, gender); ps.setString(8, "OPD");
                ps.setInt(9, userId); ps.setInt(10, userId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) throw new RuntimeException("Failed to create patient record.");
                patientId = rs.getInt(1);
            }

            conn.commit();
            AuditLogger.log("CREATE", "users", userId, "Patient user registered: " + username);
            AuditLogger.log("CREATE", "patients", patientId, "Patient " + firstName + " " + lastName + " (" + patientUid + ")");
            return new int[]{ userId, patientId };

        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            throw new RuntimeException("Database error during patient registration", e);
        } catch (IllegalStateException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            throw e;
        } finally {
            if (conn != null) { try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {} }
        }
    }

    public boolean exists(String username, String email) {
        String sql = "SELECT 1 FROM users WHERE username = ? OR email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
    }

    public List<Object[]> findAll() {
        List<Object[]> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, username, full_name, email, role, active, created_at FROM users ORDER BY id")) {
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("id"), rs.getString("username"), rs.getString("full_name"),
                    rs.getString("email"), rs.getString("role"), rs.getBoolean("active"),
                    rs.getTimestamp("created_at")
                });
            }
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return list;
    }

    public void toggleActive(int id, boolean active) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE users SET active = ? WHERE id = ?")) {
            ps.setBoolean(1, active); ps.setInt(2, id);
            ps.executeUpdate();
            AuditLogger.log("UPDATE_STATUS", "users", id, "User status set to active: " + active);
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
    }

    public void resetPassword(int id, String newPassword) {
        ValidationUtils.requireNonEmpty(newPassword, "New Password");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE users SET password_hash = ? WHERE id = ?")) {
            ps.setString(1, BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            ps.setInt(2, id);
            ps.executeUpdate();
            AuditLogger.log("RESET_PASSWORD", "users", id, "Password reset by admin");
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
    }
}
