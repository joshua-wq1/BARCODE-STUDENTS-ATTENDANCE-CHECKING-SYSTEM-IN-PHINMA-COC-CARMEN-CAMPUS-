package javabasics;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class AdminSchedules extends JFrame {
    private static final String URL = "jdbc:mysql://localhost:3306/attendance_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm");

    private JTextField sectionField, startField, endField, searchField;
    private JComboBox<ChoiceItem> subjectCombo, instructorCombo;
    private JComboBox<String> dayCombo;
    private JCheckBox activeCheck;
    private JTable scheduleTable;
    private DefaultTableModel tableModel;

    public AdminSchedules() {
        this("admin");
    }

    public AdminSchedules(String adminUsername) {
        setTitle("Schedule Management - PHINMA COC Carmen Campus");
        setSize(1150, 720);
        setMinimumSize(new Dimension(950, 600));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        initializeUI();
        loadSubjects();
        loadInstructors();
        loadSchedules("");
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

        JLabel title = new JLabel("SCHEDULE MANAGEMENT");
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
        form.setBorder(BorderFactory.createTitledBorder("Class Schedule Information"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 7, 5, 7);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        sectionField = new JTextField(18);
        subjectCombo = new JComboBox<>();
        instructorCombo = new JComboBox<>();
        dayCombo = new JComboBox<>(new String[]{
            "Monday", "Tuesday", "Wednesday", "Thursday",
            "Friday", "Saturday", "Sunday"
        });
        startField = new JTextField("08:00", 10);
        endField = new JTextField("09:30", 10);
        activeCheck = new JCheckBox("Active schedule", true);

        addFormRow(form, gbc, 0, "Section:", sectionField);
        addFormRow(form, gbc, 1, "Subject:", subjectCombo);
        addFormRow(form, gbc, 2, "Instructor:", instructorCombo);
        addFormRow(form, gbc, 3, "Day of Week:", dayCombo);
        addFormRow(form, gbc, 4, "Start Time (HH:mm):", startField);
        addFormRow(form, gbc, 5, "End Time (HH:mm):", endField);

        gbc.gridx = 1;
        gbc.gridy = 6;
        form.add(activeCheck, gbc);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("Add Schedule");
        JButton updateButton = new JButton("Update Schedule");
        JButton deactivateButton = new JButton("Deactivate Schedule");
        JButton clearButton = new JButton("Clear Form");

        addButton.addActionListener(e -> addSchedule());
        updateButton.addActionListener(e -> updateSchedule());
        deactivateButton.addActionListener(e -> deactivateSchedule());
        clearButton.addActionListener(e -> clearForm());

        buttons.add(addButton);
        buttons.add(updateButton);
        buttons.add(deactivateButton);
        buttons.add(clearButton);

        JPanel formPanel = new JPanel(new BorderLayout(5, 5));
        formPanel.add(form, BorderLayout.CENTER);
        formPanel.add(buttons, BorderLayout.SOUTH);

        searchField = new JTextField();
        JButton searchButton = new JButton("Search");
        JButton showAllButton = new JButton("Show All");
        searchButton.addActionListener(e -> loadSchedules(searchField.getText().trim()));
        searchField.addActionListener(e -> loadSchedules(searchField.getText().trim()));
        showAllButton.addActionListener(e -> {
            searchField.setText("");
            loadSchedules("");
        });

        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchPanel.add(new JLabel("Search section, subject, instructor, or day:"),
                BorderLayout.WEST);
        JPanel searchControls = new JPanel(new BorderLayout(5, 0));
        searchControls.add(searchField, BorderLayout.CENTER);
        JPanel searchButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        searchButtons.add(searchButton);
        searchButtons.add(showAllButton);
        searchControls.add(searchButtons, BorderLayout.EAST);
        searchPanel.add(searchControls, BorderLayout.CENTER);

        tableModel = new DefaultTableModel(new String[]{
            "ID", "Section", "Subject", "Instructor", "Day",
            "Start Time", "End Time", "Status"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        scheduleTable = new JTable(tableModel);
        scheduleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scheduleTable.setRowHeight(25);
        scheduleTable.setAutoCreateRowSorter(true);
        scheduleTable.removeColumn(scheduleTable.getColumnModel().getColumn(0));

        scheduleTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && scheduleTable.getSelectedRow() != -1) {
                showSelectedSchedule();
            }
        });

        JPanel tablePanel = new JPanel(new BorderLayout(5, 8));
        tablePanel.setBorder(BorderFactory.createTitledBorder("Class Schedules"));
        tablePanel.add(searchPanel, BorderLayout.NORTH);
        tablePanel.add(new JScrollPane(scheduleTable), BorderLayout.CENTER);

        content.add(formPanel, BorderLayout.NORTH);
        content.add(tablePanel, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row,
                            String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);
    }

    private void loadSubjects() {
        subjectCombo.removeAllItems();
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT id, subject_code, subject_name FROM subjects " +
                     "WHERE active=1 ORDER BY subject_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                subjectCombo.addItem(new ChoiceItem(
                        rs.getInt("id"),
                        rs.getString("subject_code") + " - " +
                        rs.getString("subject_name")));
            }
        } catch (SQLException e) {
            showError("Unable to load active subjects.", e);
        }
    }

    private void loadInstructors() {
        instructorCombo.removeAllItems();
        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT id, instructor_code, name FROM instructors " +
                     "WHERE active=1 ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                instructorCombo.addItem(new ChoiceItem(
                        rs.getInt("id"),
                        rs.getString("instructor_code") + " - " +
                        rs.getString("name")));
            }
        } catch (SQLException e) {
            showError("Unable to load active instructors.", e);
        }
    }

    private void loadSchedules(String keyword) {
        tableModel.setRowCount(0);

        String sql = "SELECT s.id, s.section, s.subject_id, s.instructor_id, " +
                "s.day_of_week, s.start_time, s.end_time, s.active, " +
                "sub.subject_name, i.name AS instructor_name " +
                "FROM schedules s " +
                "JOIN subjects sub ON s.subject_id=sub.id " +
                "JOIN instructors i ON s.instructor_id=i.id " +
                "WHERE s.section LIKE ? OR sub.subject_name LIKE ? " +
                "OR i.name LIKE ? OR s.day_of_week LIKE ? " +
                "ORDER BY FIELD(s.day_of_week,'Monday','Tuesday','Wednesday'," +
                "'Thursday','Friday','Saturday','Sunday'), s.start_time";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            String search = "%" + keyword + "%";
            for (int i = 1; i <= 4; i++) {
                ps.setString(i, search);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("section"),
                        rs.getString("subject_name"),
                        rs.getString("instructor_name"),
                        rs.getString("day_of_week"),
                        rs.getTime("start_time").toString(),
                        rs.getTime("end_time").toString(),
                        rs.getInt("active") == 1 ? "Active" : "Inactive"
                    });
                }
            }
        } catch (SQLException e) {
            showError("Unable to load schedules.", e);
        }
    }

    private boolean validateForm() {
        if (sectionField.getText().trim().isEmpty()
                || subjectCombo.getSelectedItem() == null
                || instructorCombo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this,
                    "Enter a section and select an active subject and instructor.",
                    "Missing Information", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        try {
            LocalTime start = LocalTime.parse(startField.getText().trim(), TIME_FORMAT);
            LocalTime end = LocalTime.parse(endField.getText().trim(), TIME_FORMAT);

            if (!start.isBefore(end)) {
                JOptionPane.showMessageDialog(this,
                        "End time must be later than start time. Overnight schedules are not supported.",
                        "Invalid Time Range", JOptionPane.WARNING_MESSAGE);
                return false;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Enter valid times using HH:mm format, for example 08:00 and 09:30.",
                    "Invalid Time", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private boolean hasOverlap(Connection con, int scheduleId, String section,
                               String day, LocalTime start, LocalTime end)
            throws SQLException {
        String sql = "SELECT COUNT(*) FROM schedules " +
                "WHERE active=1 AND section=? AND day_of_week=? AND id<>? " +
                "AND start_time < ? AND end_time > ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, section);
            ps.setString(2, day);
            ps.setInt(3, scheduleId);
            ps.setTime(4, Time.valueOf(end));
            ps.setTime(5, Time.valueOf(start));

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    private void addSchedule() {
        if (!validateForm()) return;

        ChoiceItem subject = (ChoiceItem) subjectCombo.getSelectedItem();
        ChoiceItem instructor = (ChoiceItem) instructorCombo.getSelectedItem();
        String section = sectionField.getText().trim();
        String day = dayCombo.getSelectedItem().toString();
        LocalTime start = LocalTime.parse(startField.getText().trim(), TIME_FORMAT);
        LocalTime end = LocalTime.parse(endField.getText().trim(), TIME_FORMAT);

        String sql = "INSERT INTO schedules " +
                "(section, subject_id, instructor_id, day_of_week, start_time, end_time, active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection con = getConnection()) {
            if (activeCheck.isSelected() &&
                    hasOverlap(con, 0, section, day, start, end)) {
                JOptionPane.showMessageDialog(this,
                        "This section already has an overlapping active class on that day.",
                        "Schedule Conflict", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, section);
                ps.setInt(2, subject.id);
                ps.setInt(3, instructor.id);
                ps.setString(4, day);
                ps.setTime(5, Time.valueOf(start));
                ps.setTime(6, Time.valueOf(end));
                ps.setInt(7, activeCheck.isSelected() ? 1 : 0);
                ps.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "Schedule added successfully!");
            clearForm();
            loadSchedules(searchField.getText().trim());

        } catch (SQLException e) {
            showError("Unable to add schedule.", e);
        }
    }

    private void updateSchedule() {
        int selectedRow = scheduleTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Select a schedule from the table first.");
            return;
        }
        if (!validateForm()) return;

        int modelRow = scheduleTable.convertRowIndexToModel(selectedRow);
        int scheduleId = Integer.parseInt(
                tableModel.getValueAt(modelRow, 0).toString());

        ChoiceItem subject = (ChoiceItem) subjectCombo.getSelectedItem();
        ChoiceItem instructor = (ChoiceItem) instructorCombo.getSelectedItem();
        String section = sectionField.getText().trim();
        String day = dayCombo.getSelectedItem().toString();
        LocalTime start = LocalTime.parse(startField.getText().trim(), TIME_FORMAT);
        LocalTime end = LocalTime.parse(endField.getText().trim(), TIME_FORMAT);

        String sql = "UPDATE schedules SET section=?, subject_id=?, instructor_id=?, " +
                "day_of_week=?, start_time=?, end_time=?, active=? WHERE id=?";

        try (Connection con = getConnection()) {
            if (activeCheck.isSelected() &&
                    hasOverlap(con, scheduleId, section, day, start, end)) {
                JOptionPane.showMessageDialog(this,
                        "This section already has an overlapping active class on that day.",
                        "Schedule Conflict", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, section);
                ps.setInt(2, subject.id);
                ps.setInt(3, instructor.id);
                ps.setString(4, day);
                ps.setTime(5, Time.valueOf(start));
                ps.setTime(6, Time.valueOf(end));
                ps.setInt(7, activeCheck.isSelected() ? 1 : 0);
                ps.setInt(8, scheduleId);

                int updated = ps.executeUpdate();
                if (updated > 0) {
                    JOptionPane.showMessageDialog(this, "Schedule updated successfully!");
                    clearForm();
                    loadSchedules(searchField.getText().trim());
                } else {
                    JOptionPane.showMessageDialog(this, "Schedule record was not found.");
                }
            }
        } catch (SQLException e) {
            showError("Unable to update schedule.", e);
        }
    }

    private void deactivateSchedule() {
        int selectedRow = scheduleTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Select a schedule from the table first.");
            return;
        }

        int modelRow = scheduleTable.convertRowIndexToModel(selectedRow);
        int scheduleId = Integer.parseInt(
                tableModel.getValueAt(modelRow, 0).toString());

        if ("Inactive".equals(tableModel.getValueAt(modelRow, 7).toString())) {
            JOptionPane.showMessageDialog(this, "This schedule is already inactive.");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Deactivate this schedule?\nExisting attendance history will be preserved.",
                "Confirm Deactivation", JOptionPane.YES_NO_OPTION);

        if (choice != JOptionPane.YES_OPTION) return;

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE schedules SET active=0 WHERE id=?")) {
            ps.setInt(1, scheduleId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Schedule deactivated successfully.");
            clearForm();
            loadSchedules(searchField.getText().trim());
        } catch (SQLException e) {
            showError("Unable to deactivate schedule.", e);
        }
    }

    private void showSelectedSchedule() {
        int selectedRow = scheduleTable.getSelectedRow();
        if (selectedRow == -1) return;

        int row = scheduleTable.convertRowIndexToModel(selectedRow);
        sectionField.setText(tableModel.getValueAt(row, 1).toString());
        String subjectName = tableModel.getValueAt(row, 2).toString();
        String instructorName = tableModel.getValueAt(row, 3).toString();

        selectChoiceByText(subjectCombo, subjectName);
        selectChoiceByText(instructorCombo, instructorName);
        dayCombo.setSelectedItem(tableModel.getValueAt(row, 4).toString());
        startField.setText(tableModel.getValueAt(row, 5).toString().substring(0, 5));
        endField.setText(tableModel.getValueAt(row, 6).toString().substring(0, 5));
        activeCheck.setSelected("Active".equals(
                tableModel.getValueAt(row, 7).toString()));
    }

    private void selectChoiceByText(JComboBox<ChoiceItem> combo, String text) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).label.endsWith(" - " + text)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
        combo.setSelectedIndex(-1);
    }

    private void clearForm() {
        sectionField.setText("");
        if (subjectCombo.getItemCount() > 0) subjectCombo.setSelectedIndex(0);
        if (instructorCombo.getItemCount() > 0) instructorCombo.setSelectedIndex(0);
        dayCombo.setSelectedIndex(0);
        startField.setText("08:00");
        endField.setText("09:30");
        activeCheck.setSelected(true);
        scheduleTable.clearSelection();
        sectionField.requestFocusInWindow();
    }

    private void showError(String message, Exception e) {
        JOptionPane.showMessageDialog(this,
                message + "\nDetails: " + e.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
    }

    private static class ChoiceItem {
        private final int id;
        private final String label;

        ChoiceItem(int id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}