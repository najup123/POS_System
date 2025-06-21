package com.pujanmapase;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;

public class MainApplication extends JFrame {
 private JTabbedPane tabbedPane;
 
 public MainApplication() throws SQLException {
     setTitle("Pujan Mapase Centre - POS System");
     setSize(1024, 768);
     setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
     setLocationRelativeTo(null);
     
     // Initialize database
     DatabaseHelper.initializeDB();
     
     // Create tabbed pane
     tabbedPane = new JTabbedPane();
     
     // Add tabs
     tabbedPane.addTab("Point of Sale", new PointOfSalePanel());
     tabbedPane.addTab("Inventory", new InventoryPanel());
     tabbedPane.addTab("Sales History", new SalesHistoryPanel());
     
     add(tabbedPane);
     
     // Add menu bar
     JMenuBar menuBar = new JMenuBar();
     
     JMenu fileMenu = new JMenu("File");
     JMenuItem exitItem = new JMenuItem("Exit");
     exitItem.addActionListener(e -> System.exit(0));
     fileMenu.add(exitItem);
     
     JMenu helpMenu = new JMenu("Help");
     JMenuItem aboutItem = new JMenuItem("About");
     aboutItem.addActionListener(e -> JOptionPane.showMessageDialog(this, 
         "Pujan Mapase Centre POS System\nVersion 1.0", "About", JOptionPane.INFORMATION_MESSAGE));
     helpMenu.add(aboutItem);
     
     menuBar.add(fileMenu);
     menuBar.add(helpMenu);
     
     setJMenuBar(menuBar);
 }
 
 public static void main(String[] args) {
     SwingUtilities.invokeLater(() -> {
         MainApplication app = null;
		try {
			app = new MainApplication();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
         app.setVisible(true);
     });
 }
}