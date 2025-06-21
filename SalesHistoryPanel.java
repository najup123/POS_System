
package com.pujanmapase;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.table.DefaultTableModel;

public class SalesHistoryPanel extends JPanel {
    private JTable salesTable;
    
    public SalesHistoryPanel() {
        setLayout(new BorderLayout());
        
        // Date filter panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JLabel fromLabel = new JLabel("From:");
        JTextField fromDateField = new JTextField(new SimpleDateFormat("yyyy-MM-dd").format(new Date()), 10);
        JLabel toLabel = new JLabel("To:");
        JTextField toDateField = new JTextField(new SimpleDateFormat("yyyy-MM-dd").format(new Date()), 10);
        JButton filterButton = new JButton("Filter");
        
        filterButton.addActionListener(e -> {
            String fromDate = fromDateField.getText().trim() + " 00:00:00";
            String toDate = toDateField.getText().trim() + " 23:59:59";
            refreshSalesTable(fromDate, toDate);
        });
        
        filterPanel.add(fromLabel);
        filterPanel.add(fromDateField);
        filterPanel.add(toLabel);
        filterPanel.add(toDateField);
        filterPanel.add(filterButton);
        
        add(filterPanel, BorderLayout.NORTH);
        
        // Sales table
        salesTable = new JTable();
        refreshSalesTable(null, null);
        JScrollPane scrollPane = new JScrollPane(salesTable);
        add(scrollPane, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton viewDetailsButton = new JButton("View Details");
        JButton refreshButton = new JButton("Refresh");
        
        // View details button
        viewDetailsButton.addActionListener(e -> {
            int selectedRow = salesTable.getSelectedRow();
            if (selectedRow >= 0) {
                int saleId = (int) salesTable.getValueAt(selectedRow, 0);
                viewSaleDetails(saleId);
            } else {
                JOptionPane.showMessageDialog(this, "Please select a sale to view details", 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        
        // Refresh button
        refreshButton.addActionListener(e -> refreshSalesTable(null, null));
        
        buttonPanel.add(viewDetailsButton);
        buttonPanel.add(refreshButton);
        
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void refreshSalesTable(String fromDate, String toDate) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            String sql = "SELECT id, transaction_date, total_amount, payment_method FROM sales";
            
            if (fromDate != null && toDate != null) {
                sql += " WHERE transaction_date BETWEEN ? AND ?";
            }
            
            sql += " ORDER BY transaction_date DESC";
            
            PreparedStatement stmt = conn.prepareStatement(sql);
            
            if (fromDate != null && toDate != null) {
                stmt.setString(1, fromDate);
                stmt.setString(2, toDate);
            }
            
            ResultSet rs = stmt.executeQuery();
            
            DefaultTableModel model = new DefaultTableModel(new Object[]{
                "ID", "Date", "Total Amount", "Payment Method"}, 0) {
                @Override
                public Class<?> getColumnClass(int columnIndex) {
                    return switch (columnIndex) {
                        case 0 -> Integer.class;
                        case 2 -> Double.class;
                        default -> String.class;
                    };
                }
            };
            
            SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            
            while (rs.next()) {
                Date transactionDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(rs.getString("transaction_date"));
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    displayFormat.format(transactionDate),
                    rs.getDouble("total_amount"),
                    rs.getString("payment_method")
                });
            }
            
            salesTable.setModel(model);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading sales: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void viewSaleDetails(int saleId) {
        try (Connection conn = DatabaseHelper.getConnection()) {
            // Get sale info
            String saleSql = "SELECT transaction_date, total_amount, payment_method FROM sales WHERE id = ?";
            PreparedStatement saleStmt = conn.prepareStatement(saleSql);
            saleStmt.setInt(1, saleId);
            ResultSet saleRs = saleStmt.executeQuery();
            
            if (!saleRs.next()) {
                throw new SQLException("Sale not found");
            }
            
            String transactionDate = saleRs.getString("transaction_date");
            double totalAmount = saleRs.getDouble("total_amount");
            String paymentMethod = saleRs.getString("payment_method");
            
            // Get sale items
            String itemsSql = "SELECT p.name, si.quantity, si.price, (si.quantity * si.price) as subtotal " +
                              "FROM sale_items si JOIN products p ON si.product_id = p.id " +
                              "WHERE si.sale_id = ?";
            PreparedStatement itemsStmt = conn.prepareStatement(itemsSql);
            itemsStmt.setInt(1, saleId);
            ResultSet itemsRs = itemsStmt.executeQuery();
            
            // Build details text
            StringBuilder details = new StringBuilder();
            details.append("Sale ID: ").append(saleId).append("\n");
            details.append("Date: ").append(transactionDate).append("\n");
            details.append("Payment Method: ").append(paymentMethod).append("\n");
            details.append("Total Amount: Rs ").append(String.format("%.2f", totalAmount)).append("\n\n");
            details.append("Items Purchased:\n");
            
            DefaultTableModel model = new DefaultTableModel(new Object[]{"Product", "Qty", "Price", "Subtotal"}, 0) {
                @Override
                public Class<?> getColumnClass(int columnIndex) {
                    return switch (columnIndex) {
                        case 1 -> Integer.class;
                        case 2, 3 -> Double.class;
                        default -> String.class;
                    };
                }
            };
            
            while (itemsRs.next()) {
                model.addRow(new Object[]{
                    itemsRs.getString("name"),
                    itemsRs.getInt("quantity"),
                    itemsRs.getDouble("price"),
                    itemsRs.getDouble("subtotal")
                });
            }
            
            JTable itemsTable = new JTable(model);
            
            // Create dialog
            JDialog dialog = new JDialog();
            dialog.setTitle("Sale Details");
            dialog.setModal(true);
            dialog.setSize(500, 400);
            dialog.setLocationRelativeTo(this);
            
            JPanel panel = new JPanel(new BorderLayout());
            
            JTextArea headerArea = new JTextArea(details.toString());
            headerArea.setEditable(false);
            panel.add(headerArea, BorderLayout.NORTH);
            
            panel.add(new JScrollPane(itemsTable), BorderLayout.CENTER);
            
            JButton closeButton = new JButton("Close");
            closeButton.addActionListener(e -> dialog.dispose());
            
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.add(closeButton);
            
            panel.add(buttonPanel, BorderLayout.SOUTH);
            
            dialog.add(panel);
            dialog.setVisible(true);
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading sale details: " + e.getMessage(), 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}