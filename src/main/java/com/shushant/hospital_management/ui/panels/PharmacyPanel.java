package com.shushant.hospital_management.ui.panels;

import com.shushant.hospital_management.dao.PharmacyDao;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Date;

public class PharmacyPanel extends JPanel {

    private final PharmacyDao dao = new PharmacyDao();
    private final DefaultTableModel tableModel;
    private final JTable table;

    private static final String[] COLUMNS = {"ID", "Name", "Generic Name", "Manufacturer", "Qty", "Unit Price", "Expiry", "Re-order"};

    public PharmacyPanel() {
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel topBar = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Pharmacy & Inventory");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(new Color(100, 180, 255));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton addBtn = btn("➕ Add Medicine", new Color(76, 175, 80));
        addBtn.addActionListener(e -> showAddDialog());
        JButton alertsBtn = btn("⚠️ Low Stock", new Color(255, 152, 0));
        alertsBtn.addActionListener(e -> showLowStock());
        JButton refreshBtn = btn("🔄", null);
        refreshBtn.addActionListener(e -> loadData());
        actions.add(addBtn); actions.add(alertsBtn); actions.add(refreshBtn);
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
        JButton editBtn = btn("✏️ Edit", new Color(255, 152, 0));
        editBtn.addActionListener(e -> showEditDialog());
        bottomBar.add(editBtn);
        add(bottomBar, BorderLayout.SOUTH);

        loadData();
    }

    private void loadData() {
        tableModel.setRowCount(0);
        for (Object[] row : dao.findAll()) tableModel.addRow(row);
    }

    private void showAddDialog() {
        JTextField fName = new JTextField(), fGeneric = new JTextField(), fMfg = new JTextField(),
                fBatch = new JTextField(), fExpiry = new JTextField(), fQty = new JTextField("0"),
                fPrice = new JTextField("0"), fReorder = new JTextField("10");
        JComboBox<String> fCat = new JComboBox<>(new String[]{"TABLET", "CAPSULE", "SYRUP", "INJECTION", "CREAM", "OTHER"});

        Object[] fields = { "Name*:", fName, "Generic Name:", fGeneric, "Manufacturer:", fMfg,
            "Batch#:", fBatch, "Expiry (yyyy-mm-dd):", fExpiry,
            "Quantity:", fQty, "Unit Price:", fPrice, "Re-order Level:", fReorder, "Category:", fCat };

        if (JOptionPane.showConfirmDialog(this, fields, "Add Medicine", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            Date expiry = null;
            try { if (!fExpiry.getText().trim().isEmpty()) expiry = Date.valueOf(fExpiry.getText().trim()); } catch (Exception ignored) {}
            dao.create(fName.getText().trim(), fGeneric.getText().trim(), fMfg.getText().trim(),
                    fBatch.getText().trim(), expiry, Integer.parseInt(fQty.getText().trim()),
                    Double.parseDouble(fPrice.getText().trim()), Integer.parseInt(fReorder.getText().trim()),
                    (String) fCat.getSelectedItem());
            loadData();
        }
    }

    private void showEditDialog() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "Select a medicine first."); return; }
        int id = (int) tableModel.getValueAt(row, 0);
        JTextField fName = new JTextField((String) tableModel.getValueAt(row, 1));
        JTextField fGeneric = new JTextField((String) tableModel.getValueAt(row, 2));
        JTextField fMfg = new JTextField((String) tableModel.getValueAt(row, 3));
        JTextField fBatch = new JTextField(), fExpiry = new JTextField(tableModel.getValueAt(row, 6) != null ? tableModel.getValueAt(row, 6).toString() : "");
        JTextField fQty = new JTextField(String.valueOf(tableModel.getValueAt(row, 4)));
        JTextField fPrice = new JTextField(String.valueOf(tableModel.getValueAt(row, 5)));
        JTextField fReorder = new JTextField(String.valueOf(tableModel.getValueAt(row, 7)));
        JComboBox<String> fCat = new JComboBox<>(new String[]{"TABLET", "CAPSULE", "SYRUP", "INJECTION", "CREAM", "OTHER"});

        Object[] fields = { "Name:", fName, "Generic:", fGeneric, "Mfg:", fMfg,
            "Batch:", fBatch, "Expiry:", fExpiry, "Qty:", fQty, "Price:", fPrice,
            "Re-order:", fReorder, "Category:", fCat };

        if (JOptionPane.showConfirmDialog(this, fields, "Edit Medicine", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            Date expiry = null;
            try { if (!fExpiry.getText().trim().isEmpty()) expiry = Date.valueOf(fExpiry.getText().trim()); } catch (Exception ignored) {}
            dao.update(id, fName.getText().trim(), fGeneric.getText().trim(), fMfg.getText().trim(),
                    fBatch.getText().trim(), expiry, Integer.parseInt(fQty.getText().trim()),
                    Double.parseDouble(fPrice.getText().trim()), Integer.parseInt(fReorder.getText().trim()),
                    (String) fCat.getSelectedItem());
            loadData();
        }
    }

    private void showLowStock() {
        var low = dao.findLowStock();
        if (low.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All stock levels are OK!", "Low Stock", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        StringBuilder sb = new StringBuilder("⚠️ Low Stock Medicines:\n\n");
        for (Object[] r : low) sb.append("• ").append(r[1]).append(" — ").append(r[2]).append(" left (min: ").append(r[3]).append(")\n");
        JOptionPane.showMessageDialog(this, sb.toString(), "Low Stock Alert", JOptionPane.WARNING_MESSAGE);
    }

    private JButton btn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        b.setFocusPainted(false); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (bg != null) { b.setBackground(bg); b.setForeground(Color.WHITE); }
        return b;
    }
}
