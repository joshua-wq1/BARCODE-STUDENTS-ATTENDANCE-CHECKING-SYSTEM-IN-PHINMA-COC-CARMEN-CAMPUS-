package javabasics;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class AdminSubjects extends JFrame {
    private static final String URL = "jdbc:mysql://localhost:3306/attendance_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    private JTextField codeField, nameField, searchField;
    private JTextArea descriptionArea;
    private JCheckBox activeCheck;
    private JTable subjectTable;
    private DefaultTableModel tableModel;

    public AdminSubjects() {
        this("admin");
    }

    public AdminSubjects(String adminUsername) {
        setTitle("Subject Management - PHINMA COC Carmen Campus");
        setSize(1050, 680);
        setMinimumSize(new Dimension(850, 560));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        initializeUI();
        loadSubjects("");
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

        JLabel title = new JLabel("SUBJECT MANAGEMENT");
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
        form.setBorder(BorderFactory.createTitledBorder("Subject Information"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 7, 5, 7);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        codeField = new JTextField(20);
        nameField = new JTextField(20);
        descriptionArea = new JTextArea(3, 20);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        activeCheck = new JCheckBox("Active subject", true);

        addFormRow(form, gbc, 0, "Subject Code:", codeField);
        addFormRow(form, gbc, 1, "Subject Name:", nameField);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        form.add(new JLabel("Description:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        form.add(new JScrollPane(descriptionArea), gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;

        gbc.gridx = 1;
        gbc.gridy = 3;
        form.add(activeCheck, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("Add Subject");
        JButton updateButton = new JButton("Update Subject");
        JButton deactivateButton = new JButton("Deactivate Subject");
        JButton clearButton = new JButton("Clear Form");

        addButton.addActionListener(e -> addSubject());
        updateButton.addActionListener(e -> updateSubject());
        deactivateButton.addActionListener(e -> deactivateSubject());
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
                loadSubjects(searchField.getText().trim()));
        searchField.addActionListener(e ->
                loadSubjects(searchField.getText().trim()));
        showAllButton.addActionListener(e -> {
            searchField.setText("");
            loadSubjects("");
        });

        searchPanel.add(new JLabel("Search by subject code or name:"),
                BorderLayout.WEST);

        JPanel searchControls = new JPanel(new BorderLayout(5, 0));
        searchControls.add(searchField, BorderLayout.CENTER);

        JPanel searchButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        searchButtons.add(searchButton);
        searchButtons.add(showAllButton);
        searchControls.add(searchButtons, BorderLayout.EAST);
        searchPanel.add(searchControls, BorderLayout.CENTER);

        tableModel = new DefaultTableModel(
                new String[]{"ID", "Subject Code", "Subject Name",
                    "Description", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        subjectTable = new JTable(tableModel);
        subjectTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        subjectTable.setRowHeight(25);
        subjectTable.setAutoCreateRowSorter(true);

        // Keep the internal ID for updates, but hide it from the user.
        subjectTable.removeColumn(subjectTable.getColumnModel().getColumn(0));

        subjectTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && subjectTable.getSelectedRow() != -1) {
                showSelectedSubject();
            }
        });

        JPanel tablePanel = new JPanel(new BorderLayout(5, 8));
        tablePanel.setBorder(BorderFactory.createTitledBorder("Subject Records"));
        tablePanel.add(searchPanel, BorderLayout.NORTH);
        tablePanel.add(new JScrollPane(subjectTable), BorderLayout.CENTER);

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

    private void loadSubjects(String keyword) {
        tableModel.setRowCount(0);

        String sql = "SELECT id, subject_code, subject_name, description, active " +
                "FROM subjects " +
                "WHERE subject_code LIKE ? OR subject_name LIKE ? " +
                "ORDER BY subject_name";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String search = "%" + keyword + "%";
            ps.setString(1, search);
            ps.setString(2, search);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("subject_code"),
                        rs.getString("subject_name"),
                        rs.getString("description"),
                        rs.getInt("active") == 1 ? "Active" : "Inactive"
                    });
                }
            }
        } catch (SQLException e) {
            showError("Unable to load subjects.", e);
        }
    }

    private void addSubject() {
        String code = codeField.getText().trim();
        String name = nameField.getText().trim();
        String description = descriptionArea.getText().trim();

        if (code.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Subject code and subject name are required.",
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "INSERT INTO subjects " +
                "(subject_code, subject_name, description, active) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, code);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.setInt(4, activeCheck.isSelected() ? 1 : 0);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this, "Subject added successfully!");
            clearForm();
            loadSubjects(searchField.getText().trim());

        } catch (SQLIntegrityConstraintViolationException e) {
            JOptionPane.showMessageDialog(this,
                    "This subject code already exists or the record conflicts with a database constraint.",
                    "Duplicate or Related Record", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            showError("Unable to add subject.", e);
        }
    }

    private void updateSubject() {
        int selectedRow = subjectTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Select a subject from the table first.");
            return;
        }

        String code = codeField.getText().trim();
        String name = nameField.getText().trim();
        String description = descriptionArea.getText().trim();

        if (code.isEmpty() || name.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Subject code and subject name are required.",
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = subjectTable.convertRowIndexToModel(selectedRow);
        int subjectId = Integer.parseInt(
                tableModel.getValueAt(modelRow, 0).toString());

        String sql = "UPDATE subjects SET subject_code=?, subject_name=?, " +
                "description=?, active=? WHERE id=?";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, code);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.setInt(4, activeCheck.isSelected() ? 1 : 0);
            ps.setInt(5, subjectId);

            int updated = ps.executeUpdate();

            if (updated > 0) {
                JOptionPane.showMessageDialog(this,
                        "Subject updated successfully!");
                clearForm();
                loadSubjects(searchField.getText().trim());
            } else {
                JOptionPane.showMessageDialog(this,
                        "Subject record was not found.");
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            JOptionPane.showMessageDialog(this,
                    "That subject code is already being used.",
                    "Duplicate Subject Code", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException e) {
            showError("Unable to update subject.", e);
        }
    }

    private void deactivateSubject() {
        int selectedRow = subjectTable.getSelectedRow();

        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Select a subject from the table first.");
            return;
        }

        int modelRow = subjectTable.convertRowIndexToModel(selectedRow);
        int subjectId = Integer.parseInt(
                tableModel.getValueAt(modelRow, 0).toString());
        String status = tableModel.getValueAt(modelRow, 4).toString();

        if ("Inactive".equals(status)) {
            JOptionPane.showMessageDialog(this,
                    "This subject is already inactive.");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Deactivate this subject?\n" +
                "Existing schedules and attendance history will be preserved.",
                "Confirm Deactivation", JOptionPane.YES_NO_OPTION);

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        String sql = "UPDATE subjects SET active=0 WHERE id=?";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, subjectId);
            ps.executeUpdate();

            JOptionPane.showMessageDialog(this,
                    "Subject deactivated successfully.");
            clearForm();
            loadSubjects(searchField.getText().trim());

        } catch (SQLException e) {
            showError("Unable to deactivate subject.", e);
        }
    }

    private void showSelectedSubject() {
        int selectedRow = subjectTable.getSelectedRow();
        if (selectedRow == -1) return;

        int modelRow = subjectTable.convertRowIndexToModel(selectedRow);

        codeField.setText(tableModel.getValueAt(modelRow, 1).toString());
        nameField.setText(tableModel.getValueAt(modelRow, 2).toString());

        Object description = tableModel.getValueAt(modelRow, 3);
        descriptionArea.setText(description == null ? "" : description.toString());

        activeCheck.setSelected(
                "Active".equals(tableModel.getValueAt(modelRow, 4).toString()));
    }

    private void clearForm() {
        codeField.setText("");
        nameField.setText("");
        descriptionArea.setText("");
        activeCheck.setSelected(true);
        subjectTable.clearSelection();
        codeField.requestFocusInWindow();
    }

    private void showError(String message, Exception e) {
        JOptionPane.showMessageDialog(this,
                message + "\nDetails: " + e.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
    }
}