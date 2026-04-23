package com.shushant.hospital_management.util;


import java.sql.Connection;
import com.shushant.hospital_management.db.DatabaseConnection;
import org.mindrot.jbcrypt.BCrypt;

public final class SessionManager {

    private static int currentUserId;
    private static String currentUsername;
    private static String currentFullName;
    private static String currentRole;
    private static int currentDoctorId;   // Set if user is linked to a doctor record
    private static int currentPatientId;  // Set if user is linked to a patient record

    private SessionManager() {}

    public static boolean login(String username, String password) {
        try (Connection conn = DatabaseConnection.getConnection();
             var ps = conn.prepareStatement(
                     "SELECT id, password_hash, full_name, role FROM users WHERE username = ? AND active = TRUE")) {
            ps.setString(1, username);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    String hash = rs.getString("password_hash");
                    if (BCrypt.checkpw(password, hash)) {
                        currentUserId = rs.getInt("id");
                        currentUsername = username;
                        currentFullName = rs.getString("full_name");
                        currentRole = rs.getString("role");
                        // Resolve linked doctor/patient IDs
                        currentDoctorId = resolveDoctorId(currentUserId);
                        currentPatientId = resolvePatientId(currentUserId);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Database error during login", e);
        }
        return false;
    }

    public static void logout() {
        currentUserId = 0;
        currentUsername = null;
        currentFullName = null;
        currentRole = null;
        currentDoctorId = 0;
        currentPatientId = 0;
    }

    public static int getCurrentUserId() { return currentUserId; }
    public static String getCurrentUsername() { return currentUsername; }
    public static String getCurrentFullName() { return currentFullName; }
    public static String getCurrentRole() { return currentRole; }
    public static int getCurrentDoctorId() { return currentDoctorId; }
    public static int getCurrentPatientId() { return currentPatientId; }

    public static boolean hasRole(String... roles) {
        if (currentRole == null) return false;
        for (String role : roles) {
            if (currentRole.equalsIgnoreCase(role)) return true;
        }
        return false;
    }

    public static boolean isAdmin() { return hasRole("ADMIN"); }

    /**
     * Look up the doctor record linked to this user via doctors.user_id.
     * Returns 0 if no linked doctor found.
     */
    private static int resolveDoctorId(int userId) {
        try (Connection conn = DatabaseConnection.getConnection();
             var ps = conn.prepareStatement("SELECT id FROM doctors WHERE user_id = ? AND active = TRUE")) {
            ps.setInt(1, userId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (Exception e) {
            // Column may not exist yet on first run — ignore gracefully
        }
        return 0;
    }

    /**
     * Look up the patient record linked to this user via patients.user_id.
     * Returns 0 if no linked patient found.
     */
    private static int resolvePatientId(int userId) {
        try (Connection conn = DatabaseConnection.getConnection();
             var ps = conn.prepareStatement("SELECT id FROM patients WHERE user_id = ? AND active = TRUE")) {
            ps.setInt(1, userId);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("id");
            }
        } catch (Exception e) {
            // Column may not exist yet on first run — ignore gracefully
        }
        return 0;
    }
}
