package com.shushant.hospital_management.ui.panels;

import com.shushant.hospital_management.dao.*;
import com.shushant.hospital_management.util.SessionManager;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Patient self-service portal. Only visible to PATIENT-role users.
 * Shows only the logged-in patient's own data.
 */
public class PatientDashboardPanel extends JPanel {

    private final AppointmentDao appointmentDao = new AppointmentDao();
    private final DoctorDao doctorDao = new DoctorDao();
    private final BillingDao billingDao = new BillingDao();
    private final LabTestDao labTestDao = new LabTestDao();
    private final PatientDao patientDao = new PatientDao();

    private final int patientId;

    private DefaultTableModel apptModel;
    private DefaultTableModel billModel;
    private DefaultTableModel labModel;

    public PatientDashboardPanel() {
        this.patientId = SessionManager.getCurrentPatientId();
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        if (patientId <= 0) {
            add(new JLabel("⚠️ Your user account is not linked to a patient record. Contact admin.",
                    SwingConstants.CENTER), BorderLayout.CENTER);
            return;
        }

        buildUI();
    }

    private void buildUI() {
        // Header
        JPanel header = new JPanel(new BorderLayout());
        JLabel title = new JLabel("🏠 My Dashboard — " + SessionManager.getCurrentFullName());
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(new Color(100, 180, 255));
        JButton refreshBtn = btn("🔄 Refresh All", null);
        refreshBtn.addActionListener(e -> refreshAll());
        header.add(title, BorderLayout.WEST);
        header.add(refreshBtn, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Tabbed pane for different sections
        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        tabs.addTab("📅 My Appointments", buildAppointmentsTab());
        tabs.addTab("💳 My Bills", buildBillsTab());
        tabs.addTab("🔬 My Lab Reports", buildLabReportsTab());
        tabs.addTab("👤 My Profile", buildProfileTab());

        add(tabs, BorderLayout.CENTER);
    }

    // ── Appointments Tab ─────────────────────────────────────────────────────

    private JPanel buildAppointmentsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Actions
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton bookBtn = btn("📅 Book Appointment", new Color(76, 175, 80));
        bookBtn.addActionListener(e -> bookAppointment());
        JButton cancelBtn = btn("❌ Cancel Appointment", new Color(244, 67, 54));
        cancelBtn.addActionListener(e -> cancelAppointment());
        actions.add(bookBtn);
        actions.add(cancelBtn);
        panel.add(actions, BorderLayout.NORTH);

        // Table
        apptModel = new DefaultTableModel(
                new String[]{"ID", "Doctor", "Date", "Time", "Status", "Token"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable apptTable = new JTable(apptModel);
        apptTable.setRowHeight(28);
        apptTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        apptTable.getColumnModel().getColumn(0).setMaxWidth(50);
        panel.add(new JScrollPane(apptTable), BorderLayout.CENTER);

        loadAppointments();
        return panel;
    }

    private void loadAppointments() {
        apptModel.setRowCount(0);
        for (Object[] row : appointmentDao.findByPatientId(patientId)) {
            // Show: ID, Doctor, Date, Time, Status, Token (skip Patient column since it's always "me")
            apptModel.addRow(new Object[]{row[0], row[2], row[3], row[4], row[5], row[6]});
        }
    }

    private void bookAppointment() {
        List<String[]> doctors = doctorDao.findAllForCombo();
        if (doctors.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No doctors available."); return;
        }

        JComboBox<String> fDoctor = new JComboBox<>(doctors.stream().map(d -> d[1]).toArray(String[]::new));
        JTextField fDate = new JTextField(LocalDate.now().toString());
        JTextField fTime = new JTextField(LocalTime.now().plusHours(1).withMinute(0).withSecond(0).toString().substring(0, 5));
        JTextField fNotes = new JTextField();

        Object[] fields = {"Doctor:", fDoctor, "Date (yyyy-mm-dd):", fDate, "Time (HH:mm):", fTime, "Notes:", fNotes};

        int res = JOptionPane.showConfirmDialog(this, fields, "Book Appointment",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            try {
                int doctorId = Integer.parseInt(doctors.get(fDoctor.getSelectedIndex())[0]);
                Date date = Date.valueOf(fDate.getText().trim());
                Time startTime = Time.valueOf(fTime.getText().trim() + ":00");
                Time endTime = Time.valueOf(LocalTime.parse(fTime.getText().trim()).plusMinutes(30).toString());

                if (appointmentDao.hasConflict(doctorId, date, startTime, endTime)) {
                    JOptionPane.showMessageDialog(this, "Doctor has a conflicting appointment at this time!",
                            "Conflict", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                int token = appointmentDao.getNextToken(doctorId, date);
                appointmentDao.create(patientId, doctorId, date, startTime, endTime, token,
                        fNotes.getText().trim(), false, SessionManager.getCurrentUserId());
                JOptionPane.showMessageDialog(this, "Appointment booked! Token: " + token,
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                loadAppointments();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void cancelAppointment() {
        // Find the appointment table
        JTable apptTable = findTable(apptModel);
        if (apptTable == null) return;

        int row = apptTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select an appointment first."); return; }

        int apptId = (int) apptModel.getValueAt(row, 0);
        String status = (String) apptModel.getValueAt(row, 4);

        if ("COMPLETED".equals(status) || "CANCELLED".equals(status)) {
            JOptionPane.showMessageDialog(this, "Cannot cancel a " + status + " appointment.");
            return;
        }

        // Ownership check
        if (!appointmentDao.belongsToPatient(apptId, patientId)) {
            JOptionPane.showMessageDialog(this, "You can only cancel your own appointments.",
                    "Access Denied", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String reason = JOptionPane.showInputDialog(this, "Cancellation reason:");
        if (reason != null && !reason.trim().isEmpty()) {
            appointmentDao.updateStatus(apptId, "CANCELLED", reason.trim());
            JOptionPane.showMessageDialog(this, "Appointment cancelled.");
            loadAppointments();
        }
    }

    // ── Bills Tab ────────────────────────────────────────────────────────────

    private JPanel buildBillsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        billModel = new DefaultTableModel(
                new String[]{"ID", "Bill#", "Net Amount", "Paid", "Status", "Type", "Date"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable billTable = new JTable(billModel);
        billTable.setRowHeight(28);
        billTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        billTable.getColumnModel().getColumn(0).setMaxWidth(50);
        panel.add(new JScrollPane(billTable), BorderLayout.CENTER);

        loadBills();
        return panel;
    }

    private void loadBills() {
        billModel.setRowCount(0);
        for (Object[] row : billingDao.findByPatientId(patientId)) {
            // Skip patient name column (index 2) since it's always "me"
            billModel.addRow(new Object[]{row[0], row[1], row[3], row[4], row[5], row[6], row[7]});
        }
    }

    // ── Lab Reports Tab ──────────────────────────────────────────────────────

    private JPanel buildLabReportsTab() {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton viewBtn = btn("👁️ View Details", new Color(156, 39, 176));
        viewBtn.addActionListener(e -> viewLabDetails());
        actions.add(viewBtn);
        panel.add(actions, BorderLayout.NORTH);

        labModel = new DefaultTableModel(
                new String[]{"ID", "Doctor", "Test", "Code", "Status", "Date"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable labTable = new JTable(labModel);
        labTable.setRowHeight(28);
        labTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        labTable.getColumnModel().getColumn(0).setMaxWidth(50);
        panel.add(new JScrollPane(labTable), BorderLayout.CENTER);

        loadLabReports();
        return panel;
    }

    private void loadLabReports() {
        labModel.setRowCount(0);
        for (Object[] row : labTestDao.findByPatientId(patientId)) {
            // Skip patient name (index 1)
            labModel.addRow(new Object[]{row[0], row[2], row[3], row[4], row[5], row[6]});
        }
    }

    private void viewLabDetails() {
        JTable labTable = findTable(labModel);
        if (labTable == null) return;

        int row = labTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a test first."); return; }
        int id = (int) labModel.getValueAt(row, 0);
        Object[] d = labTestDao.findById(id);
        if (d == null) return;
        String info = """
            Doctor: %s
            Test: %s (%s)
            Sample: %s
            Status: %s
            Result: %s
            Normal Range: %s
            Technician: %s
            """.formatted(d[2], d[3], d[4], d[5], d[6], d[7], d[8], d[9]);
        JOptionPane.showMessageDialog(this, info, "Lab Test Details", JOptionPane.INFORMATION_MESSAGE);
    }

    // ── Profile Tab ──────────────────────────────────────────────────────────

    private JPanel buildProfileTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        Object[] data = patientDao.findById(patientId);
        if (data == null) {
            panel.add(new JLabel("Unable to load profile."));
            return panel;
        }

        JPanel card = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(42, 46, 56));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.setColor(new Color(60, 65, 75));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(8, 20, 8, 20);

        // Header
        JLabel nameLbl = new JLabel((String) data[2] + " " + data[3], SwingConstants.CENTER);
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 28));
        nameLbl.setForeground(Color.WHITE);
        
        JLabel uidLbl = new JLabel((String) data[1], SwingConstants.CENTER);
        uidLbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        uidLbl.setForeground(new Color(100, 180, 255));

        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        card.add(nameLbl, c);
        c.gridy = 1; c.insets = new Insets(0, 20, 20, 20);
        card.add(uidLbl, c);

        // Data fields
        c.gridwidth = 1; c.insets = new Insets(10, 20, 10, 20);
        int row = 2;

        addProfileRow(card, c, "Email", data[4], row++);
        addProfileRow(card, c, "Phone", data[5], row++);
        addProfileRow(card, c, "Date of Birth", data[6], row++);
        addProfileRow(card, c, "Gender", data[7], row++);
        addProfileRow(card, c, "Blood Group", data[8], row++);
        addProfileRow(card, c, "Address", data[9], row++);
        addProfileRow(card, c, "Patient Type", data[10], row++);
        addProfileRow(card, c, "Allergies", data[11], row++);
        addProfileRow(card, c, "Insurance", data[12] + " (" + data[13] + ")", row++);
        addProfileRow(card, c, "Emergency", data[14] + " (" + data[15] + ")", row++);

        GridBagConstraints wrapperGbc = new GridBagConstraints();
        wrapperGbc.anchor = GridBagConstraints.NORTH;
        wrapperGbc.weighty = 1.0;
        panel.add(card, wrapperGbc);

        return panel;
    }

    private void addProfileRow(JPanel panel, GridBagConstraints c, String label, Object value, int row) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbl.setForeground(new Color(150, 180, 220));
        
        JLabel val = new JLabel(value != null ? value.toString() : "—");
        val.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        val.setForeground(Color.WHITE);
        
        c.gridy = row;
        c.gridx = 0; c.anchor = GridBagConstraints.EAST;
        panel.add(lbl, c);
        
        c.gridx = 1; c.anchor = GridBagConstraints.WEST;
        panel.add(val, c);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void refreshAll() {
        if (patientId <= 0) return;
        loadAppointments();
        loadBills();
        loadLabReports();
    }

    private JTable findTable(DefaultTableModel model) {
        // Walk component tree to find the JTable using this model
        return findTableInContainer(this, model);
    }

    private JTable findTableInContainer(Container container, DefaultTableModel model) {
        for (Component c : container.getComponents()) {
            if (c instanceof JTable t && t.getModel() == model) return t;
            if (c instanceof Container sub) {
                JTable found = findTableInContainer(sub, model);
                if (found != null) return found;
            }
        }
        return null;
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
