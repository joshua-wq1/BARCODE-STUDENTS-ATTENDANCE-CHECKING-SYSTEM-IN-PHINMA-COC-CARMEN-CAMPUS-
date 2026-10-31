package javabasics;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class AdminInstructors extends JFrame {
    private static final String URL = "jdbc:mysql://localhost:3306/attendance_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    private JTextField codeField, nameField, emailField, searchField;
    private JPasswordField passwordField;
    private JCheckBox activeCheck;
    private JTable instructorTable;
    private DefaultTableModel tableModel;

    public AdminInstructors() {
        this("admin");
    }

    public AdminInstructors(String adminUsername) {
        setTitle("Instructor Management - PHINMA COC Carmen Campus");
        setSize(1050, 680);
        setMinimumSize(new Dimension(850, 560));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        initializeUI();
        loadInstructors("");
    }

    private Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "MySQL Connector/J is missing from NetBeans Libraries.", e);
        }
        return DriverManager.getConnection(URL, DB_USER, DB_PASS);
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(36, 52, 71));
        header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel("INSTRUCTOR MANAGEMENT");
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
        form.setBorder(BorderFactory.createTitledBorder("Instructor Information"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 7, 5, 7);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        codeField = new JTextField(18);
        nameField = new JTextField(18);
        emailField = new JTextField(18);
        passwordField = new JPasswordField(18);
        activeCheck = new JCheckBox("Active instructor", true);

        addFormRow(form, gbc, 0, "Instructor Code:", codeField);
        addFormRow(form, gbc, 1, "Full Name:", nameField);
        addFormRow(form, gbc, 2, "Email:", emailField);
        addFormRow(form, gbc, 3, "Password:", passwordField);

        gbc.gridx = 1;
        gbc.gridy = 4;
        form.add(activeCheck, gbc);

        JLabel passwordNote = new JLabel(
                "Password is required when adding; leave blank when keeping the current password.");
        passwordNote.setFont(new Font("Arial", Font.ITALIC, 11));
        passwordNote.setForeground(Color.GRAY);
        gbc.gridy = 5;
        form.add(passwordNote, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("Add Instructor");
        JButton updateButton = new JButton("Update Instructor");
        JButton deactivateButton = new JButton("Deactivate Instructor");
        JButton clearButton = new JButton("Clear Form");

        addButton.addActionListener(e -> addInstructor());
        updateButton.addActionListener(e -> updateInstructor());
        deactivateButton.addActionListener(e -> deactivateInstructor());
        clearButton.addActionListener(e -> clearForm());

        buttons.add(addButton);
        buttons.add(updateButton);
        buttons.add(deactivateButton);
        buttons.add(clearButton);

        JPanel formPanel = new JPanel(new BorderLayout(5, 5));
        formPanel.add(form, BorderLayout.CENTER);
        formPanel.add(buttons, BorderLayout.SOUTH);

        JPanel searchPanel = new JPanel(new BorderLayout(8, 5));
        searchField = new JTextField();
        JButton searchButton = new JButton("Search");
        JButton showAllButton = new JButton("Show All");

        searchButton.addActionListener(e ->
                loadInstructors(searchField.getText().trim()));

        searchField.addActionListener(e ->
                loadInstructors(searchField.getText().trim()));

        showAllButton.addActionListener(e -> {
            searchField.setText("");
            loadInstructors("");
        });

        searchPanel.add(
                new JLabel("Search by instructor code, name, or email:"),
                BorderLayout.WEST);

        JPanel searchControls = new JPanel(new BorderLayout(5, 0));
        searchControls.add(searchField, BorderLayout.CENTER);

        JPanel searchButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        searchButtons.add(searchButton);
        searchButtons.add(showAllButton);
        searchControls.add(searchButtons, BorderLayout.EAST);
        searchPanel.add(searchControls, BorderLayout.CENTER);

        tableModel = new DefaultTableModel(
                new String[]{"ID", "Instructor Code", "Full Name", "Email", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        instructorTable = new JTable(tableModel);
        instructorTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        instructorTable.setRowHeight(25);
        instructorTable.setAutoCreateRowSorter(true);

        instructorTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && instructorTable.getSelectedRow() != -1) {
                showSelectedInstructor();
            }
        });

        // The internal database ID is used for updates but hidden from the table.
        instructorTable.removeColumn(instructorTable.getColumnModel().getColumn(0));

        JPanel tablePanel = new JPanel(new BorderLayout(5, 8));
        tablePanel.setBorder(BorderFactory.createTitledBorder("Instructor Records"));
        tablePanel.add(searchPanel, BorderLayout.NORTH);
        tablePanel.add(new JScrollPane(instructorTable), BorderLayout.CENTER);

        content.add(formPanel, BorderLayout.NORTH);
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

    private void loadInstructors(String keyword) {
        tableModel.setRowCount(0);

        String sql = "SELECT id, instructor_code, name, email, active " +
                "FROM instructors " +
                "WHERE instructor_code LIKE ? OR name LIKE ? OR email LIKE ? " +
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
                        rs.getInt("id"),
                        rs.getString("instructor_code"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getInt("active") == 1 ? "Active" : "Inactive"
                    });
                }
            }
        } catch (SQLException e) {
            showError("Unable to load instructor records.", e);
        }
    }

    private void addInstructor() {
        String code = codeField.getText().trim();
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (code.isEmpty() || name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Enter the instructor code, full name, email, and password.",
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            JOptionPane.showMessageDialog(this,
                    "Enter a valid email address.",
                    "Invalid Email", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (password.length() < 8) {
            JOptionPane.showMessageDialog(this,
                    "The password must contain at least 8 characters.",
                    "Weak Password", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "INSERT INTO instructors " +
                "(instructor_code, name, email, active, password_hash) " +
                "VALUES (?, ?, ?, ?, SHA2(?, 256))";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, code);
            ps.setString(2, name);
            ps.setString(3, email);
            ps.setInt(4, activeCheck.isSelected() ? 1 : 0);
            ps.setString(5, password);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this,
                    "Instructor account added successfully.");
            clearForm();
            loadInstructors(searchField.getText().trim());

        } catch (SQLIntegrityConstraintViolationException e) {
            JOptionPane.showMessageDialog(this,
                    "That instructor code or email may already exist. Check your database constraints.",
                    "Duplicate Record", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            showError("Unable to add instructor.", e);
        }
    }

    private void updateInstructor() {
        int selectedRow = instructorTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Select an instructor from the table first.");
            return;
        }

        String code = codeField.getText().trim();
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (code.isEmpty() || name.isEmpty() || email.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Instructor code, name, and email are required.",
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            JOptionPane.showMessageDialog(this,
                    "Enter a valid email address.",
                    "Invalid Email", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!password.isEmpty() && password.length() < 8) {
            JOptionPane.showMessageDialog(this,
                    "A new password must contain at least 8 characters.",
                    "Weak Password", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = instructorTable.convertRowIndexToModel(selectedRow);
        int instructorId = Integer.parseInt(
                tableModel.getValueAt(modelRow, 0).toString());

        String sql;
        if (password.isEmpty()) {
            sql = "UPDATE instructors SET instructor_code=?, name=?, email=?, active=? " +
                    "WHERE id=?";
        } else {
            sql = "UPDATE instructors SET instructor_code=?, name=?, email=?, active=?, " +
                    "password_hash=SHA2(?, 256) WHERE id=?";
        }

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, code);
            ps.setString(2, name);
            ps.setString(3, email);
            ps.setInt(4, activeCheck.isSelected() ? 1 : 0);

            if (password.isEmpty()) {
                ps.setInt(5, instructorId);
            } else {
                ps.setString(5, password);
                ps.setInt(6, instructorId);
            }

            int updated = ps.executeUpdate();

            if (updated > 0) {
                JOptionPane.showMessageDialog(this,
                        "Instructor information updated successfully.");
                clearForm();
                loadInstructors(searchField.getText().trim());
            } else {
                JOptionPane.showMessageDialog(this,
                        "Instructor record was not found.");
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            JOptionPane.showMessageDialog(this,
                    "That instructor code or email may already be assigned.",
                    "Duplicate Record", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            showError("Unable to update instructor.", e);
        }
    }

    private void deactivateInstructor() {
        int selectedRow = instructorTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Select an instructor from the table first.");
            return;
        }

        int modelRow = instructorTable.convertRowIndexToModel(selectedRow);
        int instructorId = Integer.parseInt(
                tableModel.getValueAt(modelRow, 0).toString());
        String status = tableModel.getValueAt(modelRow, 4).toString();

        if ("Inactive".equals(status)) {
            JOptionPane.showMessageDialog(this,
                    "This instructor is already inactive.");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Deactivate this instructor account?\n" +
                "Existing schedules and attendance history will be preserved.",
                "Confirm Deactivation", JOptionPane.YES_NO_OPTION);

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        String sql = "UPDATE instructors SET active=0 WHERE id=?";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, instructorId);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this,
                    "Instructor account deactivated.");
            clearForm();
            loadInstructors(searchField.getText().trim());

        } catch (SQLException e) {
            showError("Unable to deactivate instructor.", e);
        }
    }

    private void showSelectedInstructor() {
        int selectedRow = instructorTable.getSelectedRow();
        if (selectedRow == -1) return;

        int modelRow = instructorTable.convertRowIndexToModel(selectedRow);

        codeField.setText(tableModel.getValueAt(modelRow, 1).toString());
        nameField.setText(tableModel.getValueAt(modelRow, 2).toString());
        emailField.setText(tableModel.getValueAt(modelRow, 3).toString());
        activeCheck.setSelected(
                "Active".equals(tableModel.getValueAt(modelRow, 4).toString()));

        // Never display or retrieve an existing password.
        passwordField.setText("");
    }

    private void clearForm() {
        codeField.setText("");
        nameField.setText("");
        emailField.setText("");
        passwordField.setText("");
        activeCheck.setSelected(true);
        instructorTable.clearSelection();
        codeField.requestFocusInWindow();
    }

    private void showError(String message, Exception e) {
        JOptionPane.showMessageDialog(this,
                message + "\nDetails: " + e.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
    }
}