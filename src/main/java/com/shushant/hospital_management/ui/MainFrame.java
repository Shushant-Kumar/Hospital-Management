package com.shushant.hospital_management.ui;

import com.shushant.hospital_management.ui.panels.*;
import com.shushant.hospital_management.util.SessionManager;
import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {

    private final JPanel contentPanel = new JPanel(new CardLayout());
    private final JPanel sidebarPanel = new JPanel();

    private static final String[] MODULES = {
        "Dashboard", "Patients", "Doctors", "Appointments",
        "Billing", "Pharmacy", "Lab Tests", "Beds & Wards", "Users"
    };

    public MainFrame() {
        setTitle("Hospital Management System — " + SessionManager.getCurrentFullName()
                + " [" + SessionManager.getCurrentRole() + "]");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(1000, 650));
        setLocationRelativeTo(null);
        initUI();
    }

    private void initUI() {
        // Sidebar
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setPreferredSize(new Dimension(200, 0));
        sidebarPanel.setBackground(new Color(30, 33, 40));
        sidebarPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));

        // Logo area
        JLabel logo = new JLabel("🏥 HMS", SwingConstants.CENTER);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        logo.setForeground(new Color(100, 180, 255));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        logo.setBorder(BorderFactory.createEmptyBorder(5, 0, 20, 0));
        sidebarPanel.add(logo);
        sidebarPanel.add(new JSeparator());
        sidebarPanel.add(Box.createVerticalStrut(10));

        // Module buttons
        ButtonGroup bg = new ButtonGroup();
        for (String mod : MODULES) {
            if (shouldShowModule(mod)) {
                JToggleButton btn = createSidebarButton(mod);
                bg.add(btn);
                sidebarPanel.add(btn);
                sidebarPanel.add(Box.createVerticalStrut(3));
            }
        }

        sidebarPanel.add(Box.createVerticalGlue());

        // Logout button at bottom
        JButton logoutBtn = new JButton("⏻ Logout");
        logoutBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        logoutBtn.setForeground(new Color(255, 100, 100));
        logoutBtn.setBackground(new Color(45, 48, 55));
        logoutBtn.setFocusPainted(false);
        logoutBtn.setBorderPainted(false);
        logoutBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoutBtn.setMaximumSize(new Dimension(180, 36));
        logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> logout());
        sidebarPanel.add(logoutBtn);

        // Content panels
        contentPanel.add(new DashboardPanel(), "Dashboard");
        contentPanel.add(new PatientPanel(), "Patients");
        contentPanel.add(new DoctorPanel(), "Doctors");
        contentPanel.add(new AppointmentPanel(), "Appointments");
        contentPanel.add(new BillingPanel(), "Billing");
        contentPanel.add(new PharmacyPanel(), "Pharmacy");
        contentPanel.add(new LabTestPanel(), "Lab Tests");
        contentPanel.add(new BedPanel(), "Beds & Wards");
        contentPanel.add(new UserPanel(), "Users");

        // Layout
        setLayout(new BorderLayout());
        add(sidebarPanel, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        // Show Dashboard by default
        showModule("Dashboard");
    }

    private JToggleButton createSidebarButton(String name) {
        String icon = switch (name) {
            case "Dashboard"    -> "📊";
            case "Patients"     -> "🧑‍🦽";
            case "Doctors"      -> "👨‍⚕️";
            case "Appointments" -> "📅";
            case "Billing"      -> "💳";
            case "Pharmacy"     -> "💊";
            case "Lab Tests"    -> "🔬";
            case "Beds & Wards" -> "🛏️";
            case "Users"        -> "👥";
            default             -> "📋";
        };

        JToggleButton btn = new JToggleButton(icon + "  " + name);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(30, 33, 40));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(190, 38));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> showModule(name));
        return btn;
    }

    private void showModule(String name) {
        CardLayout cl = (CardLayout) contentPanel.getLayout();
        cl.show(contentPanel, name);
    }

    private boolean shouldShowModule(String mod) {
        String role = SessionManager.getCurrentRole();
        if ("ADMIN".equals(role)) return true;
        return switch (mod) {
            case "Users" -> false;
            case "Pharmacy" -> SessionManager.hasRole("PHARMACIST", "ADMIN");
            case "Lab Tests" -> SessionManager.hasRole("LAB_TECHNICIAN", "DOCTOR", "ADMIN");
            case "Beds & Wards" -> SessionManager.hasRole("NURSE", "ADMIN", "RECEPTIONIST");
            case "Billing" -> SessionManager.hasRole("ACCOUNTANT", "ADMIN", "RECEPTIONIST");
            default -> true;
        };
    }

    private void logout() {
        int opt = JOptionPane.showConfirmDialog(this, "Are you sure you want to logout?",
                "Confirm Logout", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            SessionManager.logout();
            dispose();
            SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
        }
    }
}
