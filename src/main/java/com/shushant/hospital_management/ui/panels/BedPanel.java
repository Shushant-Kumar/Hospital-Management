package com.shushant.hospital_management.ui.panels;

import com.shushant.hospital_management.dao.BedDao;
import com.shushant.hospital_management.dao.PatientDao;
import com.shushant.hospital_management.util.RBACManager;
import com.shushant.hospital_management.util.RBACManager.Module;
import com.shushant.hospital_management.util.RBACManager.Permission;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class BedPanel extends JPanel {

    private final BedDao dao = new BedDao();
    private final PatientDao patientDao = new PatientDao();
    private final DefaultTableModel tableModel;
    private final JTable table;

    private static final String[] COLUMNS = {"ID", "Ward", "Bed#", "Room Type", "Floor", "Status", "Daily Rate", "Patient"};

    public BedPanel() {
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel topBar = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Beds & Wards");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(100, 180, 255));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        if (RBACManager.hasPermission(Module.BEDS, Permission.CREATE)) {
            JButton addBtn = btn("➕ Add Bed", new Color(76, 175, 80));
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
        table.setRowHeight(28); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        if (RBACManager.hasPermission(Module.BEDS, Permission.ASSIGN_BED)) {
            JButton assignBtn = btn("🏥 Assign Patient", new Color(33, 150, 243));
            assignBtn.addActionListener(e -> assignPatient());
            bottomBar.add(assignBtn);
        }
        if (RBACManager.hasPermission(Module.BEDS, Permission.RELEASE_BED)) {
            JButton releaseBtn = btn("✅ Release Bed", new Color(76, 175, 80));
            releaseBtn.addActionListener(e -> releaseBed());
            bottomBar.add(releaseBtn);
        }
        add(bottomBar, BorderLayout.SOUTH);

        loadData();
    }

    private void loadData() {
        tableModel.setRowCount(0);
        for (Object[] row : dao.findAll()) tableModel.addRow(row);
    }

    private void showAddDialog() {
        if (!RBACManager.requirePermission(Module.BEDS, Permission.CREATE, this)) return;

        JTextField fWard = new JTextField(), fBed = new JTextField(), fFloor = new JTextField("1"),
                fRate = new JTextField("500");
        JComboBox<String> fRoom = new JComboBox<>(new String[]{"GENERAL", "SEMI_PRIVATE", "PRIVATE", "ICU", "NICU"});

        Object[] fields = { "Ward Name*:", fWard, "Bed Number*:", fBed, "Room Type:", fRoom,
            "Floor:", fFloor, "Daily Rate:", fRate };

        if (JOptionPane.showConfirmDialog(this, fields, "Add Bed", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            dao.create(fWard.getText().trim(), fBed.getText().trim(), (String) fRoom.getSelectedItem(),
                    Integer.parseInt(fFloor.getText().trim()), Double.parseDouble(fRate.getText().trim()));
            loadData();
        }
    }

    private void assignPatient() {
        if (!RBACManager.requirePermission(Module.BEDS, Permission.ASSIGN_BED, this)) return;

        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a bed first."); return; }
        if (!"AVAILABLE".equals(tableModel.getValueAt(row, 5))) {
            JOptionPane.showMessageDialog(this, "This bed is not available."); return;
        }
        int bedId = (int) tableModel.getValueAt(row, 0);
        List<Object[]> patients = patientDao.findAll();
        if (patients.isEmpty()) { JOptionPane.showMessageDialog(this, "No patients found."); return; }

        JComboBox<String> fPatient = new JComboBox<>(patients.stream()
                .map(p -> p[2] + " " + p[3] + " (" + p[1] + ")").toArray(String[]::new));
        Object[] fields = { "Select Patient:", fPatient };
        if (JOptionPane.showConfirmDialog(this, fields, "Assign Patient", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                int patientId = (int) patients.get(fPatient.getSelectedIndex())[0];
                dao.assignPatient(bedId, patientId);
                loadData();
            } catch (IllegalStateException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Assignment Error", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void releaseBed() {
        if (!RBACManager.requirePermission(Module.BEDS, Permission.RELEASE_BED, this)) return;

        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a bed first."); return; }
        int bedId = (int) tableModel.getValueAt(row, 0);
        if ("AVAILABLE".equals(tableModel.getValueAt(row, 5))) {
            JOptionPane.showMessageDialog(this, "Bed is already available."); return;
        }
        try {
            dao.releaseBed(bedId);
            loadData();
        } catch (IllegalStateException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Release Error", JOptionPane.WARNING_MESSAGE);
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
