package com.shushant.hospital_management.dao;

import com.shushant.hospital_management.db.DatabaseConnection;
import com.shushant.hospital_management.util.AuditLogger;
import com.shushant.hospital_management.util.ValidationUtils;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BedDao {

    public int create(String wardName, String bedNumber, String roomType, int floor, double dailyRate) {
        ValidationUtils.requireNonEmpty(wardName, "Ward Name");
        ValidationUtils.requireNonEmpty(bedNumber, "Bed Number");
        ValidationUtils.requireNonNegative(dailyRate, "Daily Rate");

        String sql = "INSERT INTO beds (ward_name, bed_number, room_type, floor, daily_rate) VALUES (?,?,?,?,?) RETURNING id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, wardName); ps.setString(2, bedNumber); ps.setString(3, roomType);
            ps.setInt(4, floor); ps.setDouble(5, dailyRate);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int id = rs.getInt(1);
                AuditLogger.log("CREATE", "beds", id, "Bed " + wardName + "/" + bedNumber);
                return id;
            }
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return -1;
    }

    /**
     * Transactional bed assignment — verifies bed is AVAILABLE and patient is not
     * already in another bed, auto-updates patient type to IPD.
     */
    public void assignPatient(int bedId, int patientId) {
        ValidationUtils.requirePositiveInt(bedId, "Bed ID");
        ValidationUtils.requirePositiveInt(patientId, "Patient ID");

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Verify bed is AVAILABLE (locked)
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT status FROM beds WHERE id = ? FOR UPDATE")) {
                ps.setInt(1, bedId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) throw new IllegalStateException("Bed ID " + bedId + " not found.");
                if (!"AVAILABLE".equals(rs.getString("status"))) {
                    throw new IllegalStateException("Bed is not available (current status: " + rs.getString("status") + ").");
                }
            }

            // 2. Verify patient not already in another bed
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT bed_number, ward_name FROM beds WHERE patient_id = ? AND status = 'OCCUPIED'")) {
                ps.setInt(1, patientId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    throw new IllegalStateException("Patient is already assigned to bed " +
                        rs.getString("ward_name") + "/" + rs.getString("bed_number") + ". Release first.");
                }
            }

            // 3. Assign
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE beds SET patient_id = ?, status = 'OCCUPIED' WHERE id = ?")) {
                ps.setInt(1, patientId); ps.setInt(2, bedId);
                ps.executeUpdate();
            }

            // 4. Update patient type to IPD
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE patients SET patient_type = 'IPD', updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                ps.setInt(1, patientId);
                ps.executeUpdate();
            }

            conn.commit();
            AuditLogger.log("ASSIGN_BED", "beds", bedId, "Assigned to patient " + patientId);

        } catch (SQLException e) {
            rollback(conn);
            throw new RuntimeException("Database error during bed assignment", e);
        } catch (IllegalStateException e) {
            rollback(conn);
            throw e;
        } finally {
            closeConnection(conn);
        }
    }

    /**
     * Transactional bed release — verifies bed is OCCUPIED, reverts patient type to OPD.
     */
    public void releaseBed(int bedId) {
        ValidationUtils.requirePositiveInt(bedId, "Bed ID");

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Get current patient from bed (locked)
            int patientId = 0;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT patient_id, status FROM beds WHERE id = ? FOR UPDATE")) {
                ps.setInt(1, bedId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) throw new IllegalStateException("Bed ID " + bedId + " not found.");
                if (!"OCCUPIED".equals(rs.getString("status"))) {
                    throw new IllegalStateException("Bed is not currently occupied.");
                }
                patientId = rs.getInt("patient_id");
            }

            // 2. Release bed
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE beds SET patient_id = NULL, status = 'AVAILABLE' WHERE id = ?")) {
                ps.setInt(1, bedId);
                ps.executeUpdate();
            }

            // 3. Revert patient type to OPD (only if they have no other occupied beds)
            if (patientId > 0) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT COUNT(*) FROM beds WHERE patient_id = ? AND status = 'OCCUPIED' AND id != ?")) {
                    ps.setInt(1, patientId); ps.setInt(2, bedId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next() && rs.getInt(1) == 0) {
                        try (PreparedStatement ps2 = conn.prepareStatement(
                                "UPDATE patients SET patient_type = 'OPD', updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                            ps2.setInt(1, patientId);
                            ps2.executeUpdate();
                        }
                    }
                }
            }

            conn.commit();
            AuditLogger.log("RELEASE_BED", "beds", bedId, "Released patient " + patientId);

        } catch (SQLException e) {
            rollback(conn);
            throw new RuntimeException("Database error during bed release", e);
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
            SELECT b.id, b.ward_name, b.bed_number, b.room_type, b.floor, b.status, b.daily_rate,
                   p.first_name || ' ' || p.last_name AS patient_name
            FROM beds b LEFT JOIN patients p ON b.patient_id = p.id
            ORDER BY b.ward_name, b.bed_number
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("id"), rs.getString("ward_name"), rs.getString("bed_number"),
                    rs.getString("room_type"), rs.getInt("floor"), rs.getString("status"),
                    rs.getDouble("daily_rate"), rs.getString("patient_name")
                });
            }
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return list;
    }

    public int countAvailable() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM beds WHERE status = 'AVAILABLE'")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return 0;
    }

    public int countOccupied() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM beds WHERE status = 'OCCUPIED'")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return 0;
    }

    private void rollback(Connection conn) {
        if (conn != null) { try { conn.rollback(); } catch (SQLException ignored) {} }
    }

    private void closeConnection(Connection conn) {
        if (conn != null) { try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {} }
    }
}
