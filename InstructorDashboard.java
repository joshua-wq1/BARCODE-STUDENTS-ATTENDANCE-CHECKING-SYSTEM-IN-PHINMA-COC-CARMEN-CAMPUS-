package javabasics;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class InstructorDashboard extends JFrame {
    private final String instructorCode;
    private String instructorName = "";
    private Connection db;

    private JLabel nameLabel, presentLabel, lateLabel, absentLabel;
    private JTable classesTable, recordsTable;
    private JTextField dateField;

    private final DateTimeFormatter timeFormat =
            DateTimeFormatter.ofPattern("hh:mm a");

    public InstructorDashboard(String instructorCode) {
        this.instructorCode = instructorCode;
        setTitle("Instructor Dashboard - PHINMA COC Carmen Campus");
        setSize(1100, 700);
        setMinimumSize(new Dimension(850, 550));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        if (!connectDatabase()) {
            dispose();
            return;
        }

        if (!loadInstructor()) {
            dispose();
            return;
        }

        buildUI();
        loadClasses();
        loadAttendance();
        loadSummary();
    }

    private boolean connectDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            if (Login.db != null && !Login.db.isClosed()) {
                db = Login.db;
            } else {
                db = DriverManager.getConnection(
                        Login.URL, Login.DB_USER, Login.DB_PASS);
                Login.db = db;
            }
            return true;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                    "Database connection failed:\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private boolean loadInstructor() {
        String sql = "SELECT name FROM instructors " +
                     "WHERE instructor_code=? AND active=1";

        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setString(1, instructorCode);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    instructorName = rs.getString("name");
                    return true;
                }
            }

            JOptionPane.showMessageDialog(this,
                    "Instructor account was not found or is inactive.");
            return false;
        } catch (SQLException ex) {
            showError(ex);
            return false;
        }
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(12, 12));
        main.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel header = new JPanel(new BorderLayout());
        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("INSTRUCTOR DASHBOARD");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));

        nameLabel = new JLabel(
                "Instructor: " + instructorName + " (" + instructorCode + ")");
        nameLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        heading.add(title);
        heading.add(Box.createVerticalStrut(6));
        heading.add(nameLabel);

        JButton refreshButton = new JButton("Refresh");
        JButton logoutButton = new JButton("Logout");

        JPanel headerButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        headerButtons.add(refreshButton);
        headerButtons.add(logoutButton);

        header.add(heading, BorderLayout.WEST);
        header.add(headerButtons, BorderLayout.EAST);

        JPanel summary = new JPanel(new GridLayout(1, 3, 12, 0));
        summary.setBorder(BorderFactory.createTitledBorder(
                "Attendance Summary for Selected Date"));

        presentLabel = new JLabel("Present: 0", SwingConstants.CENTER);
        lateLabel = new JLabel("Late: 0", SwingConstants.CENTER);
        absentLabel = new JLabel("Absent: 0", SwingConstants.CENTER);

        styleSummary(presentLabel);
        styleSummary(lateLabel);
        styleSummary(absentLabel);

        summary.add(presentLabel);
        summary.add(lateLabel);
        summary.add(absentLabel);

        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        datePanel.add(new JLabel("Attendance Date (YYYY-MM-DD):"));

        dateField = new JTextField(LocalDate.now().toString(), 10);
        JButton viewButton = new JButton("View Attendance");

        datePanel.add(dateField);
        datePanel.add(viewButton);

        classesTable = new JTable();
        recordsTable = new JTable();
        styleTable(classesTable);
        styleTable(recordsTable);

        JPanel classesPanel = new JPanel(new BorderLayout(5, 5));
        classesPanel.add(new JLabel("My Assigned Classes"), BorderLayout.NORTH);
        classesPanel.add(new JScrollPane(classesTable), BorderLayout.CENTER);

        JPanel recordsPanel = new JPanel(new BorderLayout(5, 5));
        recordsPanel.add(datePanel, BorderLayout.NORTH);
        recordsPanel.add(new JScrollPane(recordsTable), BorderLayout.CENTER);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("My Classes", classesPanel);
        tabs.addTab("Attendance Records", recordsPanel);

        JPanel center = new JPanel(new BorderLayout(10, 10));
        center.add(summary, BorderLayout.NORTH);
        center.add(tabs, BorderLayout.CENTER);

        main.add(header, BorderLayout.NORTH);
        main.add(center, BorderLayout.CENTER);
        setContentPane(main);

        refreshButton.addActionListener(e -> refreshDashboard());
        viewButton.addActionListener(e -> {
            if (validDate()) {
                loadAttendance();
                loadSummary();
            }
        });

        dateField.addActionListener(e -> {
            if (validDate()) {
                loadAttendance();
                loadSummary();
            }
        });

        logoutButton.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                    this, "Are you sure you want to log out?",
                    "Confirm Logout", JOptionPane.YES_NO_OPTION);

            if (choice == JOptionPane.YES_OPTION) {
                dispose();
                new Main().setVisible(true);
            }
        });
    }

    private void styleSummary(JLabel label) {
        label.setFont(new Font("SansSerif", Font.BOLD, 18));
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(18, 5, 18, 5)));
    }

    private void styleTable(JTable table) {
        table.setRowHeight(26);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    private void loadClasses() {
        String sql = "SELECT s.id, sub.subject_code, sub.subject_name, " +
                "s.section, s.day_of_week, s.start_time, s.end_time " +
                "FROM schedules s " +
                "JOIN subjects sub ON sub.id=s.subject_id " +
                "WHERE s.instructor_id=(" +
                "SELECT id FROM instructors WHERE instructor_code=?) " +
                "AND s.active=1 AND sub.active=1 " +
                "ORDER BY FIELD(s.day_of_week,'Monday','Tuesday'," +
                "'Wednesday','Thursday','Friday','Saturday','Sunday'), " +
                "s.start_time";

        String[] columns = {
            "Schedule ID", "Subject Code", "Subject",
            "Section", "Day", "Start Time", "End Time"
        };

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setString(1, instructorCode);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("subject_code"),
                        rs.getString("subject_name"),
                        rs.getString("section"),
                        rs.getString("day_of_week"),
                        rs.getTime("start_time").toLocalTime()
                                .format(timeFormat),
                        rs.getTime("end_time").toLocalTime()
                                .format(timeFormat)
                    });
                }
            }

            classesTable.setModel(model);
            if (classesTable.getColumnCount() > 0) {
                classesTable.removeColumn(
                        classesTable.getColumnModel().getColumn(0));
            }
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private boolean validDate() {
        try {
            LocalDate.parse(dateField.getText().trim());
            return true;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Enter a valid date using YYYY-MM-DD, for example 2026-10-11.");
            return false;
        }
    }

    private LocalDate selectedDate() {
        return LocalDate.parse(dateField.getText().trim());
    }

    private void loadAttendance() {
        String sql = "SELECT a.attendance_date, a.attendance_time, " +
                "st.barcode, st.name AS student_name, st.section, " +
                "sub.subject_name, a.status " +
                "FROM attendance a " +
                "JOIN schedules s ON s.id=a.schedule_id " +
                "JOIN subjects sub ON sub.id=s.subject_id " +
                "JOIN students st ON st.barcode=a.student_barcode " +
                "WHERE s.instructor_id=(" +
                "SELECT id FROM instructors WHERE instructor_code=?) " +
                "AND a.attendance_date=? " +
                "ORDER BY sub.subject_name, st.name";

        String[] columns = {
            "Date", "Time", "Barcode", "Student Name",
            "Section", "Subject", "Status"
        };

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setString(1, instructorCode);
            ps.setDate(2, Date.valueOf(selectedDate()));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getDate("attendance_date"),
                        rs.getTime("attendance_time"),
                        rs.getString("barcode"),
                        rs.getString("student_name"),
                        rs.getString("section"),
                        rs.getString("subject_name"),
                        rs.getString("status")
                    });
                }
            }

            recordsTable.setModel(model);
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void loadSummary() {
        LocalDate date = selectedDate();
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        String presentSql = "SELECT COUNT(*) FROM attendance a " +
                "JOIN schedules s ON s.id=a.schedule_id " +
                "WHERE s.instructor_id=(" +
                "SELECT id FROM instructors WHERE instructor_code=?) " +
                "AND a.attendance_date=? AND a.status='Present'";

        String lateSql = "SELECT COUNT(*) FROM attendance a " +
                "JOIN schedules s ON s.id=a.schedule_id " +
                "WHERE s.instructor_id=(" +
                "SELECT id FROM instructors WHERE instructor_code=?) " +
                "AND a.attendance_date=? AND a.status='Late'";

        String absentSql = "SELECT COUNT(*) FROM schedules s " +
                "JOIN students st ON st.section=s.section AND st.active=1 " +
                "WHERE s.instructor_id=(" +
                "SELECT id FROM instructors WHERE instructor_code=?) " +
                "AND s.active=1 " +
                "AND s.day_of_week=? " +
                "AND NOT EXISTS (SELECT 1 FROM attendance a " +
                "WHERE a.schedule_id=s.id " +
                "AND a.student_barcode=st.barcode " +
                "AND a.attendance_date=? " +
                "AND a.status IN ('Present','Late'))";

        try {
            int present = countAttendance(presentSql, date);
            int late = countAttendance(lateSql, date);
            int absent = 0;

            // Count absences only for classes that have finished.
            if (date.isBefore(today) ||
                    (date.equals(today) && hasFinishedClass(date, now))) {
                try (PreparedStatement ps = db.prepareStatement(absentSql)) {
                    ps.setString(1, instructorCode);
                    ps.setString(2, date.getDayOfWeek().getDisplayName(
                            java.time.format.TextStyle.FULL,
                            java.util.Locale.ENGLISH));
                    ps.setDate(3, Date.valueOf(date));

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) absent = rs.getInt(1);
                    }
                }
            }

            presentLabel.setText("Present: " + present);
            lateLabel.setText("Late: " + late);
            absentLabel.setText("Absent: " + absent);
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private int countAttendance(String sql, LocalDate date)
            throws SQLException {
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setString(1, instructorCode);
            ps.setDate(2, Date.valueOf(date));

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private boolean hasFinishedClass(LocalDate date, LocalTime now)
            throws SQLException {
        String sql = "SELECT COUNT(*) FROM schedules s " +
                "WHERE s.instructor_id=(" +
                "SELECT id FROM instructors WHERE instructor_code=?) " +
                "AND s.active=1 AND s.day_of_week=? " +
                "AND s.end_time <= ?";

        LocalTime cutoff = date.equals(LocalDate.now())
                ? now : LocalTime.MAX;

        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setString(1, instructorCode);
            ps.setString(2, date.getDayOfWeek().getDisplayName(
                    java.time.format.TextStyle.FULL,
                    java.util.Locale.ENGLISH));
            ps.setTime(3, Time.valueOf(cutoff));

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private void refreshDashboard() {
        if (validDate()) {
            loadInstructor();
            loadClasses();
            loadAttendance();
            loadSummary();
        }
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this,
                "Error: " + ex.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
    }
}