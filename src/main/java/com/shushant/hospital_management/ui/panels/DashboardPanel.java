package com.shushant.hospital_management.ui.panels;

import com.shushant.hospital_management.dao.*;
import com.shushant.hospital_management.util.RBACManager;
import com.shushant.hospital_management.util.RBACManager.Module;
import com.shushant.hospital_management.util.SessionManager;
import javax.swing.*;
import java.awt.*;

public class DashboardPanel extends JPanel {

    public DashboardPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        buildUI();
    }

    private void buildUI() {
        // Title
        JLabel title = new JLabel("Dashboard Overview");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(new Color(100, 180, 255));
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

        JLabel roleLabel = new JLabel("Logged in as: " + SessionManager.getCurrentFullName()
                + " (" + SessionManager.getCurrentRole() + ")");
        roleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        roleLabel.setForeground(Color.LIGHT_GRAY);
        roleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.add(title);
        header.add(roleLabel);
        add(header, BorderLayout.NORTH);

        // Stats cards — filtered by role
        JPanel cardGrid = new JPanel(new GridLayout(0, 4, 18, 18));
        cardGrid.setOpaque(false);

        if (RBACManager.canView(Module.PATIENTS)) {
            cardGrid.add(createCard("Total Patients", String.valueOf(new PatientDao().count()), new Color(76, 175, 80)));
        }
        if (RBACManager.canView(Module.DOCTORS)) {
            cardGrid.add(createCard("Active Doctors", String.valueOf(new DoctorDao().count()), new Color(33, 150, 243)));
        }
        if (RBACManager.canView(Module.APPOINTMENTS)) {
            cardGrid.add(createCard("Today's Appointments", String.valueOf(new AppointmentDao().countToday()), new Color(255, 152, 0)));
        }
        if (RBACManager.canView(Module.BEDS)) {
            BedDao bedDao = new BedDao();
            cardGrid.add(createCard("Available Beds", String.valueOf(bedDao.countAvailable()), new Color(156, 39, 176)));
            cardGrid.add(createCard("Occupied Beds", String.valueOf(bedDao.countOccupied()), new Color(233, 30, 99)));
        }
        if (RBACManager.canView(Module.BILLING)) {
            BillingDao billingDao = new BillingDao();
            cardGrid.add(createCard("Total Revenue", "₹" + String.format("%,.2f", billingDao.getTotalRevenue()), new Color(0, 150, 136)));
            cardGrid.add(createCard("Pending Payments", "₹" + String.format("%,.2f", billingDao.getPendingPayments()), new Color(244, 67, 54)));
        }

        cardGrid.add(createRefreshCard());
        add(cardGrid, BorderLayout.CENTER);
    }

    private JPanel createCard(String label, String value, Color accent) {
        return new GradientCard(label, value, accent);
    }

    private static class GradientCard extends JPanel {
        private final Color accent;
        private final Color bgColor = new Color(42, 46, 56);
        
        public GradientCard(String label, String value, Color accent) {
            this.accent = accent;
            setOpaque(false);
            setLayout(new BorderLayout(10, 8));
            setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));

            JLabel valLabel = new JLabel(value);
            valLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
            valLabel.setForeground(Color.WHITE);

            JLabel nameLabel = new JLabel(label);
            nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            nameLabel.setForeground(new Color(200, 200, 210));

            add(valLabel, BorderLayout.CENTER);
            add(nameLabel, BorderLayout.SOUTH);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Background with subtle gradient
            GradientPaint gp = new GradientPaint(0, 0, bgColor, 0, getHeight(), bgColor.darker());
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            
            // Accent line at the top
            g2.setColor(accent);
            g2.fillRoundRect(0, 0, getWidth(), 6, 16, 16);
            
            // Clean up bottom corners of the accent line to make it a flat top border if needed, 
            // but a rounded top bar is fine.
            g2.fillRect(0, 3, getWidth(), 3);
            
            // Border
            g2.setColor(new Color(60, 65, 75));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            
            g2.dispose();
        }
    }

    private JPanel createRefreshCard() {
        JPanel card = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(42, 46, 56));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(new Color(60, 65, 75));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(22, 22, 22, 22));

        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        refreshBtn.setForeground(new Color(100, 180, 255));
        refreshBtn.setBackground(new Color(42, 46, 56));
        refreshBtn.setFocusPainted(false);
        refreshBtn.setBorderPainted(false);
        refreshBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshBtn.addActionListener(e -> {
            removeAll();
            buildUI();
            revalidate();
            repaint();
        });

        card.add(refreshBtn);
        return card;
    }
}
