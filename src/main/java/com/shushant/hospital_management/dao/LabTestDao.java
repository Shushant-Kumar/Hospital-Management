package com.shushant.hospital_management.dao;

import com.shushant.hospital_management.db.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LabTestDao {

    public int create(int patientId, int doctorId, String testName, String testCode, String sampleType) {
        String sql = """
            INSERT INTO lab_tests (patient_id, doctor_id, test_name, test_code, sample_type)
            VALUES (?,?,?,?,?) RETURNING id
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId); ps.setInt(2, doctorId); ps.setString(3, testName);
            ps.setString(4, testCode); ps.setString(5, sampleType);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public void updateStatus(int id, String status) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE lab_tests SET status = ? WHERE id = ?")) {
            ps.setString(1, status); ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void saveResult(int id, String result, String normalRange, String technicianName) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "UPDATE lab_tests SET result = ?, normal_range = ?, technician_name = ?, status = 'COMPLETED' WHERE id = ?")) {
            ps.setString(1, result); ps.setString(2, normalRange);
            ps.setString(3, technicianName); ps.setInt(4, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
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
        } catch (SQLException e) { e.printStackTrace(); }
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
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }
}
