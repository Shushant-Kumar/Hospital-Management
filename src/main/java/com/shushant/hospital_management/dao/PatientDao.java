package com.shushant.hospital_management.dao;

import com.shushant.hospital_management.db.DatabaseConnection;
import com.shushant.hospital_management.util.AuditLogger;
import com.shushant.hospital_management.util.ValidationUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PatientDao {

    public int create(String patientUid, String firstName, String lastName, String email,
                      String phone, Date dob, String gender, String bloodGroup, String address,
                      String patientType, String allergies, String insuranceProvider,
                      String insurancePolicy, String emergencyContactName, String emergencyContactPhone,
                      int createdBy) {
        ValidationUtils.requireNonEmpty(patientUid, "Patient UID");
        ValidationUtils.requireNonEmpty(firstName, "First Name");
        ValidationUtils.requireNonEmpty(lastName, "Last Name");
        ValidationUtils.requireNonEmpty(phone, "Phone");

        String sql = """
            INSERT INTO patients (patient_uid, first_name, last_name, email, phone, date_of_birth,
                gender, blood_group, address, patient_type, allergies, insurance_provider,
                insurance_policy, emergency_contact_name, emergency_contact_phone, created_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, patientUid);
            ps.setString(2, firstName);
            ps.setString(3, lastName);
            ps.setString(4, email);
            ps.setString(5, phone);
            ps.setDate(6, dob);
            ps.setString(7, gender);
            ps.setString(8, bloodGroup);
            ps.setString(9, address);
            ps.setString(10, patientType != null ? patientType : "OPD");
            ps.setString(11, allergies);
            ps.setString(12, insuranceProvider);
            ps.setString(13, insurancePolicy);
            ps.setString(14, emergencyContactName);
            ps.setString(15, emergencyContactPhone);
            ps.setInt(16, createdBy);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int id = rs.getInt(1);
                AuditLogger.log("CREATE", "patients", id, "Patient " + firstName + " " + lastName + " (" + patientUid + ")");
                return id;
            }
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return -1;
    }

    /** Optimistic-locking update — checks updated_at to detect concurrent modifications. */
    public void update(int id, String firstName, String lastName, String email, String phone,
                       Date dob, String gender, String bloodGroup, String address,
                       String patientType, String allergies, String insuranceProvider,
                       String insurancePolicy, String emergencyContactName, String emergencyContactPhone,
                       Timestamp expectedUpdatedAt) {
        ValidationUtils.requireNonEmpty(firstName, "First Name");
        ValidationUtils.requireNonEmpty(lastName, "Last Name");
        ValidationUtils.requireNonEmpty(phone, "Phone");

        String sql;
        boolean useOptimisticLock = (expectedUpdatedAt != null);
        if (useOptimisticLock) {
            sql = """
                UPDATE patients SET first_name=?, last_name=?, email=?, phone=?, date_of_birth=?,
                    gender=?, blood_group=?, address=?, patient_type=?, allergies=?,
                    insurance_provider=?, insurance_policy=?, emergency_contact_name=?, emergency_contact_phone=?,
                    updated_at=CURRENT_TIMESTAMP
                WHERE id=? AND updated_at=?
            """;
        } else {
            sql = """
                UPDATE patients SET first_name=?, last_name=?, email=?, phone=?, date_of_birth=?,
                    gender=?, blood_group=?, address=?, patient_type=?, allergies=?,
                    insurance_provider=?, insurance_policy=?, emergency_contact_name=?, emergency_contact_phone=?,
                    updated_at=CURRENT_TIMESTAMP
                WHERE id=?
            """;
        }
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, firstName); ps.setString(2, lastName); ps.setString(3, email);
            ps.setString(4, phone); ps.setDate(5, dob); ps.setString(6, gender);
            ps.setString(7, bloodGroup); ps.setString(8, address); ps.setString(9, patientType);
            ps.setString(10, allergies); ps.setString(11, insuranceProvider);
            ps.setString(12, insurancePolicy); ps.setString(13, emergencyContactName);
            ps.setString(14, emergencyContactPhone); ps.setInt(15, id);
            if (useOptimisticLock) ps.setTimestamp(16, expectedUpdatedAt);

            int rows = ps.executeUpdate();
            if (useOptimisticLock && rows == 0) {
                throw new RuntimeException("Concurrent modification detected — patient record was modified by another user. Please refresh and try again.");
            }
            AuditLogger.log("UPDATE", "patients", id, "Patient details updated");
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
    }

    /** Backward-compatible update without optimistic locking */
    public void update(int id, String firstName, String lastName, String email, String phone,
                       Date dob, String gender, String bloodGroup, String address,
                       String patientType, String allergies, String insuranceProvider,
                       String insurancePolicy, String emergencyContactName, String emergencyContactPhone) {
        update(id, firstName, lastName, email, phone, dob, gender, bloodGroup, address,
               patientType, allergies, insuranceProvider, insurancePolicy,
               emergencyContactName, emergencyContactPhone, null);
    }

    /**
     * Cascading soft delete — deactivates patient AND:
     * 1. Releases any occupied bed
     * 2. Cancels non-completed appointments
     * 3. Cancels non-completed lab tests
     */
    public void delete(int id) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Soft-delete the patient
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE patients SET active = FALSE, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }

            // 2. Release any occupied bed
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE beds SET patient_id = NULL, status = 'AVAILABLE' WHERE patient_id = ? AND status = 'OCCUPIED'")) {
                ps.setInt(1, id);
                int released = ps.executeUpdate();
                if (released > 0) AuditLogger.log("CASCADE_RELEASE_BED", "beds", id, released + " bed(s) released for deactivated patient");
            }

            // 3. Cancel non-completed appointments
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE appointments SET status = 'CANCELLED', cancel_reason = 'Patient deactivated' WHERE patient_id = ? AND status IN ('BOOKED','CHECKED_IN','IN_PROGRESS')")) {
                ps.setInt(1, id);
                int cancelled = ps.executeUpdate();
                if (cancelled > 0) AuditLogger.log("CASCADE_CANCEL", "appointments", id, cancelled + " appointment(s) cancelled for deactivated patient");
            }

            // 4. Cancel non-completed lab tests
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE lab_tests SET status = 'CANCELLED' WHERE patient_id = ? AND status IN ('BOOKED','SAMPLE_COLLECTED','PROCESSING')")) {
                ps.setInt(1, id);
                int cancelled = ps.executeUpdate();
                if (cancelled > 0) AuditLogger.log("CASCADE_CANCEL", "lab_tests", id, cancelled + " lab test(s) cancelled for deactivated patient");
            }

            conn.commit();
            AuditLogger.log("DELETE", "patients", id, "Patient deactivated with cascading cleanup");

        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
            throw new RuntimeException("Database error during patient deletion", e);
        } finally {
            if (conn != null) { try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {} }
        }
    }

    public List<Object[]> findAll() {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT id, patient_uid, first_name, last_name, phone, gender, patient_type, created_at FROM patients WHERE active = TRUE ORDER BY id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("id"), rs.getString("patient_uid"),
                    rs.getString("first_name"), rs.getString("last_name"),
                    rs.getString("phone"), rs.getString("gender"),
                    rs.getString("patient_type"), rs.getTimestamp("created_at")
                });
            }
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return list;
    }

    public List<Object[]> findByDoctorId(int doctorId) {
        List<Object[]> list = new ArrayList<>();
        String sql = """
            SELECT DISTINCT p.id, p.patient_uid, p.first_name, p.last_name, p.phone, p.gender, p.patient_type, p.created_at
            FROM patients p
            JOIN appointments a ON p.id = a.patient_id
            WHERE a.doctor_id = ? AND p.active = TRUE
            ORDER BY p.id DESC
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("id"), rs.getString("patient_uid"),
                    rs.getString("first_name"), rs.getString("last_name"),
                    rs.getString("phone"), rs.getString("gender"),
                    rs.getString("patient_type"), rs.getTimestamp("created_at")
                });
            }
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return list;
    }

    public Object[] findById(int id) {
        String sql = "SELECT * FROM patients WHERE id = ? AND active = TRUE";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Object[]{
                    rs.getInt("id"), rs.getString("patient_uid"),
                    rs.getString("first_name"), rs.getString("last_name"),
                    rs.getString("email"), rs.getString("phone"),
                    rs.getDate("date_of_birth"), rs.getString("gender"),
                    rs.getString("blood_group"), rs.getString("address"),
                    rs.getString("patient_type"), rs.getString("allergies"),
                    rs.getString("insurance_provider"), rs.getString("insurance_policy"),
                    rs.getString("emergency_contact_name"), rs.getString("emergency_contact_phone")
                };
            }
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return null;
    }

    public List<Object[]> search(String query) {
        List<Object[]> list = new ArrayList<>();
        String sql = """
            SELECT id, patient_uid, first_name, last_name, phone, gender, patient_type, created_at
            FROM patients WHERE active = TRUE AND (
                LOWER(first_name) LIKE ? OR LOWER(last_name) LIKE ? 
                OR LOWER(patient_uid) LIKE ? OR phone LIKE ?
            ) ORDER BY id DESC
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String q = "%" + query.toLowerCase() + "%";
            ps.setString(1, q); ps.setString(2, q); ps.setString(3, q); ps.setString(4, q);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("id"), rs.getString("patient_uid"),
                    rs.getString("first_name"), rs.getString("last_name"),
                    rs.getString("phone"), rs.getString("gender"),
                    rs.getString("patient_type"), rs.getTimestamp("created_at")
                });
            }
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return list;
    }

    public List<Object[]> search(String query, int doctorId) {
        List<Object[]> list = new ArrayList<>();
        String sql = """
            SELECT DISTINCT p.id, p.patient_uid, p.first_name, p.last_name, p.phone, p.gender, p.patient_type, p.created_at
            FROM patients p
            JOIN appointments a ON p.id = a.patient_id
            WHERE a.doctor_id = ? AND p.active = TRUE AND (
                LOWER(p.first_name) LIKE ? OR LOWER(p.last_name) LIKE ? 
                OR LOWER(p.patient_uid) LIKE ? OR p.phone LIKE ?
            ) ORDER BY p.id DESC
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String q = "%" + query.toLowerCase() + "%";
            ps.setInt(1, doctorId);
            ps.setString(2, q); ps.setString(3, q); ps.setString(4, q); ps.setString(5, q);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("id"), rs.getString("patient_uid"),
                    rs.getString("first_name"), rs.getString("last_name"),
                    rs.getString("phone"), rs.getString("gender"),
                    rs.getString("patient_type"), rs.getTimestamp("created_at")
                });
            }
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return list;
    }

    /** Sequence-based, collision-free patient UID generation. */
    public String generatePatientUid() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT nextval('patient_uid_seq')")) {
            if (rs.next()) return "PAT-" + rs.getLong(1);
        } catch (SQLException e) { throw new RuntimeException("Database error generating patient UID", e); }
        return "PAT-" + System.currentTimeMillis(); // fallback
    }

    public boolean isAssignedToDoctor(int patientId, int doctorId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM appointments WHERE patient_id = ? AND doctor_id = ?")) {
            ps.setInt(1, patientId); ps.setInt(2, doctorId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return false;
    }

    public void linkUser(int patientId, int userId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE patients SET user_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            ps.setInt(1, userId);
            ps.setInt(2, patientId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
    }

    public int count() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM patients WHERE active = TRUE")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return 0;
    }

    public int findIdByUserId(int userId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id FROM patients WHERE user_id = ? AND active = TRUE")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return 0;
    }
}
