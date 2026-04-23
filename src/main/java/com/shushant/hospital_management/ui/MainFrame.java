package com.shushant.hospital_management.ui;

import com.shushant.hospital_management.ui.panels.*;
import com.shushant.hospital_management.util.RBACManager;
import com.shushant.hospital_management.util.RBACManager.Module;
import com.shushant.hospital_management.util.SessionManager;
import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MainFrame extends JFrame {

    private final JPanel contentPanel = new JPanel(new CardLayout());
    private final JPanel sidebarPanel = new JPanel();

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

        // Build sidebar and content panels based on RBAC
        List<Module> accessibleModules = RBACManager.getAccessibleModules();
        ButtonGroup bg = new ButtonGroup();

        for (Module mod : accessibleModules) {
            String displayName = mod.getDisplayName();
            JToggleButton btn = createSidebarButton(displayName);
            bg.add(btn);
            sidebarPanel.add(btn);
            sidebarPanel.add(Box.createVerticalStrut(3));

            // Lazy-create panel for each accessible module
            contentPanel.add(createPanelForModule(mod), displayName);
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

        // Layout
        setLayout(new BorderLayout());
        add(sidebarPanel, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        // Show first accessible module by default
        if (!accessibleModules.isEmpty()) {
            showModule(accessibleModules.get(0).getDisplayName());
        }
    }

    private JPanel createPanelForModule(Module mod) {
        return switch (mod) {
            case DASHBOARD      -> new DashboardPanel();
            case PATIENTS       -> new PatientPanel();
            case DOCTORS        -> new DoctorPanel();
            case APPOINTMENTS   -> new AppointmentPanel();
            case BILLING        -> new BillingPanel();
            case PHARMACY       -> new PharmacyPanel();
            case LAB_TESTS      -> new LabTestPanel();
            case BEDS           -> new BedPanel();
            case USERS          -> new UserPanel();
            case PATIENT_PORTAL -> new PatientDashboardPanel();
        };
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
            case "My Dashboard" -> "🏠";
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
