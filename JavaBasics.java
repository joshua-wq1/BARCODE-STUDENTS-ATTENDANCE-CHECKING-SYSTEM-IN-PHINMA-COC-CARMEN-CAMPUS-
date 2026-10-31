package javabasics;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class JavaBasics {
    public static class AttendanceApp {
        static final String URL = "jdbc:mysql://localhost:3306/attendance_db";
        static final String DB_USER = "root";
        static final String DB_PASS = "";
        static Connection db;

        static String currentBarcode;
        static String currentStudentName;
        static String currentSection;
        static String role = "Student";
        static JTabbedPane mainTabs;

        static final Color OK = new Color(0xB7E4C7);
        static final Color WARN = new Color(0xFFE8A3);
        static final Color BAD = new Color(0xF5B7B1);
        static final Color IDLE = new Color(0xEEEEEE);

        public static void start() {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                db = DriverManager.getConnection(URL, DB_USER, DB_PASS);
            } catch (ClassNotFoundException e) {
                JOptionPane.showMessageDialog(
                    null,
                    "MySQL Connector JAR is missing from NetBeans Libraries!\n" + e.getMessage(),
                    "Driver Error",
                    JOptionPane.ERROR_MESSAGE
                );
                return;
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(
                    null,
                    "Cannot connect to MySQL:\n" + e.getMessage()
                    + "\n\nMake sure MySQL is started in XAMPP and attendance_db is created.",
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            SwingUtilities.invokeLater(AttendanceApp::login);
        }

        static DefaultTableModel model(String sql, String[] cols, Object... p) {
            DefaultTableModel m = new DefaultTableModel(cols, 0) {
                @Override
                public boolean isCellEditable(int r, int c) {
                    return false;
                }
            };

            try (PreparedStatement ps = db.prepareStatement(sql)) {
                for (int i = 0; i < p.length; i++) {
                    ps.setObject(i + 1, p[i]);
                }

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Object[] row = new Object[cols.length];

                        for (int i = 0; i < cols.length; i++) {
                            row[i] = rs.getObject(i + 1);
                        }

                        m.addRow(row);
                    }
                }
            } catch (SQLException e) {
                err(e);
            }

            return m;
        }

        static int upd(String sql, Object... p) throws SQLException {
            try (PreparedStatement ps = db.prepareStatement(sql)) {
                for (int i = 0; i < p.length; i++) {
                    ps.setObject(i + 1, p[i]);
                }

                return ps.executeUpdate();
            }
        }

        static void err(Exception e) {
            JOptionPane.showMessageDialog(
                null,
                e.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }

        // ==========================================================
        // BARCODE LOGIN
        // ==========================================================

        static void login() {
            JFrame f = new JFrame("Student Barcode Login");
            f.setSize(450, 350);
            f.setLocationRelativeTo(null);
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel main = new JPanel();
            main.setBorder(
                BorderFactory.createEmptyBorder(30, 35, 30, 35)
            );
            main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));

            JLabel title = new JLabel("Student Barcode Login");
            title.setFont(
                new Font("SansSerif", Font.BOLD, 25)
            );
            title.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel school = new JLabel("PHINMA COC CARMEN");
            school.setFont(
                new Font("SansSerif", Font.PLAIN, 14)
            );
            school.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel instruction = new JLabel(
                "Scan or enter your student barcode"
            );
            instruction.setFont(
                new Font("SansSerif", Font.PLAIN, 13)
            );
            instruction.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel barcodeLabel = new JLabel("Barcode");
            barcodeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            JTextField barcode = new JTextField();
            barcode.setFont(
                new Font("SansSerif", Font.BOLD, 20)
            );
            barcode.setHorizontalAlignment(JTextField.CENTER);
            barcode.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 45)
            );

            JButton scan = new JButton("Verify Barcode");
            scan.setFont(
                new Font("SansSerif", Font.BOLD, 14)
            );
            scan.setAlignmentX(Component.CENTER_ALIGNMENT);
            scan.setMaximumSize(
                new Dimension(Integer.MAX_VALUE, 45)
            );

            main.add(title);
            main.add(Box.createVerticalStrut(5));
            main.add(school);
            main.add(Box.createVerticalStrut(5));
            main.add(instruction);
            main.add(Box.createVerticalStrut(25));
            main.add(barcodeLabel);
            main.add(Box.createVerticalStrut(5));
            main.add(barcode);
            main.add(Box.createVerticalStrut(15));
            main.add(scan);

            f.add(main);
            f.setVisible(true);
            barcode.requestFocus();

            scan.addActionListener(
                e -> verifyStudentBarcode(f, barcode)
            );

            barcode.addActionListener(
                e -> verifyStudentBarcode(f, barcode)
            );
        }

        // ==========================================================
        // VERIFY BARCODE
        // ==========================================================

        static void verifyStudentBarcode(
            JFrame f,
            JTextField barcode
        ) {
            String code = barcode.getText().trim();

            if (code.isEmpty()) {
                JOptionPane.showMessageDialog(
                    f,
                    "Please scan or enter a barcode.",
                    "Barcode Required",
                    JOptionPane.WARNING_MESSAGE
                );
                barcode.requestFocus();
                return;
            }

            String sql =
                "SELECT barcode, name, section " +
                "FROM students " +
                "WHERE barcode=? AND active=1";

            try (PreparedStatement ps = db.prepareStatement(sql)) {
                ps.setString(1, code);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        currentBarcode =
                            rs.getString("barcode");

                        currentStudentName =
                            rs.getString("name");

                        currentSection =
                            rs.getString("section");

                        role = "Student";

                        JOptionPane.showMessageDialog(
                            f,
                            "Student Verified!\n\n"
                            + "Name: " + currentStudentName
                            + "\nSection: " + currentSection
                            + "\nBarcode: " + currentBarcode
                            + "\n\nWelcome to the Attendance Checking System.",
                            "Student Identified",
                            JOptionPane.INFORMATION_MESSAGE
                        );

                        f.dispose();
                        mainFrame();

                    } else {
                        JOptionPane.showMessageDialog(
                            f,
                            "Barcode not found or student is inactive.\n\n"
                            + "Please scan a registered student barcode.",
                            "Invalid Barcode",
                            JOptionPane.ERROR_MESSAGE
                        );

                        barcode.setText("");
                        barcode.requestFocus();
                    }
                }

            } catch (SQLException e) {
                JOptionPane.showMessageDialog(
                    f,
                    "Database Error:\n" + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }

        // ==========================================================
        // MAIN FRAME
        // ==========================================================

        static void mainFrame() {
            JFrame f = new JFrame(
                "Barcode Students' Attendance Checking System - PHINMA COC Carmen Campus"
            );

            mainTabs = new JTabbedPane();

            mainTabs.addTab(
                "Scan Attendance",
                scanPanel()
            );

            mainTabs.addTab(
                "Records",
                recordsPanel()
            );

            mainTabs.addTab(
                "Reports",
                reportsPanel()
            );

            f.add(mainTabs);

            f.setSize(1000, 650);
            f.setLocationRelativeTo(null);
            f.setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
            );
            f.setVisible(true);
        }

        // ==========================================================
        // SCAN ATTENDANCE
        // ==========================================================

        static JPanel scanPanel() {
            JPanel p = new JPanel(
                new BorderLayout(10, 10)
            );

            p.setBorder(
                BorderFactory.createEmptyBorder(
                    15, 15, 15, 15
                )
            );

            // ---------- STUDENT INFORMATION ----------

            JPanel studentInfo = new JPanel(
                new GridLayout(3, 2, 10, 8)
            );

            studentInfo.setBorder(
                BorderFactory.createTitledBorder(
                    "Student Information"
                )
            );

            studentInfo.add(new JLabel("Name:"));
            studentInfo.add(
                new JLabel(currentStudentName)
            );

            studentInfo.add(new JLabel("Section:"));
            studentInfo.add(
                new JLabel(currentSection)
            );

            studentInfo.add(new JLabel("Date:"));
            studentInfo.add(
                new JLabel(
                    LocalDate.now().toString()
                )
            );

            p.add(
                studentInfo,
                BorderLayout.NORTH
            );

            // ---------- ALL SUBJECTS ----------

            String sql =
                "SELECT id, subject, start_time, end_time, day_of_week " +
                "FROM schedules " +
                "WHERE section=? " +
                "ORDER BY FIELD(day_of_week," +
                "'Monday','Tuesday','Wednesday','Thursday'," +
                "'Friday','Saturday','Sunday'), start_time";

            DefaultTableModel scheduleModel = model(
                sql,
                new String[]{
                    "ID",
                    "Subject",
                    "Start Time",
                    "End Time",
                    "Day"
                },
                currentSection
            );

            JTable scheduleTable =
                new JTable(scheduleModel);

            // Hide ID column
            scheduleTable.removeColumn(
                scheduleTable
                    .getColumnModel()
                    .getColumn(0)
            );

            scheduleTable.setRowHeight(30);

            scheduleTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION
            );

            scheduleTable.setAutoCreateRowSorter(true);

            JScrollPane scheduleScroll =
                new JScrollPane(scheduleTable);

            scheduleScroll.setBorder(
                BorderFactory.createTitledBorder(
                    "Subjects and Class Schedule"
                )
            );

            p.add(
                scheduleScroll,
                BorderLayout.CENTER
            );

            // ---------- BOTTOM ----------

            JPanel bottom = new JPanel(
                new BorderLayout(10, 10)
            );

            JButton markPresent =
                new JButton("Mark Present");

            markPresent.setFont(
                new Font(
                    "SansSerif",
                    Font.BOLD,
                    14
                )
            );

            markPresent.setBackground(OK);

            bottom.add(
                new JLabel(
                    "Select your subject above, then click Mark Present:"
                ),
                BorderLayout.CENTER
            );

            bottom.add(
                markPresent,
                BorderLayout.EAST
            );

            p.add(
                bottom,
                BorderLayout.SOUTH
            );

            // ---------- MARK PRESENT ----------

            markPresent.addActionListener(e -> {
                int row =
                    scheduleTable.getSelectedRow();

                if (row == -1) {
                    JOptionPane.showMessageDialog(
                        p,
                        "Please select the subject you are attending.",
                        "Subject Required",
                        JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }

                int modelRow =
                    scheduleTable.convertRowIndexToModel(row);

                Object idValue =
                    scheduleModel.getValueAt(
                        modelRow,
                        0
                    );

                if (!(idValue instanceof Number)) {
                    JOptionPane.showMessageDialog(
                        p,
                        "Invalid schedule selected.",
                        "Schedule Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }

                int scheduleId =
                    ((Number) idValue).intValue();

                String subject =
                    String.valueOf(
                        scheduleModel.getValueAt(
                            modelRow,
                            1
                        )
                    );

                String startTime =
                    String.valueOf(
                        scheduleModel.getValueAt(
                            modelRow,
                            2
                        )
                    );

                String endTime =
                    String.valueOf(
                        scheduleModel.getValueAt(
                            modelRow,
                            3
                        )
                    );

                String day =
                    String.valueOf(
                        scheduleModel.getValueAt(
                            modelRow,
                            4
                        )
                    );

                int confirm =
                    JOptionPane.showConfirmDialog(
                        p,
                        "Subject: " + subject
                        + "\nDay: " + day
                        + "\nStart Time: " + startTime
                        + "\nEnd Time: " + endTime
                        + "\n\nMark yourself PRESENT?",
                        "Confirm Attendance",
                        JOptionPane.YES_NO_OPTION
                    );

                if (confirm ==
                    JOptionPane.YES_OPTION) {

                    markPresent(
                        p,
                        scheduleId
                    );
                }
            });

            return p;
        }

        // ==========================================================
        // MARK PRESENT
        // ==========================================================

        static void markPresent(
            Component parent,
            int scheduleId
        ) {
            if (currentBarcode == null ||
                currentBarcode.trim().isEmpty()) {

                JOptionPane.showMessageDialog(
                    parent,
                    "No student is currently logged in.",
                    "Student Error",
                    JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            LocalDate date =
                LocalDate.now();

            LocalTime time =
                LocalTime.now();

            // Check duplicate attendance
            String check =
                "SELECT id FROM attendance " +
                "WHERE student_barcode=? " +
                "AND schedule_id=? " +
                "AND attendance_date=?";

            try (PreparedStatement ps =
                db.prepareStatement(check)) {

                ps.setString(
                    1,
                    currentBarcode
                );

                ps.setInt(
                    2,
                    scheduleId
                );

                ps.setDate(
                    3,
                    Date.valueOf(date)
                );

                try (ResultSet rs =
                    ps.executeQuery()) {

                    if (rs.next()) {
                        JOptionPane.showMessageDialog(
                            parent,
                            "You are already marked present for this subject today.",
                            "Already Recorded",
                            JOptionPane.INFORMATION_MESSAGE
                        );
                        return;
                    }
                }

            } catch (SQLException e) {
                err(e);
                return;
            }

            // Insert attendance
            String insert =
                "INSERT INTO attendance " +
                "(student_barcode,schedule_id," +
                "attendance_date,attendance_time,status) " +
                "VALUES (?,?,?,?,?)";

            try (PreparedStatement ps =
                db.prepareStatement(insert)) {

                ps.setString(
                    1,
                    currentBarcode
                );

                ps.setInt(
                    2,
                    scheduleId
                );

                ps.setDate(
                    3,
                    Date.valueOf(date)
                );

                ps.setTime(
                    4,
                    Time.valueOf(
                        time.withNano(0)
                    )
                );

                ps.setString(
                    5,
                    "Present"
                );

                ps.executeUpdate();

                JOptionPane.showMessageDialog(
                    parent,
                    "Attendance Recorded Successfully!\n\n"
                    + "Name: " + currentStudentName
                    + "\nSection: " + currentSection
                    + "\nDate: " + date
                    + "\nTime: "
                    + time.format(
                        DateTimeFormatter.ofPattern(
                            "hh:mm a"
                        )
                    )
                    + "\n\nStatus: PRESENT",
                    "Attendance Successful",
                    JOptionPane.INFORMATION_MESSAGE
                );

                // Refresh Records
                mainTabs.setComponentAt(
                    1,
                    recordsPanel()
                );

                // Open Records automatically
                mainTabs.setSelectedIndex(1);

                // Refresh Reports
                mainTabs.setComponentAt(
                    2,
                    reportsPanel()
                );

            } catch (SQLIntegrityConstraintViolationException e) {
                JOptionPane.showMessageDialog(
                    parent,
                    "Attendance has already been recorded for this subject today.",
                    "Already Recorded",
                    JOptionPane.INFORMATION_MESSAGE
                );

            } catch (SQLException e) {
                JOptionPane.showMessageDialog(
                    parent,
                    "Unable to record attendance:\n"
                    + e.getMessage(),
                    "Attendance Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }

        // ==========================================================
        // RECORDS VIEW
        // ==========================================================

        static JPanel recordsPanel() {
            JPanel p = new JPanel(
                new BorderLayout(10, 10)
            );

            p.setBorder(
                BorderFactory.createEmptyBorder(
                    15, 15, 15, 15
                )
            );

            JLabel title = new JLabel(
                "Present Attendance Records",
                SwingConstants.CENTER
            );

            title.setFont(
                new Font(
                    "SansSerif",
                    Font.BOLD,
                    22
                )
            );

            p.add(
                title,
                BorderLayout.NORTH
            );

            String sql =
                "SELECT " +
                "a.attendance_date, " +
                "a.attendance_time, " +
                "s.day_of_week, " +
                "s.subject, " +
                "s.start_time, " +
                "s.end_time " +
                "FROM attendance a " +
                "JOIN schedules s " +
                "ON a.schedule_id=s.id " +
                "WHERE a.student_barcode=? " +
                "AND a.status='Present' " +
                "ORDER BY " +
                "a.attendance_date DESC, " +
                "a.attendance_time DESC";

            DefaultTableModel m = model(
                sql,
                new String[]{
                    "Date",
                    "Time Present",
                    "Day",
                    "Subject",
                    "Start Time",
                    "End Time"
                },
                currentBarcode
            );

            JTable table =
                new JTable(m);

            table.setRowHeight(30);
            table.setAutoCreateRowSorter(true);

            JScrollPane scroll =
                new JScrollPane(table);

            p.add(
                scroll,
                BorderLayout.CENTER
            );

            JPanel info = new JPanel(
                new FlowLayout(
                    FlowLayout.LEFT
                )
            );

            info.add(
                new JLabel(
                    "Student: "
                    + currentStudentName
                    + " | Section: "
                    + currentSection
                    + " | Barcode: "
                    + currentBarcode
                )
            );

            p.add(
                info,
                BorderLayout.SOUTH
            );

            return p;
        }

        // ==========================================================
        // REPORTS VIEW - ABSENT DAYS
        // ==========================================================

        static JPanel reportsPanel() {
            JPanel p = new JPanel(
                new BorderLayout(10, 10)
            );

            p.setBorder(
                BorderFactory.createEmptyBorder(
                    15, 15, 15, 15
                )
            );

            JLabel title = new JLabel(
                "Absence Report",
                SwingConstants.CENTER
            );

            title.setFont(
                new Font(
                    "SansSerif",
                    Font.BOLD,
                    22
                )
            );

            p.add(
                title,
                BorderLayout.NORTH
            );

            String sql =
                "SELECT " +
                "d.class_date, " +
                "DAYNAME(d.class_date) AS day_name, " +
                "s.subject, " +
                "s.start_time, " +
                "s.end_time " +
                "FROM (" +
                "SELECT " +
                "DATE_SUB(CURDATE(), INTERVAL n DAY) AS class_date " +
                "FROM (" +
                "SELECT 1 n UNION SELECT 2 UNION SELECT 3 " +
                "UNION SELECT 4 UNION SELECT 5 " +
                "UNION SELECT 6 UNION SELECT 7 " +
                "UNION SELECT 8 UNION SELECT 9 " +
                "UNION SELECT 10 UNION SELECT 11 " +
                "UNION SELECT 12 UNION SELECT 13 " +
                "UNION SELECT 14 UNION SELECT 15 " +
                "UNION SELECT 16 UNION SELECT 17 " +
                "UNION SELECT 18 UNION SELECT 19 " +
                "UNION SELECT 20 UNION SELECT 21 " +
                "UNION SELECT 22 UNION SELECT 23 " +
                "UNION SELECT 24 UNION SELECT 25 " +
                "UNION SELECT 26 UNION SELECT 27 " +
                "UNION SELECT 28 UNION SELECT 29 " +
                "UNION SELECT 30" +
                ") numbers" +
                ") d " +
                "JOIN schedules s " +
                "ON s.section=? " +
                "AND s.day_of_week=DAYNAME(d.class_date) " +
                "LEFT JOIN attendance a " +
                "ON a.student_barcode=? " +
                "AND a.schedule_id=s.id " +
                "AND a.attendance_date=d.class_date " +
                "AND a.status='Present' " +
                "WHERE a.id IS NULL " +
                "AND d.class_date < CURDATE() " +
                "ORDER BY " +
                "d.class_date DESC, " +
                "s.start_time";

            DefaultTableModel m = model(
                sql,
                new String[]{
                    "Date",
                    "Day",
                    "Subject",
                    "Start Time",
                    "End Time"
                },
                currentSection,
                currentBarcode
            );

            JTable table =
                new JTable(m);

            table.setRowHeight(30);
            table.setAutoCreateRowSorter(true);

            JScrollPane scroll =
                new JScrollPane(table);

            p.add(
                scroll,
                BorderLayout.CENTER
            );

            JPanel info = new JPanel(
                new FlowLayout(
                    FlowLayout.LEFT
                )
            );

            info.add(
                new JLabel(
                    "Student: "
                    + currentStudentName
                    + " | Section: "
                    + currentSection
                )
            );

            p.add(
                info,
                BorderLayout.SOUTH
            );

            return p;
        }

        // ==========================================================
        // STUDENT MANAGEMENT
        // ==========================================================

        static JPanel studentsPanel() {
            JPanel p = new JPanel(
                new BorderLayout()
            );

            p.add(
                new JLabel(
                    "Student Management View",
                    SwingConstants.CENTER
                ),
                BorderLayout.CENTER
            );

            return p;
        }
    }

    public static void main(String[] args) {
        AttendanceApp.start();
    }
}