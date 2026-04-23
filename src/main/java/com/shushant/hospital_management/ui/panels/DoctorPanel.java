package com.shushant.hospital_management.ui.panels;

import com.shushant.hospital_management.dao.DepartmentDao;
import com.shushant.hospital_management.dao.DoctorDao;
import com.shushant.hospital_management.util.RBACManager;
import com.shushant.hospital_management.util.RBACManager.Module;
import com.shushant.hospital_management.util.RBACManager.Permission;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class DoctorPanel extends JPanel {

    private final DoctorDao dao = new DoctorDao();
    private final DepartmentDao deptDao = new DepartmentDao();
    private final DefaultTableModel tableModel;
    private final JTable table;

    private static final String[] COLUMNS = {"ID", "First Name", "Last Name", "Specialization", "Phone", "Department", "Fee"};

    public DoctorPanel() {
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel topBar = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Doctor Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(100, 180, 255));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        if (RBACManager.hasPermission(Module.DOCTORS, Permission.CREATE)) {
            JButton addBtn = btn("➕ Add Doctor", new Color(76, 175, 80));
            addBtn.addActionListener(e -> showAddDialog());
            actions.add(addBtn);
        }
        JButton refreshBtn = btn("🔄", null);
        refreshBtn.addActionListener(e -> loadData());
        actions.add(refreshBtn);

        topBar.add(title, BorderLayout.WEST);
        topBar.add(actions, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        if (RBACManager.hasPermission(Module.DOCTORS, Permission.EDIT)) {
            JButton editBtn = btn("✏️ Edit", new Color(255, 152, 0));
            editBtn.addActionListener(e -> showEditDialog());
            bottomBar.add(editBtn);
        }
        if (RBACManager.hasPermission(Module.DOCTORS, Permission.DELETE)) {
            JButton deleteBtn = btn("🗑️ Delete", new Color(244, 67, 54));
            deleteBtn.addActionListener(e -> deleteSelected());
            bottomBar.add(deleteBtn);
        }
        add(bottomBar, BorderLayout.SOUTH);

        loadData();
    }

    private void loadData() {
        tableModel.setRowCount(0);
        for (Object[] row : dao.findAll()) tableModel.addRow(row);
    }

    private void showAddDialog() {
        if (!RBACManager.requirePermission(Module.DOCTORS, Permission.CREATE, this)) return;

        JTextField fFirst = new JTextField(), fLast = new JTextField(), fEmail = new JTextField(),
                fPhone = new JTextField(), fSpec = new JTextField(), fLicense = new JTextField(),
                fFee = new JTextField("500"), fDuration = new JTextField("30");

        List<String[]> depts = deptDao.findAllForCombo();
        JComboBox<String> fDept = new JComboBox<>(depts.stream().map(d -> d[1]).toArray(String[]::new));

        Object[] fields = {
            "First Name*:", fFirst, "Last Name*:", fLast, "Email*:", fEmail,
            "Phone*:", fPhone, "Specialization*:", fSpec, "License Number*:", fLicense,
            "Department:", fDept, "Consultation Fee:", fFee, "Duration (min):", fDuration
        };

        int res = JOptionPane.showConfirmDialog(this, fields, "Add Doctor", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            if (fFirst.getText().trim().isEmpty() || fEmail.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Required fields missing."); return;
            }
            int deptIdx = fDept.getSelectedIndex();
            int deptId = deptIdx >= 0 ? Integer.parseInt(depts.get(deptIdx)[0]) : 0;
            dao.create(fFirst.getText().trim(), fLast.getText().trim(), fEmail.getText().trim(),
                    fPhone.getText().trim(), fSpec.getText().trim(), fLicense.getText().trim(),
                    deptId, Double.parseDouble(fFee.getText().trim()),
                    Integer.parseInt(fDuration.getText().trim()));
            loadData();
        }
    }

    private void showEditDialog() {
        if (!RBACManager.requirePermission(Module.DOCTORS, Permission.EDIT, this)) return;

        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a doctor first."); return; }
        int id = (int) tableModel.getValueAt(row, 0);
        Object[] d = dao.findById(id);
        if (d == null) return;

        JTextField fFirst = new JTextField((String) d[1]), fLast = new JTextField((String) d[2]),
                fEmail = new JTextField((String) d[3]), fPhone = new JTextField((String) d[4]),
                fSpec = new JTextField((String) d[5]),
                fFee = new JTextField(String.valueOf(d[8])), fDuration = new JTextField(String.valueOf(d[9]));

        List<String[]> depts = deptDao.findAllForCombo();
        JComboBox<String> fDept = new JComboBox<>(depts.stream().map(dp -> dp[1]).toArray(String[]::new));

        Object[] fields = {
            "First Name:", fFirst, "Last Name:", fLast, "Email:", fEmail,
            "Phone:", fPhone, "Specialization:", fSpec,
            "Department:", fDept, "Fee:", fFee, "Duration:", fDuration
        };

        int res = JOptionPane.showConfirmDialog(this, fields, "Edit Doctor", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            int deptIdx = fDept.getSelectedIndex();
            int deptId = deptIdx >= 0 ? Integer.parseInt(depts.get(deptIdx)[0]) : 0;
            dao.update(id, fFirst.getText().trim(), fLast.getText().trim(), fEmail.getText().trim(),
                    fPhone.getText().trim(), fSpec.getText().trim(), deptId,
                    Double.parseDouble(fFee.getText().trim()), Integer.parseInt(fDuration.getText().trim()));
            loadData();
        }
    }

    private void deleteSelected() {
        if (!RBACManager.requirePermission(Module.DOCTORS, Permission.DELETE, this)) return;

        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a doctor first."); return; }
        int id = (int) tableModel.getValueAt(row, 0);
        if (JOptionPane.showConfirmDialog(this, "Delete this doctor?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            dao.delete(id); loadData();
        }
    }

    private JButton btn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (bg != null) { b.setBackground(bg); b.setForeground(Color.WHITE); }
        return b;
    }
}
