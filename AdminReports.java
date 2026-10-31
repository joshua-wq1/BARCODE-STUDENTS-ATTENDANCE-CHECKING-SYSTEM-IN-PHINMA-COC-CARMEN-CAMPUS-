package javabasics;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class AdminReports extends JFrame {
    private static final String URL = "jdbc:mysql://localhost:3306/attendance_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    private static final LocalDate SEMESTER_START = LocalDate.of(2026, 7, 7);
    private static final LocalDate SEMESTER_END = LocalDate.of(2026, 10, 30);

    private final String adminUsername;
    private JTextField fromField;
    private JTextField toField;
    private JTextField searchField;
    private JLabel expectedLabel;
    private JLabel presentLabel;
    private JLabel lateLabel;
    private JLabel absentLabel;
    private JLabel rateLabel;
    private JTable table;
    private DefaultTableModel model;

    public AdminReports(String adminUsername) {
        this.adminUsername = adminUsername;
        setTitle("Admin - Reports and Analytics");
        setSize(1150, 680);
        setMinimumSize(new Dimension(900, 550));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        buildUI();
        loadReport();
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

        JLabel title = new JLabel("Reports & Analytics");
        title.setFont(new Font("Arial", Font.BOLD, 24));
        header.add(title, BorderLayout.WEST);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        header.add(closeButton, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        center.setBorder(BorderFactory.createEmptyBorder(0, 15, 12, 15));

        JPanel stats = new JPanel(new GridLayout(1, 5, 8, 8));
        expectedLabel = createCard(stats, "Expected Sessions");
        presentLabel = createCard(stats, "Present");
        lateLabel = createCard(stats, "Late");
        absentLabel = createCard(stats, "Absences");
        rateLabel = createCard(stats, "Attendance Rate");
        center.add(stats, BorderLayout.NORTH);

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        filters.setBorder(BorderFactory.createTitledBorder(
                "Semester Report Filters"));

        fromField = new JTextField(SEMESTER_START.toString(), 10);
        toField = new JTextField(SEMESTER_END.toString(), 10);
        searchField = new JTextField(14);
        searchField.setToolTipText("Search by barcode, student name, or section");

        JButton applyButton = new JButton("Generate Report");
        JButton resetButton = new JButton("Reset");

        applyButton.addActionListener(e -> loadReport());
        searchField.addActionListener(e -> loadReport());

        resetButton.addActionListener(e -> {
            fromField.setText(SEMESTER_START.toString());
            toField.setText(SEMESTER_END.toString());
            searchField.setText("");
            loadReport();
        });

        filters.add(new JLabel("From:"));
        filters.add(fromField);
        filters.add(new JLabel("To:"));
        filters.add(toField);
        filters.add(new JLabel("Search:"));
        filters.add(searchField);
        filters.add(applyButton);
        filters.add(resetButton);

        JPanel tablePanel = new JPanel(new BorderLayout(5, 5));
        tablePanel.add(filters, BorderLayout.NORTH);

        model = new DefaultTableModel(new String[]{
            "Barcode", "Student Name", "Section", "Expected Sessions",
            "Present", "Late", "Absences", "Attendance Rate"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setRowHeight(25);
        tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);

        JLabel note = new JLabel(
                "Only completed scheduled classes are counted. "
                + "Present and Late both count as attended.");
        note.setBorder(BorderFactory.createEmptyBorder(5, 2, 2, 2));
        tablePanel.add(note, BorderLayout.SOUTH);

        center.add(tablePanel, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    private JLabel createCard(JPanel parent, String heading) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        JLabel name = new JLabel(heading);
        JLabel value = new JLabel("0");
        value.setFont(new Font("Arial", Font.BOLD, 20));

        card.add(name, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        parent.add(card);
        return value;
    }

    private void loadReport() {
        LocalDate from;
        LocalDate to;

        try {
            from = LocalDate.parse(fromField.getText().trim());
            to = LocalDate.parse(toField.getText().trim());
        } catch (DateTimeParseException ex) {
            showError("Enter valid dates using YYYY-MM-DD.");
            return;
        }

        if (from.isBefore(SEMESTER_START)
                || to.isAfter(SEMESTER_END)
                || from.isAfter(to)) {
            showError("Dates must be within " + SEMESTER_START
                    + " and " + SEMESTER_END
                    + ", and the From date cannot be after the To date.");
            return;
        }

        String search = searchField.getText().trim();

        String sql =
            "WITH RECURSIVE report_dates(report_date) AS ("
            + " SELECT CAST(? AS DATE)"
            + " UNION ALL"
            + " SELECT DATE_ADD(report_date, INTERVAL 1 DAY)"
            + " FROM report_dates WHERE report_date < CAST(? AS DATE)"
            + "), completed_sessions AS ("
            + " SELECT rd.report_date, sc.id AS schedule_id, sc.section"
            + " FROM report_dates rd"
            + " JOIN schedules sc ON sc.active = 1"
            + " AND sc.day_of_week = DAYNAME(rd.report_date)"
            + " WHERE rd.report_date < CURDATE()"
            + " OR (rd.report_date = CURDATE() AND sc.end_time <= CURTIME())"
            + "), student_sessions AS ("
            + " SELECT st.barcode, st.name, st.section,"
            + " cs.report_date, cs.schedule_id, a.status"
            + " FROM students st"
            + " JOIN completed_sessions cs ON cs.section = st.section"
            + " LEFT JOIN attendance a"
            + " ON a.student_barcode = st.barcode"
            + " AND a.schedule_id = cs.schedule_id"
            + " AND a.attendance_date = cs.report_date"
            + " AND a.status IN ('Present', 'Late')"
            + " WHERE st.active = 1"
            + ")"
            + " SELECT barcode, name, section,"
            + " COUNT(*) AS expected_sessions,"
            + " SUM(CASE WHEN status = 'Present' THEN 1 ELSE 0 END) AS present_count,"
            + " SUM(CASE WHEN status = 'Late' THEN 1 ELSE 0 END) AS late_count,"
            + " SUM(CASE WHEN status IS NULL THEN 1 ELSE 0 END) AS absent_count"
            + " FROM student_sessions"
            + " WHERE (? = '' OR barcode LIKE ? OR name LIKE ? OR section LIKE ?)"
            + " GROUP BY barcode, name, section"
            + " ORDER BY section, name";

        int expected = 0;
        int present = 0;
        int late = 0;
        int absent = 0;

        try (Connection con = getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            ps.setString(3, search);
            ps.setString(4, "%" + search + "%");
            ps.setString(5, "%" + search + "%");
            ps.setString(6, "%" + search + "%");

            model.setRowCount(0);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int studentExpected = rs.getInt("expected_sessions");
                    int studentPresent = rs.getInt("present_count");
                    int studentLate = rs.getInt("late_count");
                    int studentAbsent = rs.getInt("absent_count");

                    double studentRate = studentExpected == 0 ? 0.0
                            : (studentPresent + studentLate) * 100.0
                            / studentExpected;

                    model.addRow(new Object[]{
                        rs.getString("barcode"),
                        rs.getString("name"),
                        rs.getString("section"),
                        studentExpected,
                        studentPresent,
                        studentLate,
                        studentAbsent,
                        String.format("%.2f%%", studentRate)
                    });

                    expected += studentExpected;
                    present += studentPresent;
                    late += studentLate;
                    absent += studentAbsent;
                }
            }

            double overallRate = expected == 0 ? 0.0
                    : (present + late) * 100.0 / expected;

            expectedLabel.setText(String.valueOf(expected));
            presentLabel.setText(String.valueOf(present));
            lateLabel.setText(String.valueOf(late));
            absentLabel.setText(String.valueOf(absent));
            rateLabel.setText(String.format("%.2f%%", overallRate));

        } catch (SQLException ex) {
            showError("Unable to generate report:\n" + ex.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message,
                "Reports & Analytics", JOptionPane.WARNING_MESSAGE);
    }
}