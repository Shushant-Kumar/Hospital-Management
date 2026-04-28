package com.shushant.hospital_management.ui;

import com.shushant.hospital_management.ui.panels.*;
import com.shushant.hospital_management.util.RBACManager;
import com.shushant.hospital_management.util.RBACManager.Module;
import com.shushant.hospital_management.util.SessionManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainFrame extends JFrame {

    private final JPanel contentPanel = new JPanel(new CardLayout());
    private final JPanel sidebarPanel = new JPanel();
    private final Map<String, JToggleButton> navButtons = new HashMap<>();
    private String activeModule = null;
    private boolean sidebarCollapsed = false;
    private static final int SIDEBAR_EXPANDED = 220;
    private static final int SIDEBAR_COLLAPSED = 60;

    public MainFrame() {
        setTitle("Levaa — " + SessionManager.getCurrentFullName()
                + " [" + SessionManager.getCurrentRole() + "]");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(1000, 650));
        setLocationRelativeTo(null);
        initUI();
    }

    private void initUI() {
        // ── Global dark background for content area ──
        contentPanel.setBackground(new Color(35, 38, 47));

        // ── Sidebar ──
        sidebarPanel.setLayout(new BoxLayout(sidebarPanel, BoxLayout.Y_AXIS));
        sidebarPanel.setPreferredSize(new Dimension(SIDEBAR_EXPANDED, 0));
        sidebarPanel.setBackground(new Color(22, 25, 32));
        sidebarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(45, 48, 58)));

        // Logo area
        JLabel logo = new JLabel("🏥 Levaa", SwingConstants.CENTER);
        logo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        logo.setForeground(new Color(100, 180, 255));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        logo.setBorder(BorderFactory.createEmptyBorder(18, 0, 16, 0));
        sidebarPanel.add(logo);

        // Thin separator
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(180, 1));
        sep.setForeground(new Color(45, 50, 62));
        sep.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebarPanel.add(sep);
        sidebarPanel.add(Box.createVerticalStrut(12));

        // Navigation buttons
        List<Module> accessibleModules = RBACManager.getAccessibleModules();
        ButtonGroup bg = new ButtonGroup();

        for (Module mod : accessibleModules) {
            String displayName = mod.getDisplayName();
            JToggleButton btn = createSidebarButton(displayName);
            bg.add(btn);
            navButtons.put(displayName, btn);
            sidebarPanel.add(btn);
            sidebarPanel.add(Box.createVerticalStrut(2));

            contentPanel.add(createPanelForModule(mod), displayName);
        }

        sidebarPanel.add(Box.createVerticalGlue());

        // ── Collapse / Expand toggle button ──
        JButton collapseBtn = createCollapseButton();
        sidebarPanel.add(collapseBtn);
        sidebarPanel.add(Box.createVerticalStrut(8));

        // ── User profile section ──
        JPanel profileSection = createProfileSection();
        sidebarPanel.add(profileSection);
        sidebarPanel.add(Box.createVerticalStrut(8));

        // ── Logout button — red, rounded, with icon ──
        JButton logoutBtn = createLogoutButton();
        sidebarPanel.add(logoutBtn);
        sidebarPanel.add(Box.createVerticalStrut(14));

        // ── Layout ──
        setLayout(new BorderLayout());
        add(sidebarPanel, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        // Show first accessible module by default
        if (!accessibleModules.isEmpty()) {
            String first = accessibleModules.get(0).getDisplayName();
            showModule(first);
            JToggleButton firstBtn = navButtons.get(first);
            if (firstBtn != null) firstBtn.setSelected(true);
        }
    }

    // ── Panel factory ──
    private JPanel createPanelForModule(Module mod) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(new Color(35, 38, 47));
        JPanel inner = switch (mod) {
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
        wrapper.add(inner, BorderLayout.CENTER);
        return wrapper;
    }

    // ── Sidebar button with hover animation and active highlight ──
    private JToggleButton createSidebarButton(String name) {
        String icon = switch (name) {
            case "Dashboard"    -> "⬛";
            case "Patients"     -> "⬛";
            case "Doctors"      -> "⬛";
            case "Appointments" -> "⬛";
            case "Billing"      -> "⬛";
            case "Pharmacy"     -> "⬛";
            case "Lab Tests"    -> "⬛";
            case "Beds & Wards" -> "⬛";
            case "Users"        -> "⬛";
            case "My Dashboard" -> "⬛";
            default             -> "⬛";
        };

        // Unicode geometric icons for a cleaner look
        String iconChar = switch (name) {
            case "Dashboard"    -> "◉";
            case "Patients"     -> "♡";
            case "Doctors"      -> "✚";
            case "Appointments" -> "◈";
            case "Billing"      -> "◆";
            case "Pharmacy"     -> "⊕";
            case "Lab Tests"    -> "◎";
            case "Beds & Wards" -> "▣";
            case "Users"        -> "◫";
            case "My Dashboard" -> "⌂";
            default             -> "▪";
        };

        Color iconColor = switch (name) {
            case "Dashboard"    -> new Color(100, 180, 255);
            case "Patients"     -> new Color(76, 175, 80);
            case "Doctors"      -> new Color(33, 150, 243);
            case "Appointments" -> new Color(255, 183, 77);
            case "Billing"      -> new Color(0, 188, 212);
            case "Pharmacy"     -> new Color(171, 71, 188);
            case "Lab Tests"    -> new Color(255, 138, 101);
            case "Beds & Wards" -> new Color(121, 134, 203);
            case "Users"        -> new Color(77, 182, 172);
            case "My Dashboard" -> new Color(100, 180, 255);
            default             -> new Color(180, 180, 180);
        };

        JToggleButton btn = new JToggleButton() {
            private float hoverAnim = 0f;
            private Timer animTimer;
            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        if (animTimer != null) animTimer.stop();
                        animTimer = new Timer(16, ev -> {
                            hoverAnim = Math.min(1f, hoverAnim + 0.15f);
                            repaint();
                            if (hoverAnim >= 1f) ((Timer) ev.getSource()).stop();
                        });
                        animTimer.start();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        if (animTimer != null) animTimer.stop();
                        animTimer = new Timer(16, ev -> {
                            hoverAnim = Math.max(0f, hoverAnim - 0.1f);
                            repaint();
                            if (hoverAnim <= 0f) ((Timer) ev.getSource()).stop();
                        });
                        animTimer.start();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth(), h = getHeight();

                if (isSelected()) {
                    // Active state — accent bar + tinted background
                    g2.setColor(new Color(iconColor.getRed(), iconColor.getGreen(), iconColor.getBlue(), 30));
                    g2.fillRoundRect(4, 1, w - 8, h - 2, 10, 10);

                    // Left accent bar
                    g2.setColor(iconColor);
                    g2.fillRoundRect(0, 4, 3, h - 8, 3, 3);
                } else if (hoverAnim > 0) {
                    // Hover animation
                    int alpha = (int) (20 * hoverAnim);
                    g2.setColor(new Color(255, 255, 255, alpha));
                    g2.fillRoundRect(4, 1, w - 8, h - 2, 10, 10);
                }

                // Icon
                g2.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 16));
                g2.setColor(isSelected() ? iconColor : new Color(140, 145, 160));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(iconChar, 18, (h + fm.getAscent() - fm.getDescent()) / 2);

                // Text
                g2.setFont(new Font("Segoe UI", isSelected() ? Font.BOLD : Font.PLAIN, 13));
                g2.setColor(isSelected() ? Color.WHITE : new Color(180, 185, 200));
                g2.drawString(name, 42, (h + fm.getAscent() - fm.getDescent()) / 2);

                g2.dispose();
            }
        };

        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(210, 38));
        btn.setPreferredSize(new Dimension(210, 38));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> showModule(name));
        return btn;
    }

    // ── Collapse / Expand button ──
    private JButton createCollapseButton() {
        JButton btn = new JButton("« Collapse") {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override
                    public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (hovered) {
                    g2.setColor(new Color(40, 44, 55));
                    g2.fillRoundRect(4, 0, getWidth() - 8, getHeight(), 8, 8);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setForeground(new Color(120, 125, 140));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(200, 30));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> toggleSidebar(btn));
        return btn;
    }

    private void toggleSidebar(JButton collapseBtn) {
        sidebarCollapsed = !sidebarCollapsed;
        int targetWidth = sidebarCollapsed ? SIDEBAR_COLLAPSED : SIDEBAR_EXPANDED;
        collapseBtn.setText(sidebarCollapsed ? "»" : "« Collapse");

        // Animate sidebar width
        Timer timer = new Timer(12, null);
        timer.addActionListener(e -> {
            Dimension current = sidebarPanel.getPreferredSize();
            int w = current.width;
            int step = (targetWidth - w) / 4;
            if (step == 0) step = targetWidth > w ? 1 : -1;
            int newW = w + step;

            if ((step > 0 && newW >= targetWidth) || (step < 0 && newW <= targetWidth)) {
                newW = targetWidth;
                ((Timer) e.getSource()).stop();
            }
            sidebarPanel.setPreferredSize(new Dimension(newW, 0));
            sidebarPanel.revalidate();
            revalidate();
        });
        timer.start();

        // Toggle visibility of button text / profiles
        for (Component comp : sidebarPanel.getComponents()) {
            if (comp instanceof JToggleButton) {
                comp.setVisible(true); // always show, just narrower
            }
        }
    }

    // ── User profile section at bottom ──
    private JPanel createProfileSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.setMaximumSize(new Dimension(200, 44));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));

        // Avatar circle
        JLabel avatar = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Circle background
                g2.setColor(new Color(60, 130, 220));
                g2.fillOval(0, 0, 32, 32);

                // Initials
                String fullName = SessionManager.getCurrentFullName();
                String initials = "";
                if (fullName != null && !fullName.isEmpty()) {
                    String[] parts = fullName.split("\\s+");
                    initials = String.valueOf(parts[0].charAt(0)).toUpperCase();
                    if (parts.length > 1) initials += String.valueOf(parts[parts.length - 1].charAt(0)).toUpperCase();
                }
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (32 - fm.stringWidth(initials)) / 2;
                int ty = (32 + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(initials, tx, ty);
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(32, 32));
        avatar.setMinimumSize(new Dimension(32, 32));
        avatar.setMaximumSize(new Dimension(32, 32));

        panel.add(avatar);
        panel.add(Box.createHorizontalStrut(10));

        // Name & role
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        String fullName = SessionManager.getCurrentFullName();
        if (fullName != null && fullName.length() > 14) fullName = fullName.substring(0, 12) + "..";

        JLabel nameLabel = new JLabel(fullName != null ? fullName : "User");
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        nameLabel.setForeground(Color.WHITE);
        nameLabel.setAlignmentX(LEFT_ALIGNMENT);

        JLabel roleLabel = new JLabel(SessionManager.getCurrentRole());
        roleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        roleLabel.setForeground(new Color(120, 130, 150));
        roleLabel.setAlignmentX(LEFT_ALIGNMENT);

        textPanel.add(nameLabel);
        textPanel.add(roleLabel);
        panel.add(textPanel);

        return panel;
    }

    // ── Red rounded Logout button with icon ──
    private JButton createLogoutButton() {
        JButton logoutBtn = new JButton("⏻  Logout") {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                    @Override
                    public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color bg = hovered ? new Color(160, 30, 30) : new Color(200, 50, 50);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setOpaque(false);
        logoutBtn.setContentAreaFilled(false);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setBorderPainted(false);
        logoutBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoutBtn.setMaximumSize(new Dimension(190, 36));
        logoutBtn.setPreferredSize(new Dimension(190, 36));
        logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> logout());
        return logoutBtn;
    }

    private void showModule(String name) {
        activeModule = name;
        CardLayout cl = (CardLayout) contentPanel.getLayout();
        cl.show(contentPanel, name);

        // Update active highlight on buttons
        for (Map.Entry<String, JToggleButton> entry : navButtons.entrySet()) {
            entry.getValue().setSelected(entry.getKey().equals(name));
            entry.getValue().repaint();
        }
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
