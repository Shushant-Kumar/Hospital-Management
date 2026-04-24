package com.shushant.hospital_management;

import com.formdev.flatlaf.FlatDarkLaf;
import com.shushant.hospital_management.db.DatabaseConnection;
import com.shushant.hospital_management.db.DatabaseInitializer;
import com.shushant.hospital_management.ui.LoginFrame;
import io.github.cdimascio.dotenv.Dotenv;
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

            // Premium Table Aesthetics
            UIManager.put("Table.alternateRowColor", new java.awt.Color(45, 45, 50));
            UIManager.put("Table.selectionBackground", new java.awt.Color(50, 100, 180));
            UIManager.put("Table.selectionForeground", java.awt.Color.WHITE);
            UIManager.put("TableHeader.background", new java.awt.Color(35, 35, 40));
            UIManager.put("TableHeader.font", new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
            UIManager.put("Table.rowHeight", 32);
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.showVerticalLines", false);
        } catch (Exception e) {
            System.err.println("Failed to set FlatLaf theme: " + e.getMessage());
        }

        // Load .env file
        Dotenv dotenv = null;
        try {
            dotenv = Dotenv.configure().ignoreIfMissing().load();
        } catch (Exception e) {
            System.err.println("Could not load .env file: " + e.getMessage());
        }

        // Database setup
        String url = envOrDefault(dotenv, "DB_URL", "jdbc:postgresql://localhost:5432/hospital_management");
        String user = envOrDefault(dotenv, "DB_USERNAME", "postgres");
        String pass = envOrDefault(dotenv, "DB_PASSWORD", ""); // Do not hardcode passwords

        if (pass.isEmpty()) {
            System.err.println("WARNING: DB_PASSWORD is not set. Ensure it is provided via environment variables for production.");
        }

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

    private static String envOrDefault(Dotenv dotenv, String key, String defaultValue) {
        // Try Dotenv first
        if (dotenv != null) {
            String val = dotenv.get(key);
            if (val != null && !val.isBlank()) return val;
        }
        // Fallback to OS environment variables
        String val = System.getenv(key);
        if (val != null && !val.isBlank()) return val;
        // Fallback to system properties
        val = System.getProperty(key);
        return (val != null && !val.isBlank()) ? val : defaultValue;
    }
}
