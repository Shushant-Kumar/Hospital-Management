package com.shushant.hospital_management.util;

import com.shushant.hospital_management.db.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AuditLogger {

    /**
     * Logs a sensitive action to the audit_logs table.
     *
     * @param action     The action performed (e.g., "DELETE", "UPDATE_STATUS", "RESET_PASSWORD")
     * @param entityName The name of the entity being modified (e.g., "patients", "appointments", "users")
     * @param entityId   The ID of the entity being modified
     * @param details    Optional description of the changes or context
     */
    public static void log(String action, String entityName, int entityId, String details) {
        String sql = "INSERT INTO audit_logs (action, entity_name, entity_id, user_id, details) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, action);
            ps.setString(2, entityName);
            ps.setInt(3, entityId);
            ps.setInt(4, SessionManager.getCurrentUserId());
            ps.setString(5, details);
            ps.executeUpdate();
        } catch (SQLException e) {
            // In a production app, we would log this to a file via SLF4J/Logback.
            System.err.println("[AuditLog Error] Failed to write audit log: " + e.getMessage());
        }
    }
}
