package com.shushant.hospital_management.ui.panels;

import com.shushant.hospital_management.dao.PatientDao;
import com.shushant.hospital_management.util.RBACManager;
import com.shushant.hospital_management.util.RBACManager.Module;
import com.shushant.hospital_management.util.RBACManager.Permission;
import com.shushant.hospital_management.util.SessionManager;
import com.shushant.hospital_management.util.SecurityGuard;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Date;
import java.util.List;

public class PatientPanel extends JPanel {

    private final PatientDao dao = new PatientDao();
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JTextField searchField = new JTextField(20);

    private static final String[] COLUMNS = {"ID", "Patient UID", "First Name", "Last Name", "Phone", "Gender", "Type", "Created"};

    public PatientPanel() {
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Top bar
        JPanel topBar = new JPanel(new BorderLayout(10, 0));
        JLabel title = new JLabel("Patient Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(100, 180, 255));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchField.putClientProperty("JTextField.placeholderText", "Search patients...");
        searchField.addActionListener(e -> search());
        JButton searchBtn = createBtn("🔍 Search", new Color(33, 150, 243));
        searchBtn.addActionListener(e -> search());
        actions.add(searchField); actions.add(searchBtn);

        if (RBACManager.hasPermission(Module.PATIENTS, Permission.CREATE)) {
            JButton addBtn = createBtn("➕ Add Patient", new Color(76, 175, 80));
            addBtn.addActionListener(e -> showAddDialog());
            actions.add(addBtn);
        }

        JButton refreshBtn = createBtn("🔄", null);
        refreshBtn.addActionListener(e -> loadData());
        actions.add(refreshBtn);

        topBar.add(title, BorderLayout.WEST);
        topBar.add(actions, BorderLayout.EAST);
        add(topBar, BorderLayout.NORTH);

        // Table
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(28);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Bottom bar
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        if (RBACManager.hasPermission(Module.PATIENTS, Permission.EDIT)) {
            JButton editBtn = createBtn("✏️ Edit", new Color(255, 152, 0));
            editBtn.addActionListener(e -> showEditDialog());
            bottomBar.add(editBtn);
        }
        if (RBACManager.hasPermission(Module.PATIENTS, Permission.DELETE)) {
            JButton deleteBtn = createBtn("🗑️ Delete", new Color(244, 67, 54));
            deleteBtn.addActionListener(e -> deleteSelected());
            bottomBar.add(deleteBtn);
        }
        JButton viewBtn = createBtn("👁️ View Details", new Color(33, 150, 243));
        viewBtn.addActionListener(e -> viewDetails());
        bottomBar.add(viewBtn);
        add(bottomBar, BorderLayout.SOUTH);

        loadData();
    }

    private void loadData() {
        tableModel.setRowCount(0);
        List<Object[]> data;
        if (RBACManager.isDoctorRole()) {
            data = dao.findByDoctorId(SessionManager.getCurrentDoctorId());
        } else {
            data = dao.findAll();
        }
        for (Object[] row : data) tableModel.addRow(row);
    }

    private void search() {
        String q = searchField.getText().trim();
        tableModel.setRowCount(0);
        List<Object[]> results;
        if (q.isEmpty()) {
            results = RBACManager.isDoctorRole() ? dao.findByDoctorId(SessionManager.getCurrentDoctorId()) : dao.findAll();
        } else {
            results = RBACManager.isDoctorRole() ? dao.search(q, SessionManager.getCurrentDoctorId()) : dao.search(q);
        }
        for (Object[] row : results) tableModel.addRow(row);
    }

    private void showAddDialog() {
        if (!RBACManager.requirePermission(Module.PATIENTS, Permission.CREATE, this)) return;

        JTextField fFirst = new JTextField(), fLast = new JTextField(), fEmail = new JTextField(),
                fPhone = new JTextField(), fDob = new JTextField(), fAddress = new JTextField(),
                fAllergies = new JTextField(), fInsurer = new JTextField(), fPolicy = new JTextField(),
                fEmergName = new JTextField(), fEmergPhone = new JTextField();
        JComboBox<String> fGender = new JComboBox<>(new String[]{"MALE", "FEMALE", "OTHER"});
        JComboBox<String> fBlood = new JComboBox<>(new String[]{"", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"});
        JComboBox<String> fType = new JComboBox<>(new String[]{"OPD", "IPD", "EMERGENCY"});

        Object[] fields = {
            "First Name*:", fFirst, "Last Name*:", fLast, "Email:", fEmail,
            "Phone*:", fPhone, "Date of Birth (yyyy-mm-dd):", fDob,
            "Gender:", fGender, "Blood Group:", fBlood, "Address:", fAddress,
            "Patient Type:", fType, "Allergies:", fAllergies,
            "Insurance Provider:", fInsurer, "Policy Number:", fPolicy,
            "Emergency Contact Name:", fEmergName, "Emergency Contact Phone:", fEmergPhone
        };

        int result = JOptionPane.showConfirmDialog(this, fields, "Add New Patient", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            if (fFirst.getText().trim().isEmpty() || fLast.getText().trim().isEmpty() || fPhone.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "First name, last name, and phone are required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Date dob = null;
            try {
                if (!fDob.getText().trim().isEmpty()) {
                    dob = Date.valueOf(fDob.getText().trim());
                }
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "Invalid Date of Birth format. Use YYYY-MM-DD.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            dao.create(dao.generatePatientUid(), fFirst.getText().trim(), fLast.getText().trim(),
                    fEmail.getText().trim(), fPhone.getText().trim(), dob,
                    (String) fGender.getSelectedItem(), (String) fBlood.getSelectedItem(),
                    fAddress.getText().trim(), (String) fType.getSelectedItem(),
                    fAllergies.getText().trim(), fInsurer.getText().trim(), fPolicy.getText().trim(),
                    fEmergName.getText().trim(), fEmergPhone.getText().trim(),
                    SessionManager.getCurrentUserId());
            loadData();
        }
    }

    private void showEditDialog() {
        if (!RBACManager.requirePermission(Module.PATIENTS, Permission.EDIT, this)) return;

        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a patient first."); return; }
        int id = (int) tableModel.getValueAt(row, 0);
        try { SecurityGuard.verifyPatientAssignment(id); } catch (SecurityException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Access Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Object[] data = dao.findById(id);
        if (data == null) return;

        JTextField fFirst = new JTextField((String) data[2]), fLast = new JTextField((String) data[3]),
                fEmail = new JTextField((String) data[4]), fPhone = new JTextField((String) data[5]),
                fDob = new JTextField(data[6] != null ? data[6].toString() : ""),
                fAddress = new JTextField((String) data[9]),
                fAllergies = new JTextField((String) data[11]),
                fInsurer = new JTextField((String) data[12]), fPolicy = new JTextField((String) data[13]),
                fEmergName = new JTextField((String) data[14]), fEmergPhone = new JTextField((String) data[15]);
        JComboBox<String> fGender = new JComboBox<>(new String[]{"MALE", "FEMALE", "OTHER"});
        fGender.setSelectedItem(data[7]);
        JComboBox<String> fBlood = new JComboBox<>(new String[]{"", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"});
        fBlood.setSelectedItem(data[8]);
        JComboBox<String> fType = new JComboBox<>(new String[]{"OPD", "IPD", "EMERGENCY"});
        fType.setSelectedItem(data[10]);

        Object[] fields = {
            "First Name*:", fFirst, "Last Name*:", fLast, "Email:", fEmail,
            "Phone*:", fPhone, "Date of Birth:", fDob, "Gender:", fGender,
            "Blood Group:", fBlood, "Address:", fAddress, "Type:", fType,
            "Allergies:", fAllergies, "Insurance Provider:", fInsurer,
            "Policy Number:", fPolicy, "Emergency Name:", fEmergName, "Emergency Phone:", fEmergPhone
        };

        int result = JOptionPane.showConfirmDialog(this, fields, "Edit Patient", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            Date dob = null;
            try {
                if (!fDob.getText().trim().isEmpty()) {
                    dob = Date.valueOf(fDob.getText().trim());
                }
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, "Invalid Date of Birth format. Use YYYY-MM-DD.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            dao.update(id, fFirst.getText().trim(), fLast.getText().trim(), fEmail.getText().trim(),
                    fPhone.getText().trim(), dob, (String) fGender.getSelectedItem(),
                    (String) fBlood.getSelectedItem(), fAddress.getText().trim(),
                    (String) fType.getSelectedItem(), fAllergies.getText().trim(),
                    fInsurer.getText().trim(), fPolicy.getText().trim(),
                    fEmergName.getText().trim(), fEmergPhone.getText().trim());
            loadData();
        }
    }

    private void deleteSelected() {
        if (!RBACManager.requirePermission(Module.PATIENTS, Permission.DELETE, this)) return;

        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a patient first."); return; }
        int id = (int) tableModel.getValueAt(row, 0);
        int opt = JOptionPane.showConfirmDialog(this, "Delete this patient?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) { dao.delete(id); loadData(); }
    }

    private void viewDetails() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a patient first."); return; }
        int id = (int) tableModel.getValueAt(row, 0);
        try { SecurityGuard.verifyPatientAssignment(id); } catch (SecurityException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Access Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Object[] d = dao.findById(id);
        if (d == null) return;
        String info = """
            UID: %s
            Name: %s %s
            Email: %s | Phone: %s
            DOB: %s | Gender: %s | Blood: %s
            Address: %s
            Type: %s
            Allergies: %s
            Insurance: %s (%s)
            Emergency: %s (%s)
            """.formatted(d[1], d[2], d[3], d[4], d[5], d[6], d[7], d[8], d[9], d[10], d[11], d[12], d[13], d[14], d[15]);
        JOptionPane.showMessageDialog(this, info, "Patient Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private JButton createBtn(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (bg != null) { btn.setBackground(bg); btn.setForeground(Color.WHITE); }
        return btn;
    }
}
