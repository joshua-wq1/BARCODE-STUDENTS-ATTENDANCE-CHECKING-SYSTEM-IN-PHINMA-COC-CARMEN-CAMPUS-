package javabasics;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;



public class AdminAttendance extends JFrame {
    private static final String URL = "jdbc:mysql://localhost:3306/attendance_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    private final String adminUsername;
    private JTextField searchField;
    private JTextField dateField;
    private JComboBox<String> statusBox;
    private JComboBox<String> subjectBox;
    private JLabel totalLabel;
    private JLabel presentLabel;
    private JLabel lateLabel;
    private JLabel dateTotalLabel;
    private JTable table;
    private DefaultTableModel model;

    public AdminAttendance(String adminUsername) {
        this.adminUsername = adminUsername;
        setTitle("Admin - Attendance Monitoring");
        setSize(1250, 700);
        setMinimumSize(new Dimension(950, 550));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        buildUI();
        loadSubjects();
        refreshAttendance();
    }

    private Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            throw new SQLException("MySQL Connector/J is missing from Libraries.", ex);
        }
        return DriverManager.getConnection(URL, DB_USER, DB_PASS);
    }

    private void buildUI() {
        setLayout(new BorderLayout(10, 10));

        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(BorderFactory.createEmptyBorder(12, 15, 5, 15));

        JLabel title = new JLabel("Attendance Monitoring");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        header.add(title, BorderLayout.WEST);

        JButton backButton = new JButton("Close");
        backButton.addActionListener(e -> dispose());
        header.add(backButton, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setBorder(BorderFactory.createEmptyBorder(0, 15, 10, 15));

        JPanel stats = new JPanel(new GridLayout(1, 4, 10, 10));
        totalLabel = createStatCard(stats, "Total Records");
        presentLabel = createStatCard(stats, "Present");
        lateLabel = createStatCard(stats, "Late");
        dateTotalLabel = createStatCard(stats, "Records on Date");
        center.add(stats, BorderLayout.NORTH);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        filters.setBorder(BorderFactory.createTitledBorder("Search and Filters"));

        searchField = new JTextField(16);
        searchField.setToolTipText("Barcode, student name, or section");

        dateField = new JTextField(LocalDate.now().toString(), 10);
        dateField.setToolTipText("YYYY-MM-DD; leave blank for all dates");

        statusBox = new JComboBox<>(
                new String[]{"All Statuses", "Present", "Late"});

        subjectBox = new JComboBox<>();
        subjectBox.addItem("All Subjects");

        JButton searchButton = new JButton("Apply Filters");
        JButton refreshButton = new JButton("Refresh");
        JButton clearButton = new JButton("Clear Filters");

        searchButton.addActionListener(e -> refreshAttendance());
        refreshButton.addActionListener(e -> {
            searchField.setText("");
            dateField.setText(LocalDate.now().toString());
            statusBox.setSelectedIndex(0);
            subjectBox.setSelectedIndex(0);
            refreshAttendance();
        });
        clearButton.addActionListener(e -> {
            searchField.setText("");
            dateField.setText("");
            statusBox.setSelectedIndex(0);
            subjectBox.setSelectedIndex(0);
            refreshAttendance();
        });
        searchField.addActionListener(e -> refreshAttendance());
        dateField.addActionListener(e -> refreshAttendance());

        filters.add(new JLabel("Search:"));
        filters.add(searchField);
        filters.add(new JLabel("Date:"));
        filters.add(dateField);
        filters.add(new JLabel("Subject:"));
        filters.add(subjectBox);
        filters.add(new JLabel("Status:"));
        filters.add(statusBox);
        filters.add(searchButton);
        filters.add(refreshButton);
        filters.add(clearButton);

        JPanel tablePanel = new JPanel(new BorderLayout(5, 5));
        tablePanel.add(filters, BorderLayout.NORTH);

        model = new DefaultTableModel(
                new String[]{"ID", "Barcode", "Student Name", "Section",
                    "Date", "Time", "Day", "Subject", "Instructor",
                    "Start Time", "End Time", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };


    table = new JTable(model);
    table.setAutoCreateRowSorter(true);
    table.setRowHeight(24);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.removeColumn(table.getColumnModel().getColumn(0));
    tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);


        center.add(tablePanel, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    private JLabel createStatCard(JPanel parent, String heading) {
        JPanel card = new JPanel(new BorderLayout(4, 6));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        JLabel name = new JLabel(heading);
        JLabel value = new JLabel("0");
        value.setFont(new Font("Arial", Font.BOLD, 23));

        card.add(name, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        parent.add(card);
        return value;
    }

    private void loadSubjects() {
        subjectBox.removeAllItems();
        subjectBox.addItem("All Subjects");

        String sql = "SELECT DISTINCT s.id, s.subject_name "
                + "FROM subjects s "
                + "JOIN schedules sc ON sc.subject_id = s.id "
                + "JOIN attendance a ON a.schedule_id = sc.id "
                + "ORDER BY s.subject_name";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                subjectBox.addItem(rs.getInt("id") + " - "
                        + rs.getString("subject_name"));
            }
        } catch (SQLException ex) {
            showError("Unable to load subjects", ex);
        }
    }

    private void refreshAttendance() {
        String date = dateField.getText().trim();
        if (!date.isEmpty()) {
            try {
                LocalDate.parse(date);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Enter a valid date using YYYY-MM-DD, or leave it blank.",
                        "Invalid Date", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        String search = searchField.getText().trim();
        String status = (String) statusBox.getSelectedItem();
        String selectedSubject = (String) subjectBox.getSelectedItem();

        Integer subjectId = null;
        if (selectedSubject != null && !"All Subjects".equals(selectedSubject)) {
            try {
                subjectId = Integer.parseInt(selectedSubject.split(" - ", 2)[0]);
            } catch (NumberFormatException ex) {
                showError("Invalid subject selection.", ex);
                return;
            }
        }

        String sql = "SELECT a.id, a.student_barcode, st.name, st.section, "
                + "a.attendance_date, a.attendance_time, sc.day_of_week, "
                + "su.subject_name, i.name AS instructor_name, "
                + "sc.start_time, sc.end_time, a.status "
                + "FROM attendance a "
                + "JOIN students st ON st.barcode = a.student_barcode "
                + "JOIN schedules sc ON sc.id = a.schedule_id "
                + "JOIN subjects su ON su.id = sc.subject_id "
                + "JOIN instructors i ON i.id = sc.instructor_id "
                + "WHERE (? = '' OR a.student_barcode LIKE ? "
                + "OR st.name LIKE ? OR st.section LIKE ?) "
                + "AND (? = '' OR a.attendance_date = ?) "
                + "AND (? = 'All Statuses' OR a.status = ?) "
                + "AND (? IS NULL OR su.id = ?) "
                + "ORDER BY a.attendance_date DESC, a.attendance_time DESC, a.id DESC";

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, search);
            ps.setString(2, "%" + search + "%");
            ps.setString(3, "%" + search + "%");
            ps.setString(4, "%" + search + "%");
            ps.setString(5, date);
            ps.setString(6, date);
            ps.setString(7, status);
            ps.setString(8, status);

            if (subjectId == null) {
                ps.setNull(9, Types.INTEGER);
                ps.setNull(10, Types.INTEGER);
            } else {
                ps.setInt(9, subjectId);
                ps.setInt(10, subjectId);
            }

            model.setRowCount(0);
            int total = 0;
            int present = 0;
            int late = 0;
            int onDate = 0;
            String today = LocalDate.now().toString();

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String recordDate = rs.getString("attendance_date");
                    String recordStatus = rs.getString("status");

                    model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("student_barcode"),
                        rs.getString("name"),
                        rs.getString("section"),
                        recordDate,
                        rs.getString("attendance_time"),
                        rs.getString("day_of_week"),
                        rs.getString("subject_name"),
                        rs.getString("instructor_name"),
                        rs.getString("start_time"),
                        rs.getString("end_time"),
                        recordStatus
                    });

                    total++;
                    if ("Present".equalsIgnoreCase(recordStatus)) {
                        present++;
                    } else if ("Late".equalsIgnoreCase(recordStatus)) {
                        late++;
                    }
                    if (today.equals(recordDate)) {
                        onDate++;
                    }
                }
            }

            totalLabel.setText(String.valueOf(total));
            presentLabel.setText(String.valueOf(present));
            lateLabel.setText(String.valueOf(late));
            dateTotalLabel.setText(String.valueOf(onDate));

            if (table.getColumnModel().getColumnCount() > 0) {
         
            }

        } catch (SQLException ex) {
            showError("Unable to load attendance records", ex);
        }
    }

    private void showError(String message, Exception ex) {
        JOptionPane.showMessageDialog(this,
                message + "\n" + ex.getMessage(),
                "Attendance Monitoring Error",
                JOptionPane.ERROR_MESSAGE);
    }
}
