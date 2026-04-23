package com.shushant.hospital_management.util;


import java.sql.Connection;
import com.shushant.hospital_management.db.DatabaseConnection;
import org.mindrot.jbcrypt.BCrypt;

public final class SessionManager {

    private static int currentUserId;
    private static String currentUsername;
    private static String currentFullName;
    private static String currentRole;

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
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void logout() {
        currentUserId = 0;
        currentUsername = null;
        currentFullName = null;
        currentRole = null;
    }

    public static int getCurrentUserId() { return currentUserId; }
    public static String getCurrentUsername() { return currentUsername; }
    public static String getCurrentFullName() { return currentFullName; }
    public static String getCurrentRole() { return currentRole; }

    public static boolean hasRole(String... roles) {
        if (currentRole == null) return false;
        for (String role : roles) {
            if (currentRole.equalsIgnoreCase(role)) return true;
        }
        return false;
    }

    public static boolean isAdmin() { return hasRole("ADMIN"); }
}
