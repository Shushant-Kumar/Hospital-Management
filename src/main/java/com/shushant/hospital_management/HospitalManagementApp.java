package com.shushant.hospital_management;

import com.formdev.flatlaf.FlatDarkLaf;
import com.shushant.hospital_management.db.DatabaseConnection;
import com.shushant.hospital_management.db.DatabaseInitializer;
import com.shushant.hospital_management.ui.LoginFrame;
import javax.swing.*;

public class HospitalManagementApp {

    public static void main(String[] args) {
        // Set modern FlatLaf dark theme
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 8);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
        } catch (Exception e) {
            System.err.println("Failed to set FlatLaf theme: " + e.getMessage());
        }

        // Database setup
        String url = envOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/hospital_management");
        String user = envOrDefault("DB_USERNAME", "postgres");
        String pass = envOrDefault("DB_PASSWORD", "Saraswati123");

        try {
            DatabaseConnection.init(url, user, pass);
            DatabaseInitializer.initialize();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Cannot connect to database.\n\n" + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        // Launch login screen
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(DatabaseConnection::shutdown));
    }

    private static String envOrDefault(String key, String defaultValue) {
        String val = System.getenv(key);
        if (val != null && !val.isBlank()) return val;
        // Also check system properties
        val = System.getProperty(key);
        return (val != null && !val.isBlank()) ? val : defaultValue;
    }
}
