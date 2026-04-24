package com.shushant.hospital_management.dao;

import com.shushant.hospital_management.db.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AppointmentDao {

    public int create(int patientId, int doctorId, Date appointmentDate, Time startTime,
                      Time endTime, int tokenNumber, String notes, boolean walkIn, int createdBy) {
        String sql = """
            INSERT INTO appointments (patient_id, doctor_id, appointment_date, start_time, end_time,
                token_number, notes, walk_in, created_by) VALUES (?,?,?,?,?,?,?,?,?) RETURNING id
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId); ps.setInt(2, doctorId); ps.setDate(3, appointmentDate);
            ps.setTime(4, startTime); ps.setTime(5, endTime); ps.setInt(6, tokenNumber);
            ps.setString(7, notes); ps.setBoolean(8, walkIn); ps.setInt(9, createdBy);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return -1;
    }

    public void updateStatus(int id, String status, String cancelReason) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE appointments SET status=?, cancel_reason=? WHERE id=?")) {
            ps.setString(1, status); ps.setString(2, cancelReason); ps.setInt(3, id);
            ps.executeUpdate();
            com.shushant.hospital_management.util.AuditLogger.log("UPDATE_STATUS", "appointments", id, "Status: " + status);
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
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
}
