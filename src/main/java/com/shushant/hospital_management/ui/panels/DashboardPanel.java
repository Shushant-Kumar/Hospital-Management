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
        JPanel card = new JPanel(new BorderLayout(10, 8));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent.darker(), 1, true),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));
        card.setBackground(new Color(38, 42, 52));

        JLabel valLabel = new JLabel(value);
        valLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valLabel.setForeground(accent);

        JLabel nameLabel = new JLabel(label);
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        nameLabel.setForeground(Color.LIGHT_GRAY);

        card.add(valLabel, BorderLayout.CENTER);
        card.add(nameLabel, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createRefreshCard() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 70), 1, true),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)));
        card.setBackground(new Color(38, 42, 52));

        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        refreshBtn.setFocusPainted(false);
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
