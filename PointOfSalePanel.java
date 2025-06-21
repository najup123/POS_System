package com.pujanmapase;



import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.table.DefaultTableModel;

public class PointOfSalePanel extends JPanel {
 private JTable productsTable;
 private JTable cartTable;
 private DefaultTableModel cartModel;
 private JLabel totalLabel;
 private double currentTotal = 0.0;
 
 public PointOfSalePanel() {
     setLayout(new BorderLayout());
     
     // Product selection panel
     JPanel productPanel = new JPanel(new BorderLayout());
     productPanel.setBorder(BorderFactory.createTitledBorder("Available Products"));
     
     // Product table
     productsTable = new JTable();
     refreshProductsTable();
     JScrollPane productScroll = new JScrollPane(productsTable);
     productPanel.add(productScroll, BorderLayout.CENTER);
     
     // Search panel
     JPanel searchPanel = new JPanel(new FlowLayout());
     JTextField searchField = new JTextField(20);
     JButton searchButton = new JButton("Search");
     
     searchButton.addActionListener(e -> {
         String searchTerm = searchField.getText().trim();
         if (!searchTerm.isEmpty()) {
             searchProducts(searchTerm);
         } else {
             refreshProductsTable();
         }
     });
     
     searchPanel.add(new JLabel("Search:"));
     searchPanel.add(searchField);
     searchPanel.add(searchButton);
     productPanel.add(searchPanel, BorderLayout.NORTH);
     
     // Cart panel
     JPanel cartPanel = new JPanel(new BorderLayout());
     cartPanel.setBorder(BorderFactory.createTitledBorder("Shopping Cart"));
     
     // Cart table
     cartModel = new DefaultTableModel(new Object[]{"ID", "Product", "Price", "Qty", "Subtotal"}, 0);
     cartTable = new JTable(cartModel);
     JScrollPane cartScroll = new JScrollPane(cartTable);
     cartPanel.add(cartScroll, BorderLayout.CENTER);
     
     // Total and buttons panel
     JPanel bottomPanel = new JPanel(new BorderLayout());
     
     totalLabel = new JLabel("Total: Rs 0.00", JLabel.RIGHT);
     bottomPanel.add(totalLabel, BorderLayout.NORTH);
     
     JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
     JButton addButton = new JButton("Add to Cart");
     JButton removeButton = new JButton("Remove");
     JButton clearButton = new JButton("Clear Cart");
     JButton checkoutButton = new JButton("Checkout");
     
     // Add to cart button
     addButton.addActionListener(e -> {
         int selectedRow = productsTable.getSelectedRow();
         if (selectedRow >= 0) {
             int id = (int) productsTable.getValueAt(selectedRow, 0);
             String name = (String) productsTable.getValueAt(selectedRow, 1);
             double price = (double) productsTable.getValueAt(selectedRow, 3);
             
             // Check if already in cart
             boolean found = false;
             for (int i = 0; i < cartModel.getRowCount(); i++) {
                 if ((int) cartModel.getValueAt(i, 0) == id) {
                     int qty = (int) cartModel.getValueAt(i, 3);
                     cartModel.setValueAt(qty + 1, i, 3);
                     cartModel.setValueAt(price * (qty + 1), i, 4);
                     found = true;
                     break;
                 }
             }
             
             if (!found) {
                 cartModel.addRow(new Object[]{id, name, price, 1, price});
             }
             
             updateTotal();
         }
     });
     
     // Remove button
     removeButton.addActionListener(e -> {
         int selectedRow = cartTable.getSelectedRow();
         if (selectedRow >= 0) {
             int qty = (int) cartModel.getValueAt(selectedRow, 3);
             if (qty > 1) {
                 double price = (double) cartModel.getValueAt(selectedRow, 2);
                 cartModel.setValueAt(qty - 1, selectedRow, 3);
                 cartModel.setValueAt(price * (qty - 1), selectedRow, 4);
             } else {
                 cartModel.removeRow(selectedRow);
             }
             updateTotal();
         }
     });
     
     // Clear cart button
     clearButton.addActionListener(e -> {
         cartModel.setRowCount(0);
         currentTotal = 0.0;
         totalLabel.setText("Total: Rs 0.00");
     });
     
     // Checkout button
     checkoutButton.addActionListener(e -> processCheckout());
     
     buttonPanel.add(addButton);
     buttonPanel.add(removeButton);
     buttonPanel.add(clearButton);
     buttonPanel.add(checkoutButton);
     bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
     
     cartPanel.add(bottomPanel, BorderLayout.SOUTH);
     
     // Add panels to main panel
     add(productPanel, BorderLayout.CENTER);
     add(cartPanel, BorderLayout.EAST);
 }
 
 private void refreshProductsTable() {
     try (Connection conn = DatabaseHelper.getConnection();
          Statement stmt = conn.createStatement();
          ResultSet rs = stmt.executeQuery("SELECT id, name, category, price, stock FROM products WHERE stock > 0")) {
         
         DefaultTableModel model = new DefaultTableModel(new Object[]{"ID", "Name", "Category", "Price", "Stock"}, 0) {
             @Override
             public Class<?> getColumnClass(int columnIndex) {
                 return switch (columnIndex) {
                     case 0 -> Integer.class;
                     case 3 -> Double.class;
                     case 4 -> Integer.class;
                     default -> String.class;
                 };
             }
         };
         
         while (rs.next()) {
             model.addRow(new Object[]{
                 rs.getInt("id"),
                 rs.getString("name"),
                 rs.getString("category"),
                 rs.getDouble("price"),
                 rs.getInt("stock")
             });
         }
         
         productsTable.setModel(model);
         
     } catch (SQLException e) {
         JOptionPane.showMessageDialog(this, "Error loading products: " + e.getMessage(), 
             "Error", JOptionPane.ERROR_MESSAGE);
     }
 }
 
 private void searchProducts(String searchTerm) {
     try (Connection conn = DatabaseHelper.getConnection();
          PreparedStatement stmt = conn.prepareStatement(
              "SELECT id, name, category, price, stock FROM products WHERE (name LIKE ? OR category LIKE ?) AND stock > 0")) {
         
         stmt.setString(1, "%" + searchTerm + "%");
         stmt.setString(2, "%" + searchTerm + "%");
         
         ResultSet rs = stmt.executeQuery();
         
         DefaultTableModel model = new DefaultTableModel(new Object[]{"ID", "Name", "Category", "Price", "Stock"}, 0) {
             @Override
             public Class<?> getColumnClass(int columnIndex) {
                 return switch (columnIndex) {
                     case 0 -> Integer.class;
                     case 3 -> Double.class;
                     case 4 -> Integer.class;
                     default -> String.class;
                 };
             }
         };
         
         while (rs.next()) {
             model.addRow(new Object[]{
                 rs.getInt("id"),
                 rs.getString("name"),
                 rs.getString("category"),
                 rs.getDouble("price"),
                 rs.getInt("stock")
             });
         }
         
         productsTable.setModel(model);
         
     } catch (SQLException e) {
         JOptionPane.showMessageDialog(this, "Error searching products: " + e.getMessage(), 
             "Error", JOptionPane.ERROR_MESSAGE);
     }
 }
 
 private void updateTotal() {
     currentTotal = 0.0;
     for (int i = 0; i < cartModel.getRowCount(); i++) {
         currentTotal += (double) cartModel.getValueAt(i, 4);
     }
     totalLabel.setText(String.format("Total: Rs %.2f", currentTotal));
 }
 
 private void processCheckout() {
     if (cartModel.getRowCount() == 0) {
         JOptionPane.showMessageDialog(this, "Cart is empty", "Error", JOptionPane.ERROR_MESSAGE);
         return;
     }
     
     // Payment method selection
     String[] options = {"Cash", "Credit Card", "Mobile Payment"};
     String paymentMethod = (String) JOptionPane.showInputDialog(this, 
         "Select payment method:", "Checkout", 
         JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
     
     if (paymentMethod == null) return;
     
     try (Connection conn = DatabaseHelper.getConnection()) {
         conn.setAutoCommit(false);
         
         // Create sale record
         String saleSql = "INSERT INTO sales (transaction_date, total_amount, payment_method) VALUES (?, ?, ?)";
         PreparedStatement saleStmt = conn.prepareStatement(saleSql, Statement.RETURN_GENERATED_KEYS);
         
         SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
         saleStmt.setString(1, sdf.format(new Date()));
         saleStmt.setDouble(2, currentTotal);
         saleStmt.setString(3, paymentMethod);
         
         saleStmt.executeUpdate();
         
         // Get generated sale ID
         int saleId;
         try (ResultSet generatedKeys = saleStmt.getGeneratedKeys()) {
             if (generatedKeys.next()) {
                 saleId = generatedKeys.getInt(1);
             } else {
                 throw new SQLException("Creating sale failed, no ID obtained.");
             }
         }
         
         // Add sale items
         String itemSql = "INSERT INTO sale_items (sale_id, product_id, quantity, price) VALUES (?, ?, ?, ?)";
         String updateStockSql = "UPDATE products SET stock = stock - ? WHERE id = ?";
         
         PreparedStatement itemStmt = conn.prepareStatement(itemSql);
         PreparedStatement updateStockStmt = conn.prepareStatement(updateStockSql);
         
         for (int i = 0; i < cartModel.getRowCount(); i++) {
             int productId = (int) cartModel.getValueAt(i, 0);
             int quantity = (int) cartModel.getValueAt(i, 3);
             double price = (double) cartModel.getValueAt(i, 2);
             
             // Add sale item
             itemStmt.setInt(1, saleId);
             itemStmt.setInt(2, productId);
             itemStmt.setInt(3, quantity);
             itemStmt.setDouble(4, price);
             itemStmt.addBatch();
             
             // Update stock
             updateStockStmt.setInt(1, quantity);
             updateStockStmt.setInt(2, productId);
             updateStockStmt.addBatch();
         }
         
         itemStmt.executeBatch();
         updateStockStmt.executeBatch();
         
         conn.commit();
         
         // Generate receipt
         generateReceipt(saleId);
         
         // Clear cart
         cartModel.setRowCount(0);
         currentTotal = 0.0;
         totalLabel.setText("Total: Rs 0.00");
         
         // Refresh products table
         refreshProductsTable();
         
         JOptionPane.showMessageDialog(this, "Sale completed successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
         
     } catch (SQLException e) {
         JOptionPane.showMessageDialog(this, "Error processing checkout: " + e.getMessage(), 
             "Error", JOptionPane.ERROR_MESSAGE);
     }
 }
 
 private void generateReceipt(int saleId) {
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
         
         // Build receipt text
         StringBuilder receipt = new StringBuilder();
         receipt.append("Pujan Mapase Centre\n");
         receipt.append("Alcohol Beverage Store\n");
         receipt.append("--------------------------------\n");
         receipt.append("Receipt #").append(saleId).append("\n");
         receipt.append("Date: ").append(transactionDate).append("\n");
         receipt.append("--------------------------------\n");
         receipt.append(String.format("%-20s %5s %10s %10s\n", "Item", "Qty", "Price", "Subtotal"));
         
         while (itemsRs.next()) {
             receipt.append(String.format("%-20s %5d %10.2f %10.2f\n", 
                 itemsRs.getString("name"),
                 itemsRs.getInt("quantity"),
                 itemsRs.getDouble("price"),
                 itemsRs.getDouble("subtotal")));
         }
         
         receipt.append("--------------------------------\n");
         receipt.append(String.format("%46s %.2f\n", "Total:", totalAmount));
         receipt.append(String.format("%46s %s\n", "Payment Method:", paymentMethod));
         receipt.append("--------------------------------\n");
         receipt.append("Thank you for your purchase!\n");
         
         // Show receipt
         JTextArea receiptArea = new JTextArea(receipt.toString());
         receiptArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
         JOptionPane.showMessageDialog(this, new JScrollPane(receiptArea), 
             "Receipt", JOptionPane.INFORMATION_MESSAGE);
         
     } catch (SQLException e) {
         JOptionPane.showMessageDialog(this, "Error generating receipt: " + e.getMessage(), 
             "Error", JOptionPane.ERROR_MESSAGE);
     }
 }
}
