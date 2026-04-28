package com.shushant.hospital_management.dao;

import com.shushant.hospital_management.db.DatabaseConnection;
import com.shushant.hospital_management.util.AuditLogger;
import com.shushant.hospital_management.util.ValidationUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LabTestDao {

    // ── Valid state transitions ──────────────────────────────────────────────
    private static final Map<String, Set<String>> VALID_TRANSITIONS = Map.of(
        "BOOKED",           Set.of("SAMPLE_COLLECTED", "CANCELLED"),
        "SAMPLE_COLLECTED", Set.of("PROCESSING", "CANCELLED"),
        "PROCESSING",       Set.of("COMPLETED", "CANCELLED"),
        "COMPLETED",        Set.of(),
        "CANCELLED",        Set.of()
    );

    public int create(int patientId, int doctorId, String testName, String testCode, String sampleType, int createdBy) {
        ValidationUtils.requirePositiveInt(patientId, "Patient ID");
        ValidationUtils.requirePositiveInt(doctorId, "Doctor ID");
        ValidationUtils.requireNonEmpty(testName, "Test Name");

        String sql = """
            INSERT INTO lab_tests (patient_id, doctor_id, test_name, test_code, sample_type, created_by)
            VALUES (?,?,?,?,?,?) RETURNING id
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId); ps.setInt(2, doctorId); ps.setString(3, testName);
            ps.setString(4, testCode); ps.setString(5, sampleType); ps.setInt(6, createdBy);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int id = rs.getInt(1);
                AuditLogger.log("CREATE", "lab_tests", id, "Test " + testName + " for patient=" + patientId);
                return id;
            }
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return -1;
    }

    /** State-machine validated status update. */
    public void updateStatus(int id, String newStatus) {
        ValidationUtils.requireNonEmpty(newStatus, "Status");

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            String currentStatus;
            try (PreparedStatement ps = conn.prepareStatement("SELECT status FROM lab_tests WHERE id = ? FOR UPDATE")) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) throw new IllegalStateException("Lab test ID " + id + " not found.");
                currentStatus = rs.getString("status");
            }

            Set<String> allowed = VALID_TRANSITIONS.get(currentStatus);
            if (allowed == null || !allowed.contains(newStatus)) {
                throw new IllegalStateException("Invalid lab test status transition: " + currentStatus + " → " + newStatus);
            }

            try (PreparedStatement ps = conn.prepareStatement("UPDATE lab_tests SET status = ? WHERE id = ?")) {
                ps.setString(1, newStatus); ps.setInt(2, id);
                ps.executeUpdate();
            }

            conn.commit();
            AuditLogger.log("UPDATE_STATUS", "lab_tests", id, currentStatus + " → " + newStatus);

        } catch (SQLException e) {
            rollback(conn);
            throw new RuntimeException("Database error", e);
        } catch (IllegalStateException e) {
            rollback(conn);
            throw e;
        } finally {
            closeConnection(conn);
        }
    }

    public void saveResult(int id, String result, String normalRange, String technicianName) {
        ValidationUtils.requireNonEmpty(result, "Result");

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Verify test is in PROCESSING state
            String currentStatus;
            try (PreparedStatement ps = conn.prepareStatement("SELECT status FROM lab_tests WHERE id = ? FOR UPDATE")) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) throw new IllegalStateException("Lab test ID " + id + " not found.");
                currentStatus = rs.getString("status");
            }

            if (!"PROCESSING".equals(currentStatus)) {
                throw new IllegalStateException("Cannot enter result — test must be in PROCESSING state (current: " + currentStatus + ").");
            }

            try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE lab_tests SET result = ?, normal_range = ?, technician_name = ?, status = 'COMPLETED' WHERE id = ?")) {
                ps.setString(1, result); ps.setString(2, normalRange);
                ps.setString(3, technicianName); ps.setInt(4, id);
                ps.executeUpdate();
            }

            conn.commit();
            AuditLogger.log("ENTER_RESULT", "lab_tests", id, "Result entered by " + technicianName);

        } catch (SQLException e) {
            rollback(conn);
            throw new RuntimeException("Database error", e);
        } catch (IllegalStateException e) {
            rollback(conn);
            throw e;
        } finally {
            closeConnection(conn);
        }
    }

    public List<Object[]> findAll() {
        List<Object[]> list = new ArrayList<>();
        String sql = """
            SELECT lt.id, p.first_name || ' ' || p.last_name AS patient,
                   d.first_name || ' ' || d.last_name AS doctor,
                   lt.test_name, lt.test_code, lt.status, lt.created_at
            FROM lab_tests lt
            JOIN patients p ON lt.patient_id = p.id
            JOIN doctors d ON lt.doctor_id = d.id
            ORDER BY lt.created_at DESC
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("id"), rs.getString("patient"), rs.getString("doctor"),
                    rs.getString("test_name"), rs.getString("test_code"),
                    rs.getString("status"), rs.getTimestamp("created_at")
                });
            }
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return list;
    }

    public Object[] findById(int id) {
        String sql = """
            SELECT lt.*, p.first_name || ' ' || p.last_name AS patient_name,
                   d.first_name || ' ' || d.last_name AS doctor_name
            FROM lab_tests lt
            JOIN patients p ON lt.patient_id = p.id
            JOIN doctors d ON lt.doctor_id = d.id
            WHERE lt.id = ?
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Object[]{
                    rs.getInt("id"), rs.getString("patient_name"), rs.getString("doctor_name"),
                    rs.getString("test_name"), rs.getString("test_code"), rs.getString("sample_type"),
                    rs.getString("status"), rs.getString("result"), rs.getString("normal_range"),
                    rs.getString("technician_name")
                };
            }
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return null;
    }

    /** Find lab tests for a specific patient */
    public List<Object[]> findByPatientId(int patientId) {
        List<Object[]> list = new ArrayList<>();
        String sql = """
            SELECT lt.id, p.first_name || ' ' || p.last_name AS patient,
                   d.first_name || ' ' || d.last_name AS doctor,
                   lt.test_name, lt.test_code, lt.status, lt.created_at
            FROM lab_tests lt
            JOIN patients p ON lt.patient_id = p.id
            JOIN doctors d ON lt.doctor_id = d.id
            WHERE lt.patient_id = ?
            ORDER BY lt.created_at DESC
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("id"), rs.getString("patient"), rs.getString("doctor"),
                    rs.getString("test_name"), rs.getString("test_code"),
                    rs.getString("status"), rs.getTimestamp("created_at")
                });
            }
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return list;
    }

    /** Find lab tests for a specific doctor */
    public List<Object[]> findByDoctorId(int doctorId) {
        List<Object[]> list = new ArrayList<>();
        String sql = """
            SELECT lt.id, p.first_name || ' ' || p.last_name AS patient,
                   d.first_name || ' ' || d.last_name AS doctor,
                   lt.test_name, lt.test_code, lt.status, lt.created_at
            FROM lab_tests lt
            JOIN patients p ON lt.patient_id = p.id
            JOIN doctors d ON lt.doctor_id = d.id
            WHERE lt.doctor_id = ?
            ORDER BY lt.created_at DESC
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("id"), rs.getString("patient"), rs.getString("doctor"),
                    rs.getString("test_name"), rs.getString("test_code"),
                    rs.getString("status"), rs.getTimestamp("created_at")
                });
            }
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return list;
    }

    public boolean isOrderedByDoctor(int testId, int doctorId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM lab_tests WHERE id = ? AND doctor_id = ?")) {
            ps.setInt(1, testId); ps.setInt(2, doctorId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return false;
    }

    private void rollback(Connection conn) {
        if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
    }

    private void closeConnection(Connection conn) {
        if (conn != null) { try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {} }
    }
}
