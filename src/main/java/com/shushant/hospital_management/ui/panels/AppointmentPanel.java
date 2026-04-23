package com.shushant.hospital_management.ui.panels;

import com.shushant.hospital_management.dao.AppointmentDao;
import com.shushant.hospital_management.dao.DoctorDao;
import com.shushant.hospital_management.dao.PatientDao;
import com.shushant.hospital_management.util.RBACManager;
import com.shushant.hospital_management.util.RBACManager.Module;
import com.shushant.hospital_management.util.RBACManager.Permission;
import com.shushant.hospital_management.util.SessionManager;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class AppointmentPanel extends JPanel {

    private final AppointmentDao dao = new AppointmentDao();
    private final DoctorDao doctorDao = new DoctorDao();
    private final PatientDao patientDao = new PatientDao();
    private final DefaultTableModel tableModel;
    private final JTable table;

    private static final String[] COLUMNS = {"ID", "Patient", "Doctor", "Date", "Time", "Status", "Token"};

    public AppointmentPanel() {
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel topBar = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Appointment Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(100, 180, 255));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        if (RBACManager.hasPermission(Module.APPOINTMENTS, Permission.CREATE)) {
            JButton bookBtn = btn("📅 Book Appointment", new Color(76, 175, 80));
            bookBtn.addActionListener(e -> showBookDialog());
            actions.add(bookBtn);
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
        if (RBACManager.hasPermission(Module.APPOINTMENTS, Permission.CHECKIN_APPOINTMENT)) {
            JButton checkinBtn = btn("📋 Check-In", new Color(33, 150, 243));
            checkinBtn.addActionListener(e -> updateStatus("CHECKED_IN"));
            bottomBar.add(checkinBtn);
        }
        if (RBACManager.hasPermission(Module.APPOINTMENTS, Permission.COMPLETE_APPOINTMENT)) {
            JButton completeBtn = btn("✅ Complete", new Color(76, 175, 80));
            completeBtn.addActionListener(e -> updateStatus("COMPLETED"));
            bottomBar.add(completeBtn);
        }
        if (RBACManager.hasPermission(Module.APPOINTMENTS, Permission.CANCEL_APPOINTMENT)) {
            JButton cancelBtn = btn("❌ Cancel", new Color(244, 67, 54));
            cancelBtn.addActionListener(e -> cancelSelected());
            bottomBar.add(cancelBtn);
        }
        add(bottomBar, BorderLayout.SOUTH);

        loadData();
    }

    private void loadData() {
        tableModel.setRowCount(0);
        List<Object[]> data;
        // DOCTORs see only their own appointments
        if (RBACManager.isDoctorRole() && SessionManager.getCurrentDoctorId() > 0) {
            data = dao.findByDoctorId(SessionManager.getCurrentDoctorId());
        } else {
            data = dao.findAll();
        }
        for (Object[] row : data) tableModel.addRow(row);
    }

    private void showBookDialog() {
        if (!RBACManager.requirePermission(Module.APPOINTMENTS, Permission.CREATE, this)) return;

        List<Object[]> patients = patientDao.findAll();
        List<String[]> doctors = doctorDao.findAllForCombo();

        if (patients.isEmpty() || doctors.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Need at least one patient and one doctor to book.");
            return;
        }

        JComboBox<String> fPatient = new JComboBox<>(patients.stream()
                .map(r -> r[2] + " " + r[3] + " (" + r[1] + ")").toArray(String[]::new));
        JComboBox<String> fDoctor = new JComboBox<>(doctors.stream().map(d -> d[1]).toArray(String[]::new));
        JTextField fDate = new JTextField(LocalDate.now().toString());
        JTextField fTime = new JTextField(LocalTime.now().plusHours(1).withMinute(0).withSecond(0).toString().substring(0, 5));
        JTextField fNotes = new JTextField();
        JCheckBox fWalkIn = new JCheckBox("Walk-in patient");

        Object[] fields = { "Patient:", fPatient, "Doctor:", fDoctor,
            "Date (yyyy-mm-dd):", fDate, "Time (HH:mm):", fTime,
            "Notes:", fNotes, fWalkIn };

        int res = JOptionPane.showConfirmDialog(this, fields, "Book Appointment", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            try {
                int patientId = (int) patients.get(fPatient.getSelectedIndex())[0];
                int doctorId = Integer.parseInt(doctors.get(fDoctor.getSelectedIndex())[0]);
                Date date = Date.valueOf(fDate.getText().trim());
                Time startTime = Time.valueOf(fTime.getText().trim() + ":00");
                Time endTime = Time.valueOf(LocalTime.parse(fTime.getText().trim()).plusMinutes(30).toString());

                if (dao.hasConflict(doctorId, date, startTime, endTime)) {
                    JOptionPane.showMessageDialog(this, "Doctor has a conflicting appointment at this time!", "Conflict", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                int token = dao.getNextToken(doctorId, date);
                dao.create(patientId, doctorId, date, startTime, endTime, token, fNotes.getText().trim(),
                        fWalkIn.isSelected(), SessionManager.getCurrentUserId());
                JOptionPane.showMessageDialog(this, "Appointment booked! Token: " + token, "Success", JOptionPane.INFORMATION_MESSAGE);
                loadData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void cancelSelected() {
        if (!RBACManager.requirePermission(Module.APPOINTMENTS, Permission.CANCEL_APPOINTMENT, this)) return;

        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select an appointment first."); return; }
        int id = (int) tableModel.getValueAt(row, 0);

        // DOCTORs can only cancel their own
        if (RBACManager.isDoctorRole() && !dao.belongsToDoctor(id, SessionManager.getCurrentDoctorId())) {
            JOptionPane.showMessageDialog(this, "You can only cancel your own appointments.", "Access Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String reason = JOptionPane.showInputDialog(this, "Cancel reason:");
        if (reason != null && !reason.trim().isEmpty()) {
            dao.updateStatus(id, "CANCELLED", reason.trim());
            loadData();
        }
    }

    private void updateStatus(String status) {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select an appointment first."); return; }
        int id = (int) tableModel.getValueAt(row, 0);

        // DOCTORs can only update their own
        if (RBACManager.isDoctorRole() && !dao.belongsToDoctor(id, SessionManager.getCurrentDoctorId())) {
            JOptionPane.showMessageDialog(this, "You can only modify your own appointments.", "Access Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }

        dao.updateStatus(id, status, null);
        loadData();
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
