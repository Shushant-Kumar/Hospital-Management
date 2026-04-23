package com.shushant.hospital_management.dao;

import com.shushant.hospital_management.db.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BedDao {

    public int create(String wardName, String bedNumber, String roomType, int floor, double dailyRate) {
        String sql = "INSERT INTO beds (ward_name, bed_number, room_type, floor, daily_rate) VALUES (?,?,?,?,?) RETURNING id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, wardName); ps.setString(2, bedNumber); ps.setString(3, roomType);
            ps.setInt(4, floor); ps.setDouble(5, dailyRate);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public void assignPatient(int bedId, int patientId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE beds SET patient_id = ?, status = 'OCCUPIED' WHERE id = ?")) {
            ps.setInt(1, patientId); ps.setInt(2, bedId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void releaseBed(int bedId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE beds SET patient_id = NULL, status = 'AVAILABLE' WHERE id = ?")) {
            ps.setInt(1, bedId);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
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
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public int countAvailable() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM beds WHERE status = 'AVAILABLE'")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public int countOccupied() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM beds WHERE status = 'OCCUPIED'")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }
}
