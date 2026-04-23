package com.shushant.hospital_management.dao;

import com.shushant.hospital_management.db.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PharmacyDao {

    public int create(String name, String genericName, String manufacturer, String batchNumber,
                      Date expiryDate, int quantity, double unitPrice, int reorderLevel, String category) {
        String sql = """
            INSERT INTO medicines (name, generic_name, manufacturer, batch_number, expiry_date,
                quantity, unit_price, reorder_level, category) VALUES (?,?,?,?,?,?,?,?,?) RETURNING id
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name); ps.setString(2, genericName); ps.setString(3, manufacturer);
            ps.setString(4, batchNumber); ps.setDate(5, expiryDate); ps.setInt(6, quantity);
            ps.setDouble(7, unitPrice); ps.setInt(8, reorderLevel); ps.setString(9, category);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    public void update(int id, String name, String genericName, String manufacturer,
                       String batchNumber, Date expiryDate, int quantity, double unitPrice,
                       int reorderLevel, String category) {
        String sql = """
            UPDATE medicines SET name=?, generic_name=?, manufacturer=?, batch_number=?,
                expiry_date=?, quantity=?, unit_price=?, reorder_level=?, category=? WHERE id=?
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name); ps.setString(2, genericName); ps.setString(3, manufacturer);
            ps.setString(4, batchNumber); ps.setDate(5, expiryDate); ps.setInt(6, quantity);
            ps.setDouble(7, unitPrice); ps.setInt(8, reorderLevel); ps.setString(9, category);
            ps.setInt(10, id);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<Object[]> findAll() {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT id, name, generic_name, manufacturer, quantity, unit_price, expiry_date, reorder_level FROM medicines ORDER BY name";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("id"), rs.getString("name"), rs.getString("generic_name"),
                    rs.getString("manufacturer"), rs.getInt("quantity"), rs.getDouble("unit_price"),
                    rs.getDate("expiry_date"), rs.getInt("reorder_level")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Object[]> findLowStock() {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT id, name, quantity, reorder_level FROM medicines WHERE quantity <= reorder_level ORDER BY quantity";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("id"), rs.getString("name"), rs.getInt("quantity"), rs.getInt("reorder_level")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public List<Object[]> findExpiringSoon() {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT id, name, expiry_date, quantity FROM medicines WHERE expiry_date <= CURRENT_DATE + INTERVAL '30 days' AND expiry_date >= CURRENT_DATE ORDER BY expiry_date";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Object[]{
                    rs.getInt("id"), rs.getString("name"), rs.getDate("expiry_date"), rs.getInt("quantity")
                });
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    public void deductStock(int medicineId, int qty) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE medicines SET quantity = quantity - ? WHERE id = ? AND quantity >= ?")) {
            ps.setInt(1, qty); ps.setInt(2, medicineId); ps.setInt(3, qty);
            ps.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }
}
