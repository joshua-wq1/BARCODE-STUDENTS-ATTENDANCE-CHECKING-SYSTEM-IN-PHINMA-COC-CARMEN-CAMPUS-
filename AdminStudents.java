package javabasics;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class AdminStudents extends JFrame {
    private static final String URL = "jdbc:mysql://localhost:3306/attendance_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    private JTextField barcodeField, nameField, sectionField, searchField;
    private JTable studentTable;
    private DefaultTableModel tableModel;
    private JCheckBox activeCheck;
    private JButton addButton, updateButton, deactivateButton, clearButton;
    private String adminUsername;

    public AdminStudents() {
        this("admin");
    }

    public AdminStudents(String adminUsername) {
        this.adminUsername = adminUsername;
        setTitle("Student Management - PHINMA COC Carmen Campus");
        setSize(1000, 650);
        setMinimumSize(new Dimension(800, 550));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        initializeUI();
        loadStudents("");
    }

    private Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL Connector/J is missing from NetBeans Libraries.", e);
        }
        return DriverManager.getConnection(URL, DB_USER, DB_PASS);
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(36, 52, 71));
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel("STUDENT MANAGEMENT");
        title.setFont(new Font("Arial", Font.BOLD, 22));
        title.setForeground(Color.WHITE);

        JButton backButton = new JButton("Back to Dashboard");
        backButton.addActionListener(e -> dispose());

        header.add(title, BorderLayout.WEST);
        header.add(backButton, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Student Information"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 7, 5, 7);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        barcodeField = new JTextField(18);
        nameField = new JTextField(18);
        sectionField = new JTextField(18);
        activeCheck = new JCheckBox("Active student", true);

        addFormRow(form, gbc, 0, "Student Barcode:", barcodeField);
        addFormRow(form, gbc, 1, "Full Name:", nameField);
        addFormRow(form, gbc, 2, "Section:", sectionField);

        gbc.gridx = 1;
        gbc.gridy = 3;
        form.add(activeCheck, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addButton = new JButton("Add Student");
        updateButton = new JButton("Update Student");
        deactivateButton = new JButton("Deactivate Student");
        clearButton = new JButton("Clear Form");

        addButton.addActionListener(e -> addStudent());
        updateButton.addActionListener(e -> updateStudent());
        deactivateButton.addActionListener(e -> deactivateStudent());
        clearButton.addActionListener(e -> clearForm());

        buttons.add(addButton);
        buttons.add(updateButton);
        buttons.add(deactivateButton);
        buttons.add(clearButton);

        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.add(form, BorderLayout.CENTER);
        leftPanel.add(buttons, BorderLayout.SOUTH);

        JPanel searchPanel = new JPanel(new BorderLayout(8, 5));
        searchField = new JTextField();
        JButton searchButton = new JButton("Search");
        JButton showAllButton = new JButton("Show All");

        searchButton.addActionListener(e -> loadStudents(searchField.getText().trim()));
        showAllButton.addActionListener(e -> {
            searchField.setText("");
            loadStudents("");
        });
        searchField.addActionListener(e -> loadStudents(searchField.getText().trim()));

        searchPanel.add(new JLabel("Search by barcode, name, or section:"), BorderLayout.WEST);

        JPanel searchControls = new JPanel(new BorderLayout(5, 0));
        searchControls.add(searchField, BorderLayout.CENTER);

        JPanel searchButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        searchButtons.add(searchButton);
        searchButtons.add(showAllButton);
        searchControls.add(searchButtons, BorderLayout.EAST);
        searchPanel.add(searchControls, BorderLayout.CENTER);

        tableModel = new DefaultTableModel(
                new String[]{"Barcode", "Full Name", "Section", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        studentTable = new JTable(tableModel);
        studentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        studentTable.setRowHeight(25);
        studentTable.setAutoCreateRowSorter(true);
        studentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && studentTable.getSelectedRow() != -1) {
                showSelectedStudent();
            }
        });

        JPanel tablePanel = new JPanel(new BorderLayout(5, 8));
        tablePanel.setBorder(BorderFactory.createTitledBorder("Student Records"));
        tablePanel.add(searchPanel, BorderLayout.NORTH);
        tablePanel.add(new JScrollPane(studentTable), BorderLayout.CENTER);

        content.add(leftPanel, BorderLayout.NORTH);
        content.add(tablePanel, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row,
                            String label, JTextField field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);
    }

    private void loadStudents(String keyword) {
        tableModel.setRowCount(0);

        String sql = "SELECT barcode, name, section, active FROM students " +
                "WHERE barcode LIKE ? OR name LIKE ? OR section LIKE ? " +
                "ORDER BY name";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String search = "%" + keyword + "%";
            ps.setString(1, search);
            ps.setString(2, search);
            ps.setString(3, search);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tableModel.addRow(new Object[]{
                        rs.getString("barcode"),
                        rs.getString("name"),
                        rs.getString("section"),
                        rs.getInt("active") == 1 ? "Active" : "Inactive"
                    });
                }
            }
        } catch (SQLException e) {
            showError("Unable to load student records.", e);
        }
    }

    private void addStudent() {
        String barcode = barcodeField.getText().trim();
        String name = nameField.getText().trim();
        String section = sectionField.getText().trim();

        if (barcode.isEmpty() || name.isEmpty() || section.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter the barcode, full name, and section.",
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "INSERT INTO students (barcode, name, section, active) VALUES (?, ?, ?, ?)";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, barcode);
            ps.setString(2, name);
            ps.setString(3, section);
            ps.setInt(4, activeCheck.isSelected() ? 1 : 0);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Student added successfully!");
            clearForm();
            loadStudents(searchField.getText().trim());

        } catch (SQLIntegrityConstraintViolationException e) {
            JOptionPane.showMessageDialog(this,
                    "This barcode already exists. Please use a unique barcode.",
                    "Duplicate Barcode", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            showError("Unable to add student.", e);
        }
    }

    private void updateStudent() {
        String barcode = barcodeField.getText().trim();
        String name = nameField.getText().trim();
        String section = sectionField.getText().trim();

        if (barcode.isEmpty() || name.isEmpty() || section.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Select a student and complete all fields.",
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a student from the table first.");
            return;
        }

        int modelRow = studentTable.convertRowIndexToModel(selectedRow);
        String originalBarcode = tableModel.getValueAt(modelRow, 0).toString();

        String sql = "UPDATE students SET barcode=?, name=?, section=?, active=? WHERE barcode=?";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, barcode);
            ps.setString(2, name);
            ps.setString(3, section);
            ps.setInt(4, activeCheck.isSelected() ? 1 : 0);
            ps.setString(5, originalBarcode);

            int updated = ps.executeUpdate();

            if (updated > 0) {
                JOptionPane.showMessageDialog(this, "Student updated successfully!");
                clearForm();
                loadStudents(searchField.getText().trim());
            } else {
                JOptionPane.showMessageDialog(this, "Student record was not found.");
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            JOptionPane.showMessageDialog(this,
                    "That barcode is already assigned to another student.",
                    "Duplicate Barcode", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            showError("Unable to update student.", e);
        }
    }

    private void deactivateStudent() {
        int selectedRow = studentTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select a student from the table first.");
            return;
        }

        int modelRow = studentTable.convertRowIndexToModel(selectedRow);
        String barcode = tableModel.getValueAt(modelRow, 0).toString();
        String status = tableModel.getValueAt(modelRow, 3).toString();

        if ("Inactive".equals(status)) {
            JOptionPane.showMessageDialog(this, "This student is already inactive.");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Deactivate this student?\nTheir attendance history will be preserved.",
                "Confirm Deactivation", JOptionPane.YES_NO_OPTION);

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        String sql = "UPDATE students SET active=0 WHERE barcode=?";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, barcode);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Student deactivated successfully.");
            clearForm();
            loadStudents(searchField.getText().trim());

        } catch (SQLException e) {
            showError("Unable to deactivate student.", e);
        }
    }

    private void showSelectedStudent() {
        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow == -1) return;

        int modelRow = studentTable.convertRowIndexToModel(selectedRow);

        barcodeField.setText(tableModel.getValueAt(modelRow, 0).toString());
        nameField.setText(tableModel.getValueAt(modelRow, 1).toString());
        sectionField.setText(tableModel.getValueAt(modelRow, 2).toString());
        activeCheck.setSelected(
                "Active".equals(tableModel.getValueAt(modelRow, 3).toString()));
    }

    private void clearForm() {
        barcodeField.setText("");
        nameField.setText("");
        sectionField.setText("");
        activeCheck.setSelected(true);
        studentTable.clearSelection();
        barcodeField.requestFocusInWindow();
    }

    private void showError(String message, Exception e) {
        JOptionPane.showMessageDialog(this,
                message + "\nDetails: " + e.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
    }
}