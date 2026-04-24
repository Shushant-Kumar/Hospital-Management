package com.shushant.hospital_management.ui.panels;

import com.shushant.hospital_management.dao.DoctorDao;
import com.shushant.hospital_management.dao.LabTestDao;
import com.shushant.hospital_management.dao.PatientDao;
import com.shushant.hospital_management.util.RBACManager;
import com.shushant.hospital_management.util.RBACManager.Module;
import com.shushant.hospital_management.util.RBACManager.Permission;
import com.shushant.hospital_management.util.SessionManager;
import com.shushant.hospital_management.util.SecurityGuard;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class LabTestPanel extends JPanel {

    private final LabTestDao dao = new LabTestDao();
    private final PatientDao patientDao = new PatientDao();
    private final DoctorDao doctorDao = new DoctorDao();
    private final DefaultTableModel tableModel;
    private final JTable table;

    private static final String[] COLUMNS = {"ID", "Patient", "Doctor", "Test", "Code", "Status", "Date"};

    public LabTestPanel() {
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel topBar = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Laboratory Tests");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(100, 180, 255));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        if (RBACManager.hasPermission(Module.LAB_TESTS, Permission.CREATE)) {
            JButton orderBtn = btn("🔬 Order Test", new Color(76, 175, 80));
            orderBtn.addActionListener(e -> showOrderDialog());
            actions.add(orderBtn);
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
        table.setRowHeight(28); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        if (RBACManager.hasPermission(Module.LAB_TESTS, Permission.COLLECT_SAMPLE)) {
            JButton collectBtn = btn("🧪 Collect Sample", new Color(255, 152, 0));
            collectBtn.addActionListener(e -> updateStatus("SAMPLE_COLLECTED"));
            bottomBar.add(collectBtn);
        }
        if (RBACManager.hasPermission(Module.LAB_TESTS, Permission.PROCESS_LAB)) {
            JButton processBtn = btn("⚙️ Processing", new Color(33, 150, 243));
            processBtn.addActionListener(e -> updateStatus("PROCESSING"));
            bottomBar.add(processBtn);
        }
        if (RBACManager.hasPermission(Module.LAB_TESTS, Permission.ENTER_LAB_RESULT)) {
            JButton resultBtn = btn("📝 Enter Result", new Color(76, 175, 80));
            resultBtn.addActionListener(e -> enterResult());
            bottomBar.add(resultBtn);
        }
        JButton viewBtn = btn("👁️ View", new Color(156, 39, 176));
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

    private void showOrderDialog() {
        if (!RBACManager.requirePermission(Module.LAB_TESTS, Permission.CREATE, this)) return;

        List<Object[]> patients = patientDao.findAll();
        List<String[]> doctors = doctorDao.findAllForCombo();
        if (patients.isEmpty() || doctors.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Need patients and doctors to order tests."); return;
        }

        JComboBox<String> fPatient = new JComboBox<>(patients.stream()
                .map(p -> p[2] + " " + p[3]).toArray(String[]::new));
        JComboBox<String> fDoctor = new JComboBox<>(doctors.stream().map(d -> d[1]).toArray(String[]::new));
        JTextField fTest = new JTextField(), fCode = new JTextField();
        JComboBox<String> fSample = new JComboBox<>(new String[]{"BLOOD", "URINE", "STOOL", "SWAB", "TISSUE", "OTHER"});

        Object[] fields = { "Patient:", fPatient, "Ordering Doctor:", fDoctor,
            "Test Name*:", fTest, "Test Code:", fCode, "Sample Type:", fSample };

        if (JOptionPane.showConfirmDialog(this, fields, "Order Lab Test", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            int patientId = (int) patients.get(fPatient.getSelectedIndex())[0];
            int doctorId = Integer.parseInt(doctors.get(fDoctor.getSelectedIndex())[0]);
            dao.create(patientId, doctorId, fTest.getText().trim(), fCode.getText().trim(),
                    (String) fSample.getSelectedItem(), SessionManager.getCurrentUserId());
            loadData();
        }
    }

    private void updateStatus(String status) {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a test first."); return; }
        int id = (int) tableModel.getValueAt(row, 0);
        dao.updateStatus(id, status);
        loadData();
    }

    private void enterResult() {
        if (!RBACManager.requirePermission(Module.LAB_TESTS, Permission.ENTER_LAB_RESULT, this)) return;

        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a test first."); return; }
        int id = (int) tableModel.getValueAt(row, 0);
        JTextField fResult = new JTextField(), fNormal = new JTextField(), fTech = new JTextField();
        Object[] fields = { "Result*:", fResult, "Normal Range:", fNormal, "Technician:", fTech };
        if (JOptionPane.showConfirmDialog(this, fields, "Enter Test Result", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            dao.saveResult(id, fResult.getText().trim(), fNormal.getText().trim(), fTech.getText().trim());
            loadData();
        }
    }

    private void viewDetails() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a test first."); return; }
        int id = (int) tableModel.getValueAt(row, 0);
        try { SecurityGuard.verifyLabTestOwnership(id); } catch (SecurityException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Access Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Object[] d = dao.findById(id);
        if (d == null) return;
        String info = """
            Patient: %s
            Doctor: %s
            Test: %s (%s)
            Sample: %s
            Status: %s
            Result: %s
            Normal Range: %s
            Technician: %s
            """.formatted(d[1], d[2], d[3], d[4], d[5], d[6], d[7], d[8], d[9]);
        JOptionPane.showMessageDialog(this, info, "Lab Test Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private JButton btn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (bg != null) { b.setBackground(bg); b.setForeground(Color.WHITE); }
        return b;
    }
}
