package com.shushant.hospital_management.ui;

import com.shushant.hospital_management.util.SessionManager;
import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {

    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);

    public LoginFrame() {
        setTitle("Hospital Management System — Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(480, 380);
        setLocationRelativeTo(null);
        setResizable(false);
        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        JLabel titleLabel = new JLabel("🏥 Hospital Management System", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(new Color(100, 180, 255));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        mainPanel.add(titleLabel, gbc);

        JLabel subtitleLabel = new JLabel("Sign in to continue", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(Color.GRAY);
        gbc.gridy = 1;
        mainPanel.add(subtitleLabel, gbc);

        gbc.gridy = 2;
        mainPanel.add(Box.createVerticalStrut(15), gbc);

        // Username
        gbc.gridwidth = 1; gbc.gridy = 3; gbc.gridx = 0;
        mainPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        mainPanel.add(usernameField, gbc);

        // Password
        gbc.gridy = 4; gbc.gridx = 0;
        mainPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        mainPanel.add(passwordField, gbc);

        // Login button
        JButton loginButton = new JButton("Login");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.setBackground(new Color(60, 130, 230));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        gbc.gridy = 5; gbc.gridx = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 6, 6, 6);
        mainPanel.add(loginButton, gbc);

        // Default credentials hint
        JLabel hintLabel = new JLabel("Default: admin / admin123", SwingConstants.CENTER);
        hintLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hintLabel.setForeground(new Color(120, 120, 120));
        gbc.gridy = 6; gbc.insets = new Insets(6, 6, 6, 6);
        mainPanel.add(hintLabel, gbc);

        // Sign Up Button
        JButton signUpButton = new JButton("New Patient? Sign Up");
        signUpButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        signUpButton.setForeground(new Color(100, 180, 255));
        signUpButton.setBackground(new Color(45, 48, 55));
        signUpButton.setFocusPainted(false);
        signUpButton.setBorderPainted(false);
        signUpButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        gbc.gridy = 7;
        mainPanel.add(signUpButton, gbc);

        // Sign Up action
        signUpButton.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(() -> new SignUpFrame().setVisible(true));
        });

        add(mainPanel);

        // Actions
        loginButton.addActionListener(e -> doLogin());
        passwordField.addActionListener(e -> doLogin());
        usernameField.addActionListener(e -> passwordField.requestFocus());

        usernameField.setText("admin");
    }

    private void doLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both username and password.",
                    "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (SessionManager.login(username, password)) {
            dispose();
            SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
        } else {
            JOptionPane.showMessageDialog(this, "Invalid credentials. Please try again.",
                    "Login Failed", JOptionPane.ERROR_MESSAGE);
            passwordField.setText("");
            passwordField.requestFocus();
        }
    }
}
