package com.shushant.hospital_management.dao;

import com.shushant.hospital_management.db.DatabaseConnection;
import com.shushant.hospital_management.util.AuditLogger;
import com.shushant.hospital_management.util.ValidationUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AppointmentDao {

    // ── Valid state transitions ──────────────────────────────────────────────
    private static final Map<String, Set<String>> VALID_TRANSITIONS = Map.of(
        "BOOKED",       Set.of("CHECKED_IN", "CANCELLED", "NO_SHOW"),
        "CHECKED_IN",   Set.of("IN_PROGRESS", "CANCELLED", "NO_SHOW"),
        "IN_PROGRESS",  Set.of("COMPLETED", "CANCELLED"),
        "COMPLETED",    Set.of(),
        "CANCELLED",    Set.of(),
        "NO_SHOW",      Set.of()
    );

    /**
     * Transactional appointment creation — conflict check + token generation + insert
     * all within one transaction using row-level locking to prevent race conditions.
     */
    public int create(int patientId, int doctorId, Date appointmentDate, Time startTime,
                      Time endTime, String notes, boolean walkIn, int createdBy) {
        ValidationUtils.requirePositiveInt(patientId, "Patient ID");
        ValidationUtils.requirePositiveInt(doctorId, "Doctor ID");
        ValidationUtils.requireNotNull(appointmentDate, "Appointment Date");
        ValidationUtils.requireNotNull(startTime, "Start Time");
        ValidationUtils.requireNotNull(endTime, "End Time");

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Verify patient is active
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id FROM patients WHERE id = ? AND active = TRUE")) {
                ps.setInt(1, patientId);
                if (!ps.executeQuery().next()) {
                    throw new IllegalStateException("Patient ID " + patientId + " is not active or does not exist.");
                }
            }

            // 2. Verify doctor is active and acquire lock to serialize appointments
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id FROM doctors WHERE id = ? AND active = TRUE FOR UPDATE")) {
                ps.setInt(1, doctorId);
                if (!ps.executeQuery().next()) {
                    throw new IllegalStateException("Doctor ID " + doctorId + " is not active or does not exist.");
                }
            }

            // 3. Check for scheduling conflicts
            try (PreparedStatement ps = conn.prepareStatement("""
                    SELECT COUNT(*) FROM appointments
                    WHERE doctor_id = ? AND appointment_date = ? AND status IN ('BOOKED','CHECKED_IN','IN_PROGRESS')
                    AND start_time < ? AND end_time > ?
                """)) {
                ps.setInt(1, doctorId); ps.setDate(2, appointmentDate);
                ps.setTime(3, endTime); ps.setTime(4, startTime);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new IllegalStateException("Doctor has a conflicting appointment at this time.");
                }
            }

            // 4. Generate next token
            int token;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(MAX(token_number), 0) + 1 FROM appointments WHERE doctor_id = ? AND appointment_date = ?")) {
                ps.setInt(1, doctorId); ps.setDate(2, appointmentDate);
                ResultSet rs = ps.executeQuery();
                token = rs.next() ? rs.getInt(1) : 1;
            }

            // 5. Insert
            int id;
            try (PreparedStatement ps = conn.prepareStatement("""
                    INSERT INTO appointments (patient_id, doctor_id, appointment_date, start_time, end_time,
                        token_number, notes, walk_in, created_by) VALUES (?,?,?,?,?,?,?,?,?) RETURNING id
                """)) {
                ps.setInt(1, patientId); ps.setInt(2, doctorId); ps.setDate(3, appointmentDate);
                ps.setTime(4, startTime); ps.setTime(5, endTime); ps.setInt(6, token);
                ps.setString(7, notes); ps.setBoolean(8, walkIn); ps.setInt(9, createdBy);
                ResultSet rs = ps.executeQuery();
                id = rs.next() ? rs.getInt(1) : -1;
            }

            conn.commit();
            AuditLogger.log("CREATE", "appointments", id, "Booked for patient=" + patientId + " doctor=" + doctorId + " token=" + token);
            return id;

        } catch (SQLException e) {
            rollback(conn);
            throw new RuntimeException("Database error during appointment creation", e);
        } catch (IllegalStateException e) {
            rollback(conn);
            throw e;
        } finally {
            closeConnection(conn);
        }
    }

    /** State-machine validated status update. */
    public void updateStatus(int id, String newStatus, String cancelReason) {
        ValidationUtils.requireNonEmpty(newStatus, "Status");

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // Get current status with lock
            String currentStatus;
            try (PreparedStatement ps = conn.prepareStatement("SELECT status FROM appointments WHERE id = ? FOR UPDATE")) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) throw new IllegalStateException("Appointment ID " + id + " not found.");
                currentStatus = rs.getString("status");
            }

            // Validate transition
            Set<String> allowed = VALID_TRANSITIONS.get(currentStatus);
            if (allowed == null || !allowed.contains(newStatus)) {
                throw new IllegalStateException("Invalid status transition: " + currentStatus + " → " + newStatus);
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE appointments SET status=?, cancel_reason=? WHERE id=?")) {
                ps.setString(1, newStatus); ps.setString(2, cancelReason); ps.setInt(3, id);
                ps.executeUpdate();
            }

            conn.commit();
            AuditLogger.log("UPDATE_STATUS", "appointments", id, currentStatus + " → " + newStatus);

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

    public boolean hasConflict(int doctorId, Date date, Time startTime, Time endTime) {
        String sql = """
            SELECT COUNT(*) FROM appointments
            WHERE doctor_id = ? AND appointment_date = ? AND status IN ('BOOKED','CHECKED_IN','IN_PROGRESS')
            AND start_time < ? AND end_time > ?
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId); ps.setDate(2, date);
            ps.setTime(3, endTime); ps.setTime(4, startTime);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return false;
    }

    public List<Object[]> findAll() {
        List<Object[]> list = new ArrayList<>();
        String sql = """
            SELECT a.id, p.first_name || ' ' || p.last_name AS patient,
                   d.first_name || ' ' || d.last_name AS doctor,
                   a.appointment_date, a.start_time, a.status, a.token_number
            FROM appointments a
            JOIN patients p ON a.patient_id = p.id
            JOIN doctors d ON a.doctor_id = d.id
            ORDER BY a.appointment_date DESC, a.start_time DESC
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("id"), rs.getString("patient"), rs.getString("doctor"),
                    rs.getDate("appointment_date"), rs.getTime("start_time"),
                    rs.getString("status"), rs.getInt("token_number")
                });
            }
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return list;
    }

    public List<Object[]> findTodayByDoctor(int doctorId) {
        List<Object[]> list = new ArrayList<>();
        String sql = """
            SELECT a.id, a.token_number, p.first_name || ' ' || p.last_name AS patient,
                   a.start_time, a.status
            FROM appointments a JOIN patients p ON a.patient_id = p.id
            WHERE a.doctor_id = ? AND a.appointment_date = CURRENT_DATE
            ORDER BY a.token_number
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("id"), rs.getInt("token_number"), rs.getString("patient"),
                    rs.getTime("start_time"), rs.getString("status")
                });
            }
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return list;
    }

    public int getNextToken(int doctorId, Date date) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT COALESCE(MAX(token_number), 0) + 1 FROM appointments WHERE doctor_id = ? AND appointment_date = ?")) {
            ps.setInt(1, doctorId); ps.setDate(2, date);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return 1;
    }

    public int countToday() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM appointments WHERE appointment_date = CURRENT_DATE")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return 0;
    }

    /** Find all appointments for a specific patient */
    public List<Object[]> findByPatientId(int patientId) {
        List<Object[]> list = new ArrayList<>();
        String sql = """
            SELECT a.id, p.first_name || ' ' || p.last_name AS patient,
                   d.first_name || ' ' || d.last_name AS doctor,
                   a.appointment_date, a.start_time, a.status, a.token_number
            FROM appointments a
            JOIN patients p ON a.patient_id = p.id
            JOIN doctors d ON a.doctor_id = d.id
            WHERE a.patient_id = ?
            ORDER BY a.appointment_date DESC, a.start_time DESC
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("id"), rs.getString("patient"), rs.getString("doctor"),
                    rs.getDate("appointment_date"), rs.getTime("start_time"),
                    rs.getString("status"), rs.getInt("token_number")
                });
            }
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return list;
    }

    /** Find all appointments for a specific doctor */
    public List<Object[]> findByDoctorId(int doctorId) {
        List<Object[]> list = new ArrayList<>();
        String sql = """
            SELECT a.id, p.first_name || ' ' || p.last_name AS patient,
                   d.first_name || ' ' || d.last_name AS doctor,
                   a.appointment_date, a.start_time, a.status, a.token_number
            FROM appointments a
            JOIN patients p ON a.patient_id = p.id
            JOIN doctors d ON a.doctor_id = d.id
            WHERE a.doctor_id = ?
            ORDER BY a.appointment_date DESC, a.start_time DESC
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("id"), rs.getString("patient"), rs.getString("doctor"),
                    rs.getDate("appointment_date"), rs.getTime("start_time"),
                    rs.getString("status"), rs.getInt("token_number")
                });
            }
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return list;
    }

    /** Check if an appointment belongs to a specific patient */
    public boolean belongsToPatient(int appointmentId, int patientId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM appointments WHERE id = ? AND patient_id = ?")) {
            ps.setInt(1, appointmentId); ps.setInt(2, patientId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return false;
    }

    /** Check if an appointment belongs to a specific doctor */
    public boolean belongsToDoctor(int appointmentId, int doctorId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM appointments WHERE id = ? AND doctor_id = ?")) {
            ps.setInt(1, appointmentId); ps.setInt(2, doctorId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return false;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void rollback(Connection conn) {
        if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
    }

    private void closeConnection(Connection conn) {
        if (conn != null) { try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {} }
    }
}
