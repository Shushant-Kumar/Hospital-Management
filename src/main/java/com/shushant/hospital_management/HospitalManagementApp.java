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
