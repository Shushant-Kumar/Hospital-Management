package com.shushant.hospital_management.ui.panels;

import com.shushant.hospital_management.dao.UserDao;
import com.shushant.hospital_management.util.RBACManager;
import com.shushant.hospital_management.util.RBACManager.Module;
import com.shushant.hospital_management.util.RBACManager.Permission;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class UserPanel extends JPanel {

    private final UserDao dao = new UserDao();
    private final DefaultTableModel tableModel;
    private final JTable table;

    private static final String[] COLUMNS = {"ID", "Username", "Full Name", "Email", "Role", "Active", "Created"};

    public UserPanel() {
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Defense-in-depth: block non-admin users
        if (!RBACManager.hasPermission(Module.USERS, Permission.VIEW)) {
            add(new JLabel("Access Denied — Admin only.", SwingConstants.CENTER), BorderLayout.CENTER);
            tableModel = new DefaultTableModel(COLUMNS, 0);
            table = new JTable(tableModel);
            return;
        }

        JPanel topBar = new JPanel(new BorderLayout());
        JLabel title = new JLabel("User Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(100, 180, 255));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton addBtn = btn("➕ Add User", new Color(76, 175, 80));
        addBtn.addActionListener(e -> showAddDialog());
        JButton refreshBtn = btn("🔄", null);
        refreshBtn.addActionListener(e -> loadData());
        actions.add(addBtn); actions.add(refreshBtn);
        topBar.add(title, BorderLayout.WEST);
        topBar.add(actions, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(28); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        JButton toggleBtn = btn("🔒 Toggle Active", new Color(255, 152, 0));
        toggleBtn.addActionListener(e -> toggleActive());
        JButton resetBtn = btn("🔑 Reset Password", new Color(33, 150, 243));
        resetBtn.addActionListener(e -> resetPassword());
        bottomBar.add(toggleBtn); bottomBar.add(resetBtn);
        add(bottomBar, BorderLayout.SOUTH);

        loadData();
    }

    private void loadData() {
        tableModel.setRowCount(0);
        for (Object[] row : dao.findAll()) tableModel.addRow(row);
    }

    private void showAddDialog() {
        if (!RBACManager.requirePermission(Module.USERS, Permission.CREATE, this)) return;

        JTextField fUsername = new JTextField(), fPassword = new JTextField(),
                fFullName = new JTextField(), fEmail = new JTextField();
        JComboBox<String> fRole = new JComboBox<>(new String[]{
            "ADMIN", "DOCTOR", "NURSE", "RECEPTIONIST", "PHARMACIST", "LAB_TECHNICIAN", "ACCOUNTANT", "PATIENT"
        });

        Object[] fields = { "Username*:", fUsername, "Password*:", fPassword,
            "Full Name*:", fFullName, "Email:", fEmail, "Role:", fRole };

        if (JOptionPane.showConfirmDialog(this, fields, "Add User", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            if (fUsername.getText().trim().isEmpty() || fPassword.getText().trim().isEmpty() || fFullName.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username, password, and full name are required."); return;
            }
            dao.create(fUsername.getText().trim(), fPassword.getText().trim(), fFullName.getText().trim(),
                    fEmail.getText().trim(), (String) fRole.getSelectedItem());
            loadData();
        }
    }

    private void toggleActive() {
        if (!RBACManager.requirePermission(Module.USERS, Permission.EDIT, this)) return;

        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a user first."); return; }
        int id = (int) tableModel.getValueAt(row, 0);
        boolean current = (boolean) tableModel.getValueAt(row, 5);
        dao.toggleActive(id, !current);
        loadData();
    }

    private void resetPassword() {
        if (!RBACManager.requirePermission(Module.USERS, Permission.EDIT, this)) return;

        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a user first."); return; }
        int id = (int) tableModel.getValueAt(row, 0);
        String newPass = JOptionPane.showInputDialog(this, "Enter new password:");
        if (newPass != null && !newPass.trim().isEmpty()) {
            dao.resetPassword(id, newPass.trim());
            JOptionPane.showMessageDialog(this, "Password reset successfully.");
        }
    }

    private JButton btn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (bg != null) { b.setBackground(bg); b.setForeground(Color.WHITE); }
        return b;
    }
}
