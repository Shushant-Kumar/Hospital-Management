package com.shushant.hospital_management.dao;

import com.shushant.hospital_management.db.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DepartmentDao {

    public List<Object[]> findAll() {
        List<Object[]> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name, code, description FROM departments WHERE active = TRUE ORDER BY name")) {
            while (rs.next()) {
                list.add(new Object[]{ rs.getInt("id"), rs.getString("name"), rs.getString("code"), rs.getString("description") });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<String[]> findAllForCombo() {
        List<String[]> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name FROM departments WHERE active = TRUE ORDER BY name")) {
            while (rs.next()) {
                list.add(new String[]{ String.valueOf(rs.getInt("id")), rs.getString("name") });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public int create(String name, String code, String description) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO departments (name, code, description) VALUES (?,?,?) RETURNING id")) {
            ps.setString(1, name); ps.setString(2, code); ps.setString(3, description);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }
}
