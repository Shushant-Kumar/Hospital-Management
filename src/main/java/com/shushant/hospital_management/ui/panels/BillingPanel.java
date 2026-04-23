package com.shushant.hospital_management.ui.panels;

import com.shushant.hospital_management.dao.BillingDao;
import com.shushant.hospital_management.dao.PatientDao;
import com.shushant.hospital_management.util.RBACManager;
import com.shushant.hospital_management.util.RBACManager.Module;
import com.shushant.hospital_management.util.RBACManager.Permission;
import com.shushant.hospital_management.util.SessionManager;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class BillingPanel extends JPanel {

    private final BillingDao dao = new BillingDao();
    private final PatientDao patientDao = new PatientDao();
    private final DefaultTableModel tableModel;
    private final JTable table;

    private static final String[] COLUMNS = {"ID", "Bill#", "Patient", "Net Amount", "Paid", "Status", "Type", "Date"};

    public BillingPanel() {
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel topBar = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Billing & Payments");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(100, 180, 255));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        if (RBACManager.hasPermission(Module.BILLING, Permission.CREATE)) {
            JButton createBtn = btn("➕ Create Bill", new Color(76, 175, 80));
            createBtn.addActionListener(e -> showCreateBillDialog());
            actions.add(createBtn);
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
        if (RBACManager.hasPermission(Module.BILLING, Permission.RECORD_PAYMENT)) {
            JButton payBtn = btn("💳 Record Payment", new Color(0, 150, 136));
            payBtn.addActionListener(e -> recordPayment());
            bottomBar.add(payBtn);
        }
        add(bottomBar, BorderLayout.SOUTH);

        loadData();
    }

    private void loadData() {
        tableModel.setRowCount(0);
        for (Object[] row : dao.findAll()) tableModel.addRow(row);
    }

    private void showCreateBillDialog() {
        if (!RBACManager.requirePermission(Module.BILLING, Permission.CREATE, this)) return;

        List<Object[]> patients = patientDao.findAll();
        if (patients.isEmpty()) { JOptionPane.showMessageDialog(this, "No patients found."); return; }

        JComboBox<String> fPatient = new JComboBox<>(patients.stream()
                .map(p -> p[2] + " " + p[3] + " (" + p[1] + ")").toArray(String[]::new));
        JTextField fTotal = new JTextField("0"), fDiscount = new JTextField("0"), fTax = new JTextField("0");
        JComboBox<String> fType = new JComboBox<>(new String[]{"CONSULTATION", "SURGERY", "LAB", "PHARMACY", "BED_CHARGES", "OTHER"});

        Object[] fields = { "Patient:", fPatient, "Total Amount:", fTotal,
            "Discount:", fDiscount, "Tax Amount:", fTax, "Bill Type:", fType };

        if (JOptionPane.showConfirmDialog(this, fields, "Create Bill", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                int patientId = (int) patients.get(fPatient.getSelectedIndex())[0];
                double total = Double.parseDouble(fTotal.getText().trim());
                double discount = Double.parseDouble(fDiscount.getText().trim());
                double tax = Double.parseDouble(fTax.getText().trim());
                double net = total - discount + tax;
                dao.createBill(dao.generateBillNumber(), patientId, total, discount, tax, net,
                        (String) fType.getSelectedItem(), SessionManager.getCurrentUserId());
                loadData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage());
            }
        }
    }

    private void recordPayment() {
        if (!RBACManager.requirePermission(Module.BILLING, Permission.RECORD_PAYMENT, this)) return;

        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a bill first."); return; }
        int id = (int) tableModel.getValueAt(row, 0);
        JTextField fAmount = new JTextField();
        JComboBox<String> fMethod = new JComboBox<>(new String[]{"CASH", "CARD", "UPI", "BANK_TRANSFER", "CHEQUE"});
        JTextField fRef = new JTextField();

        Object[] fields = { "Amount:", fAmount, "Method:", fMethod, "Transaction Ref:", fRef };
        if (JOptionPane.showConfirmDialog(this, fields, "Record Payment", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            try {
                dao.recordPayment(id, Double.parseDouble(fAmount.getText().trim()),
                        (String) fMethod.getSelectedItem(), fRef.getText().trim());
                loadData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Invalid input: " + ex.getMessage());
            }
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
