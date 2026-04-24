package com.shushant.hospital_management.ui;

import com.shushant.hospital_management.dao.PatientDao;
import com.shushant.hospital_management.dao.UserDao;
import com.shushant.hospital_management.util.SessionManager;

import javax.swing.*;
import java.awt.*;
import java.sql.Date;

public class SignUpFrame extends JFrame {

    private final JTextField firstNameField = new JTextField(20);
    private final JTextField lastNameField = new JTextField(20);
    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JTextField emailField = new JTextField(20);
    private final JTextField phoneField = new JTextField(20);
    private final JTextField dobField = new JTextField(20);
    private final JComboBox<String> genderBox = new JComboBox<>(new String[]{"MALE", "FEMALE", "OTHER"});

    private final UserDao userDao = new UserDao();
    private final PatientDao patientDao = new PatientDao();

    public SignUpFrame() {
        setTitle("Hospital Management System — Patient Sign Up");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(500, 600);
        setLocationRelativeTo(null);
        setResizable(false);
        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        JLabel titleLabel = new JLabel("📝 Patient Registration", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(new Color(100, 180, 255));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        mainPanel.add(titleLabel, gbc);

        JLabel subtitleLabel = new JLabel("Create an account to access your dashboard", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(Color.GRAY);
        gbc.gridy = 1;
        mainPanel.add(subtitleLabel, gbc);

        gbc.gridy = 2;
        mainPanel.add(Box.createVerticalStrut(10), gbc);

        // Form Fields
        gbc.gridwidth = 1;
        int row = 3;

        addField(mainPanel, gbc, "First Name*:", firstNameField, row++);
        addField(mainPanel, gbc, "Last Name*:", lastNameField, row++);
        addField(mainPanel, gbc, "Username*:", usernameField, row++);
        addField(mainPanel, gbc, "Password*:", passwordField, row++);
        addField(mainPanel, gbc, "Email*:", emailField, row++);
        addField(mainPanel, gbc, "Phone*:", phoneField, row++);
        
        dobField.putClientProperty("JTextField.placeholderText", "YYYY-MM-DD");
        addField(mainPanel, gbc, "Date of Birth*:", dobField, row++);
        
        gbc.gridy = row++; gbc.gridx = 0;
        mainPanel.add(new JLabel("Gender*:"), gbc);
        gbc.gridx = 1;
        genderBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        mainPanel.add(genderBox, gbc);

        gbc.gridy = row++; gbc.gridx = 0; gbc.gridwidth = 2;
        mainPanel.add(Box.createVerticalStrut(15), gbc);

        // Sign Up button
        JButton signUpButton = new JButton("Register");
        signUpButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        signUpButton.setBackground(new Color(76, 175, 80));
        signUpButton.setForeground(Color.WHITE);
        signUpButton.setFocusPainted(false);
        signUpButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        gbc.gridy = row++; gbc.insets = new Insets(10, 6, 6, 6);
        mainPanel.add(signUpButton, gbc);

        // Back to Login
        JButton backButton = new JButton("Already have an account? Login");
        backButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        backButton.setForeground(new Color(100, 180, 255));
        backButton.setBackground(new Color(45, 48, 55));
        backButton.setFocusPainted(false);
        backButton.setBorderPainted(false);
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        gbc.gridy = row; gbc.insets = new Insets(0, 6, 6, 6);
        mainPanel.add(backButton, gbc);

        add(mainPanel);

        // Actions
        signUpButton.addActionListener(e -> doSignUp());
        backButton.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        });
    }

    private void addField(JPanel panel, GridBagConstraints gbc, String label, JTextField field, int row) {
        gbc.gridy = row; gbc.gridx = 0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        panel.add(field, gbc);
    }

    private void doSignUp() {
        String first = firstNameField.getText().trim();
        String last = lastNameField.getText().trim();
        String user = usernameField.getText().trim();
        String pass = new String(passwordField.getPassword());
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String dobStr = dobField.getText().trim();

        if (first.isEmpty() || last.isEmpty() || user.isEmpty() || pass.isEmpty() || 
            email.isEmpty() || phone.isEmpty() || dobStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all required fields.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Date dob;
        try {
            dob = Date.valueOf(dobStr);
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, "Invalid Date of Birth format. Use YYYY-MM-DD.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (userDao.exists(user, email)) {
            JOptionPane.showMessageDialog(this, "Username or email is already taken. Please choose another.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            // 1. Create User
            int userId = userDao.create(user, pass, first + " " + last, email, "PATIENT");
            if (userId == -1) {
                JOptionPane.showMessageDialog(this, "Failed to create user account.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 2. Create Patient Record
            int patientId = patientDao.create(
                patientDao.generatePatientUid(), first, last, email, phone, dob,
                (String) genderBox.getSelectedItem(), "", "", "OPD", "", "", "", "", "", userId
            );

            // 3. Link them
            patientDao.linkUser(patientId, userId);

            // 4. Auto-login
            if (SessionManager.login(user, pass)) {
                JOptionPane.showMessageDialog(this, "Registration Successful! Welcome to your dashboard.", "Success", JOptionPane.INFORMATION_MESSAGE);
                dispose();
                SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
            } else {
                // Fallback if auto-login fails for some reason
                dispose();
                SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "An error occurred during registration.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
