package com.shushant.hospital_management.ui.panels;

import com.shushant.hospital_management.dao.*;
import com.shushant.hospital_management.util.RBACManager;
import com.shushant.hospital_management.util.RBACManager.Module;
import com.shushant.hospital_management.util.SessionManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class DashboardPanel extends JPanel {

    private JPanel cardGrid;

    public DashboardPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));
        buildUI();
    }

    private void buildUI() {
        // ── Header: Title + Role on left, Refresh button on right ──
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JPanel titleArea = new JPanel();
        titleArea.setLayout(new BoxLayout(titleArea, BoxLayout.Y_AXIS));
        titleArea.setOpaque(false);

        JLabel title = new JLabel("Dashboard Overview");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(new Color(100, 180, 255));
        title.setAlignmentX(LEFT_ALIGNMENT);

        JLabel roleLabel = new JLabel("Logged in as: " + SessionManager.getCurrentFullName()
                + " (" + SessionManager.getCurrentRole() + ")");
        roleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        roleLabel.setForeground(new Color(160, 165, 180));
        roleLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        roleLabel.setAlignmentX(LEFT_ALIGNMENT);

        titleArea.add(title);
        titleArea.add(roleLabel);
        header.add(titleArea, BorderLayout.WEST);

        // Refresh button — top right
        JButton refreshBtn = createRefreshButton();
        JPanel refreshWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 6));
        refreshWrapper.setOpaque(false);
        refreshWrapper.add(refreshBtn);
        header.add(refreshWrapper, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // ── Stats cards grid ──
        cardGrid = new JPanel(new GridLayout(0, 4, 16, 16));
        cardGrid.setOpaque(false);
        cardGrid.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        populateCards();

        // Wrap in a top-aligned container so cards don't stretch vertically
        JPanel cardWrapper = new JPanel(new BorderLayout());
        cardWrapper.setOpaque(false);
        cardWrapper.add(cardGrid, BorderLayout.NORTH);

        add(cardWrapper, BorderLayout.CENTER);
    }

    private void populateCards() {
        cardGrid.removeAll();
        int delay = 0;

        if (RBACManager.canView(Module.PATIENTS)) {
            cardGrid.add(createAnimatedCard("Total Patients", String.valueOf(new PatientDao().count()),
                    new Color(76, 175, 80), "👤", delay));
            delay += 80;
        }
        if (RBACManager.canView(Module.DOCTORS)) {
            cardGrid.add(createAnimatedCard("Active Doctors", String.valueOf(new DoctorDao().count()),
                    new Color(33, 150, 243), "⚕", delay));
            delay += 80;
        }
        if (RBACManager.canView(Module.APPOINTMENTS)) {
            cardGrid.add(createAnimatedCard("Today's Appts", String.valueOf(new AppointmentDao().countToday()),
                    new Color(255, 152, 0), "📅", delay));
            delay += 80;
        }
        if (RBACManager.canView(Module.BEDS)) {
            BedDao bedDao = new BedDao();
            cardGrid.add(createAnimatedCard("Available Beds", String.valueOf(bedDao.countAvailable()),
                    new Color(156, 39, 176), "🛏", delay));
            delay += 80;
            cardGrid.add(createAnimatedCard("Occupied Beds", String.valueOf(bedDao.countOccupied()),
                    new Color(233, 30, 99), "🏥", delay));
            delay += 80;
        }
        if (RBACManager.canView(Module.BILLING)) {
            BillingDao billingDao = new BillingDao();
            cardGrid.add(createAnimatedCard("Total Revenue",
                    "₹" + String.format("%,.2f", billingDao.getTotalRevenue()),
                    new Color(0, 150, 136), "💰", delay));
            delay += 80;
            cardGrid.add(createAnimatedCard("Pending", "₹" + String.format("%,.2f", billingDao.getPendingPayments()),
                    new Color(244, 67, 54), "⏳", delay));
        }

        cardGrid.revalidate();
        cardGrid.repaint();
    }

    // ── Animated card wrapper — fades in with delay ──
    private JPanel createAnimatedCard(String label, String value, Color accent, String icon, int delayMs) {
        GradientCard card = new GradientCard(label, value, accent, icon);
        card.setAlpha(0f);

        Timer fadeTimer = new Timer(16, null);
        fadeTimer.setInitialDelay(delayMs);
        fadeTimer.addActionListener(e -> {
            float a = card.getAlpha() + 0.08f;
            if (a >= 1f) {
                a = 1f;
                fadeTimer.stop();
            }
            card.setAlpha(a);
            card.repaint();
        });
        fadeTimer.start();

        return card;
    }

    // ── Gradient card with reduced height ──
    private static class GradientCard extends JPanel {
        private final Color accent;
        private final Color bgColor = new Color(42, 46, 56);
        private float alpha = 1f;
        private boolean hovered = false;

        public GradientCard(String label, String value, Color accent, String icon) {
            this.accent = accent;
            setOpaque(false);
            setLayout(new BorderLayout(8, 4));
            // Reduced padding = reduced card height
            setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
            setPreferredSize(new Dimension(0, 100));

            // Icon + value row
            JPanel topRow = new JPanel(new BorderLayout(8, 0));
            topRow.setOpaque(false);

            JLabel iconLabel = new JLabel(icon);
            iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
            iconLabel.setForeground(accent);
            topRow.add(iconLabel, BorderLayout.WEST);

            JLabel valLabel = new JLabel(value);
            valLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
            valLabel.setForeground(Color.WHITE);
            topRow.add(valLabel, BorderLayout.CENTER);

            JLabel nameLabel = new JLabel(label);
            nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            nameLabel.setForeground(new Color(180, 185, 200));

            add(topRow, BorderLayout.CENTER);
            add(nameLabel, BorderLayout.SOUTH);

            // Hover animation
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
        }

        public float getAlpha() { return alpha; }
        public void setAlpha(float alpha) { this.alpha = alpha; }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            int w = getWidth(), h = getHeight();

            // Background
            Color bg = hovered ? bgColor.brighter() : bgColor;
            GradientPaint gp = new GradientPaint(0, 0, bg, 0, h, bg.darker());
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, w, h, 14, 14);

            // Accent top bar
            g2.setColor(accent);
            g2.fillRoundRect(0, 0, w, 4, 14, 14);
            g2.fillRect(0, 2, w, 2);

            // Subtle glow on hover
            if (hovered) {
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 25));
                g2.fillRoundRect(0, 0, w, h, 14, 14);
            }

            // Border
            g2.setColor(hovered ? new Color(80, 85, 100) : new Color(55, 60, 70));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);

            g2.dispose();
        }
    }

    // ── Refresh Button ──
    private JButton createRefreshButton() {
        JButton btn = new JButton("⟳  Refresh") {
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
                Color bg = hovered ? new Color(50, 120, 220) : new Color(42, 100, 200);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(42, 100, 200));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(110, 34));
        btn.addActionListener(e -> {
            populateCards();
        });
        return btn;
    }
}
