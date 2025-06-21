package com.pujanmapase;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import javax.swing.table.DefaultTableModel;

public class InventoryPanel extends JPanel {
 private JTable productsTable;
 
 public InventoryPanel() {
     setLayout(new BorderLayout());
     
     // Main table
     productsTable = new JTable();
     refreshProductsTable();
     JScrollPane scrollPane = new JScrollPane(productsTable);
     add(scrollPane, BorderLayout.CENTER);
     
     // Button panel
     JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
     
     JButton addButton = new JButton("Add Product");
     JButton editButton = new JButton("Edit Product");
     JButton deleteButton = new JButton("Delete Product");
     JButton refreshButton = new JButton("Refresh");
     
     // Add product button
     addButton.addActionListener(e -> showProductDialog(null));
     
     // Edit product button
     editButton.addActionListener(e -> {
         int selectedRow = productsTable.getSelectedRow();
         if (selectedRow >= 0) {
             int id = (int) productsTable.getValueAt(selectedRow, 0);
             showProductDialog(id);
         } else {
             JOptionPane.showMessageDialog(this, "Please select a product to edit", 
                 "Error", JOptionPane.ERROR_MESSAGE);
         }
     });
     
     // Delete product button
     deleteButton.addActionListener(e -> {
         int selectedRow = productsTable.getSelectedRow();
         if (selectedRow >= 0) {
             int id = (int) productsTable.getValueAt(selectedRow, 0);
             String name = (String) productsTable.getValueAt(selectedRow, 1);
             
             int confirm = JOptionPane.showConfirmDialog(this, 
                 "Are you sure you want to delete " + name + "?", 
                 "Confirm Delete", JOptionPane.YES_NO_OPTION);
             
             if (confirm == JOptionPane.YES_OPTION) {
                 deleteProduct(id);
             }
         } else {
             JOptionPane.showMessageDialog(this, "Please select a product to delete", 
                 "Error", JOptionPane.ERROR_MESSAGE);
         }
     });
     
     // Refresh button
     refreshButton.addActionListener(e -> refreshProductsTable());
     
     buttonPanel.add(addButton);
     buttonPanel.add(editButton);
     buttonPanel.add(deleteButton);
     buttonPanel.add(refreshButton);
     
     add(buttonPanel, BorderLayout.SOUTH);
 }
 
 private void refreshProductsTable() {
     try (Connection conn = DatabaseHelper.getConnection();
          Statement stmt = conn.createStatement();
          ResultSet rs = stmt.executeQuery("SELECT id, name, category, price, stock, alcohol_content, volume_ml FROM products")) {
         
         DefaultTableModel model = new DefaultTableModel(new Object[]{
             "ID", "Name", "Category", "Price", "Stock", "Alcohol %", "Volume (ml)"}, 0) {
             @Override
             public Class<?> getColumnClass(int columnIndex) {
                 return switch (columnIndex) {
                     case 0 -> Integer.class;
                     case 3 -> Double.class;
                     case 4 -> Integer.class;
                     case 5 -> Double.class;
                     case 6 -> Integer.class;
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
                 rs.getInt("stock"),
                 rs.getDouble("alcohol_content"),
                 rs.getInt("volume_ml")
             });
         }
         
         productsTable.setModel(model);
         
     } catch (SQLException e) {
         JOptionPane.showMessageDialog(this, "Error loading products: " + e.getMessage(), 
             "Error", JOptionPane.ERROR_MESSAGE);
     }
 }
 
 private void showProductDialog(Integer productId) {
     JDialog dialog = new JDialog();
     dialog.setTitle(productId == null ? "Add New Product" : "Edit Product");
     dialog.setModal(true);
     dialog.setSize(400, 350);
     dialog.setLocationRelativeTo(this);
     
     JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
     panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
     
     // Form fields
     JTextField nameField = new JTextField();
     JComboBox<String> categoryField = new JComboBox<>(new String[]{
         "Beer", "Wine", "Whisky", "Vodka", "Rum", "Gin", "Tequila", "Brandy", "Other"
     });
     JTextField priceField = new JTextField();
     JTextField stockField = new JTextField();
     JTextField alcoholField = new JTextField();
     JTextField volumeField = new JTextField();
     
     // If editing, load existing data
     if (productId != null) {
         try (Connection conn = DatabaseHelper.getConnection();
              PreparedStatement stmt = conn.prepareStatement(
                  "SELECT name, category, price, stock, alcohol_content, volume_ml FROM products WHERE id = ?")) {
             
             stmt.setInt(1, productId);
             ResultSet rs = stmt.executeQuery();
             
             if (rs.next()) {
                 nameField.setText(rs.getString("name"));
                 categoryField.setSelectedItem(rs.getString("category"));
                 priceField.setText(String.valueOf(rs.getDouble("price")));
                 stockField.setText(String.valueOf(rs.getInt("stock")));
                 alcoholField.setText(String.valueOf(rs.getDouble("alcohol_content")));
                 volumeField.setText(String.valueOf(rs.getInt("volume_ml")));
             }
             
         } catch (SQLException e) {
             JOptionPane.showMessageDialog(dialog, "Error loading product: " + e.getMessage(), 
                 "Error", JOptionPane.ERROR_MESSAGE);
         }
     }
     
     // Add fields to panel
     panel.add(new JLabel("Product Name:"));
     panel.add(nameField);
     panel.add(new JLabel("Category:"));
     panel.add(categoryField);
     panel.add(new JLabel("Price (Rs):"));
     panel.add(priceField);
     panel.add(new JLabel("Stock:"));
     panel.add(stockField);
     panel.add(new JLabel("Alcohol Content (%):"));
     panel.add(alcoholField);
     panel.add(new JLabel("Volume (ml):"));
     panel.add(volumeField);
     
     // Buttons
     JButton saveButton = new JButton("Save");
     JButton cancelButton = new JButton("Cancel");
     
     saveButton.addActionListener(e -> {
         try {
             String name = nameField.getText().trim();
             String category = (String) categoryField.getSelectedItem();
             double price = Double.parseDouble(priceField.getText());
             int stock = Integer.parseInt(stockField.getText());
             double alcoholContent = Double.parseDouble(alcoholField.getText());
             int volume = Integer.parseInt(volumeField.getText());
             
             if (name.isEmpty()) {
                 throw new IllegalArgumentException("Product name cannot be empty");
             }
             
             if (productId == null) {
                 addProduct(name, category, price, stock, alcoholContent, volume);
             } else {
                 updateProduct(productId, name, category, price, stock, alcoholContent, volume);
             }
             
             dialog.dispose();
             refreshProductsTable();
             
         } catch (NumberFormatException ex) {
             JOptionPane.showMessageDialog(dialog, "Please enter valid numbers for price, stock, alcohol content and volume", 
                 "Error", JOptionPane.ERROR_MESSAGE);
         } catch (IllegalArgumentException ex) {
             JOptionPane.showMessageDialog(dialog, ex.getMessage(), 
                 "Error", JOptionPane.ERROR_MESSAGE);
         } catch (SQLException ex) {
             JOptionPane.showMessageDialog(dialog, "Database error: " + ex.getMessage(), 
                 "Error", JOptionPane.ERROR_MESSAGE);
         }
     });
     
     cancelButton.addActionListener(e -> dialog.dispose());
     
     JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
     buttonPanel.add(saveButton);
     buttonPanel.add(cancelButton);
     
     dialog.add(panel, BorderLayout.CENTER);
     dialog.add(buttonPanel, BorderLayout.SOUTH);
     
     dialog.setVisible(true);
 }
 
 private void addProduct(String name, String category, double price, int stock, 
                       double alcoholContent, int volume) throws SQLException {
     try (Connection conn = DatabaseHelper.getConnection();
          PreparedStatement stmt = conn.prepareStatement(
              "INSERT INTO products (name, category, price, stock, alcohol_content, volume_ml) " +
              "VALUES (?, ?, ?, ?, ?, ?)")) {
         
         stmt.setString(1, name);
         stmt.setString(2, category);
         stmt.setDouble(3, price);
         stmt.setInt(4, stock);
         stmt.setDouble(5, alcoholContent);
         stmt.setInt(6, volume);
         
         stmt.executeUpdate();
         
         JOptionPane.showMessageDialog(this, "Product added successfully", 
             "Success", JOptionPane.INFORMATION_MESSAGE);
     }
 }
 
 private void updateProduct(int id, String name, String category, double price, int stock, 
                          double alcoholContent, int volume) throws SQLException {
     try (Connection conn = DatabaseHelper.getConnection();
          PreparedStatement stmt = conn.prepareStatement(
              "UPDATE products SET name = ?, category = ?, price = ?, stock = ?, " +
              "alcohol_content = ?, volume_ml = ? WHERE id = ?")) {
         
         stmt.setString(1, name);
         stmt.setString(2, category);
         stmt.setDouble(3, price);
         stmt.setInt(4, stock);
         stmt.setDouble(5, alcoholContent);
         stmt.setInt(6, volume);
         stmt.setInt(7, id);
         
         stmt.executeUpdate();
         
         JOptionPane.showMessageDialog(this, "Product updated successfully", 
             "Success", JOptionPane.INFORMATION_MESSAGE);
     }
 }
 
 private void deleteProduct(int id) {
     try (Connection conn = DatabaseHelper.getConnection();
          PreparedStatement stmt = conn.prepareStatement("DELETE FROM products WHERE id = ?")) {
         
         stmt.setInt(1, id);
         int rowsAffected = stmt.executeUpdate();
         
         if (rowsAffected > 0) {
             JOptionPane.showMessageDialog(this, "Product deleted successfully", 
                 "Success", JOptionPane.INFORMATION_MESSAGE);
             refreshProductsTable();
         }
         
     } catch (SQLException e) {
         JOptionPane.showMessageDialog(this, "Error deleting product: " + e.getMessage(), 
             "Error", JOptionPane.ERROR_MESSAGE);
     }
 }
}
