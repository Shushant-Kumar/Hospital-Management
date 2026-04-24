package com.shushant.hospital_management.dao;

import com.shushant.hospital_management.db.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PatientDao {

    public int create(String patientUid, String firstName, String lastName, String email,
                      String phone, Date dob, String gender, String bloodGroup, String address,
                      String patientType, String allergies, String insuranceProvider,
                      String insurancePolicy, String emergencyContactName, String emergencyContactPhone,
                      int createdBy) {
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
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
        return -1;
    }

    public void update(int id, String firstName, String lastName, String email, String phone,
                       Date dob, String gender, String bloodGroup, String address,
                       String patientType, String allergies, String insuranceProvider,
                       String insurancePolicy, String emergencyContactName, String emergencyContactPhone) {
        String sql = """
            UPDATE patients SET first_name=?, last_name=?, email=?, phone=?, date_of_birth=?,
                gender=?, blood_group=?, address=?, patient_type=?, allergies=?,
                insurance_provider=?, insurance_policy=?, emergency_contact_name=?, emergency_contact_phone=?
            WHERE id=?
        """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, firstName); ps.setString(2, lastName); ps.setString(3, email);
            ps.setString(4, phone); ps.setDate(5, dob); ps.setString(6, gender);
            ps.setString(7, bloodGroup); ps.setString(8, address); ps.setString(9, patientType);
            ps.setString(10, allergies); ps.setString(11, insuranceProvider);
            ps.setString(12, insurancePolicy); ps.setString(13, emergencyContactName);
            ps.setString(14, emergencyContactPhone); ps.setInt(15, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
    }

    public void delete(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE patients SET active = FALSE WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException("Database error", e); }
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

    public String generatePatientUid() {
        return "PAT-" + (100000 + (int)(Math.random() * 900000));
    }

    public void linkUser(int patientId, int userId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE patients SET user_id = ? WHERE id = ?")) {
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
