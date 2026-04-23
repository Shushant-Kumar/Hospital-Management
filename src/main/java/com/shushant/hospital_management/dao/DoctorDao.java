package com.shushant.hospital_management.dao;

import com.shushant.hospital_management.db.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DoctorDao {

    public int create(String firstName, String lastName, String email, String phone,
                      String specialization, String licenseNumber, int departmentId,
                      double consultationFee, int consultationDurationMin) {
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
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public void update(int id, String firstName, String lastName, String email, String phone,
                       String specialization, int departmentId, double consultationFee,
                       int consultationDurationMin) {
        String sql = """
            UPDATE doctors SET first_name=?, last_name=?, email=?, phone=?, specialization=?,
                department_id=?, consultation_fee=?, consultation_duration_min=? WHERE id=?
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, firstName); ps.setString(2, lastName); ps.setString(3, email);
            ps.setString(4, phone); ps.setString(5, specialization);
            ps.setInt(6, departmentId); ps.setDouble(7, consultationFee);
            ps.setInt(8, consultationDurationMin); ps.setInt(9, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void delete(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE doctors SET active = FALSE WHERE id = ?")) {
            ps.setInt(1, id); ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
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
        } catch (SQLException e) { e.printStackTrace(); }
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
        } catch (SQLException e) { e.printStackTrace(); }
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
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public int count() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM doctors WHERE active = TRUE")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }
}
