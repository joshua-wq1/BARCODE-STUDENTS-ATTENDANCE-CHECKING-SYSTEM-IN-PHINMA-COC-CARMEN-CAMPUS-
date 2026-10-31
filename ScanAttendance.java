package javabasics;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class ScanAttendance extends JFrame {
    private static final Color OK = new Color(0xB7E4C7);
    private static final Color IDLE = new Color(0xEEEEEE);

    private JTable scheduleTable;
    private DefaultTableModel scheduleModel;
    private JTextField barcodeField;
    private JLabel statusLabel;

    public ScanAttendance() {
        setTitle("Automatic Attendance - PHINMA COC Carmen Campus");
        setSize(1000, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        buildUI();
        loadSchedules();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel info = new JPanel(new GridLayout(3, 2, 10, 8));
        info.setBorder(BorderFactory.createTitledBorder("Student Information"));
        info.add(new JLabel("Name:"));
        info.add(new JLabel(Login.currentStudentName));
        info.add(new JLabel("Section:"));
        info.add(new JLabel(Login.currentSection));
        info.add(new JLabel("Date:"));
        info.add(new JLabel(LocalDate.now().toString()));
        main.add(info, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(8, 8));

        JPanel scannerPanel = new JPanel(new BorderLayout(8, 8));
        scannerPanel.setBorder(BorderFactory.createTitledBorder(
            "Scan Student Barcode to Record Attendance"
        ));

        barcodeField = new JTextField();
        barcodeField.setFont(new Font("SansSerif", Font.PLAIN, 20));
        barcodeField.setToolTipText("Scan the student's barcode here");

        JButton scanButton = new JButton("Scan Barcode");
        scanButton.setBackground(OK);
        scanButton.setFont(new Font("SansSerif", Font.BOLD, 14));

        scannerPanel.add(barcodeField, BorderLayout.CENTER);
        scannerPanel.add(scanButton, BorderLayout.EAST);

        statusLabel = new JLabel(
            "Ready. Scan your barcode to check attendance.",
            SwingConstants.CENTER
        );
        statusLabel.setOpaque(true);
        statusLabel.setBackground(IDLE);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));

        center.add(scannerPanel, BorderLayout.NORTH);
        center.add(statusLabel, BorderLayout.SOUTH);

        scheduleModel = new DefaultTableModel(
            new String[]{"ID", "Subject", "Instructor", "Start Time",
                "End Time", "Day"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        scheduleTable = new JTable(scheduleModel);
        scheduleTable.setRowHeight(30);
        scheduleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        scheduleTable.removeColumn(
            scheduleTable.getColumnModel().getColumn(0)
        );

        JScrollPane scroll = new JScrollPane(scheduleTable);
        scroll.setBorder(BorderFactory.createTitledBorder(
            "Active Class Schedules"
        ));

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.add(center, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        main.add(content, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton records = new JButton("Records");
        JButton reports = new JButton("Reports");
        JButton logout = new JButton("Logout");

        buttons.add(records);
        buttons.add(reports);
        buttons.add(logout);
        main.add(buttons, BorderLayout.SOUTH);

        setContentPane(main);

        barcodeField.addActionListener(e -> recordAttendance());
        scanButton.addActionListener(e -> recordAttendance());

        records.addActionListener(e -> {
            dispose();
            Login.openRecords();
        });

        reports.addActionListener(e -> {
            dispose();
            Login.openReports();
        });

        logout.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to logout?",
                "Logout",
                JOptionPane.YES_NO_OPTION
            );
            if (choice == JOptionPane.YES_OPTION) {
                Login.logout(this);
            }
        });
    }

    private void loadSchedules() {
        String sql =
            "SELECT s.id, sub.subject_name, i.name, " +
            "s.start_time, s.end_time, s.day_of_week " +
            "FROM schedules s " +
            "JOIN subjects sub ON sub.id = s.subject_id " +
            "JOIN instructors i ON i.id = s.instructor_id " +
            "WHERE s.section = ? AND s.active = 1 " +
            "ORDER BY FIELD(s.day_of_week,'Monday','Tuesday'," +
            "'Wednesday','Thursday','Friday','Saturday','Sunday'), " +
            "s.start_time";

        scheduleModel.setRowCount(0);

        try (PreparedStatement ps = Login.db.prepareStatement(sql)) {
            ps.setString(1, Login.currentSection);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    scheduleModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("subject_name"),
                        rs.getString("name"),
                        rs.getTime("start_time"),
                        rs.getTime("end_time"),
                        rs.getString("day_of_week")
                    });
                }
            }
        } catch (SQLException e) {
            showError(e);
        }
    }

    private void recordAttendance() {
        String scannedBarcode = barcodeField.getText().trim();

        if (scannedBarcode.isEmpty()) {
            showStatus("Please scan your student barcode.", false);
            barcodeField.requestFocusInWindow();
            return;
        }

        if (Login.currentBarcode == null ||
            !scannedBarcode.equals(Login.currentBarcode)) {
            showStatus(
                "Barcode does not match the logged-in student.",
                false
            );
            barcodeField.setText("");
            barcodeField.requestFocusInWindow();
            return;
        }

        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now().withNano(0);
        String day = date.getDayOfWeek().getDisplayName(
            java.time.format.TextStyle.FULL, Locale.ENGLISH
        );

        String findSchedule =
            "SELECT s.id, sub.subject_name, i.name, " +
            "s.start_time, s.end_time " +
            "FROM schedules s " +
            "JOIN subjects sub ON sub.id = s.subject_id " +
            "JOIN instructors i ON i.id = s.instructor_id " +
            "WHERE s.section = ? AND s.active = 1 " +
            "AND s.day_of_week = ? " +
            "AND TIME(?) >= s.start_time " +
            "AND TIME(?) <= s.end_time";

        try (PreparedStatement ps = Login.db.prepareStatement(findSchedule)) {
            ps.setString(1, Login.currentSection);
            ps.setString(2, day);
            ps.setTime(3, Time.valueOf(time));
            ps.setTime(4, Time.valueOf(time));

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    showStatus(
                        "No active class is scheduled for attendance right now.",
                        false
                    );
                    barcodeField.setText("");
                    barcodeField.requestFocusInWindow();
                    return;
                }

                int scheduleId = rs.getInt("id");
                String subject = rs.getString("subject_name");
                String instructor = rs.getString("name");
                LocalTime start = rs.getTime("start_time").toLocalTime();
                LocalTime end = rs.getTime("end_time").toLocalTime();

                if (rs.next()) {
                    showStatus(
                        "More than one active class matches this time. " +
                        "Please ask the administrator to check the schedules.",
                        false
                    );
                    barcodeField.setText("");
                    barcodeField.requestFocusInWindow();
                    return;
                }

                saveAttendance(
                    scheduleId, subject, instructor,
                    start, end, date, time
                );
            }
        } catch (SQLException e) {
            showError(e);
        }
    }

    private void saveAttendance(
        int scheduleId,
        String subject,
        String instructor,
        LocalTime start,
        LocalTime end,
        LocalDate date,
        LocalTime time
    ) {
        String check =
            "SELECT id FROM attendance " +
            "WHERE student_barcode = ? AND schedule_id = ? " +
            "AND attendance_date = ?";

        try (PreparedStatement ps = Login.db.prepareStatement(check)) {
            ps.setString(1, Login.currentBarcode);
            ps.setInt(2, scheduleId);
            ps.setDate(3, Date.valueOf(date));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    showStatus(
                        "Attendance has already been recorded for this class today.",
                        false
                    );
                    barcodeField.setText("");
                    barcodeField.requestFocusInWindow();
                    return;
                }
            }

            LocalTime lateThreshold = start.plusMinutes(15);
            String attendanceStatus =
                time.isBefore(lateThreshold) ? "Present" : "Late";

            String insert =
                "INSERT INTO attendance " +
                "(student_barcode, schedule_id, attendance_date, " +
                "attendance_time, status) VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement insertPs =
                    Login.db.prepareStatement(insert)) {
                insertPs.setString(1, Login.currentBarcode);
                insertPs.setInt(2, scheduleId);
                insertPs.setDate(3, Date.valueOf(date));
                insertPs.setTime(4, Time.valueOf(time));
                insertPs.setString(5, attendanceStatus);
                insertPs.executeUpdate();
            }

            String message =
                "Attendance recorded successfully!\n\n" +
                "Student: " + Login.currentStudentName +
                "\nSubject: " + subject +
                "\nInstructor: " + instructor +
                "\nDate: " + date +
                "\nTime: " + time.format(
                    DateTimeFormatter.ofPattern("hh:mm a")
                ) +
                "\nStatus: " + attendanceStatus.toUpperCase();

            showStatus(
                "Attendance recorded: " + attendanceStatus + " — " + subject,
                true
            );

            JOptionPane.showMessageDialog(
                this, message, "Attendance Successful",
                JOptionPane.INFORMATION_MESSAGE
            );

            barcodeField.setText("");
            barcodeField.requestFocusInWindow();

        } catch (SQLIntegrityConstraintViolationException e) {
            showStatus("Attendance already exists for this class today.", false);
        } catch (SQLException e) {
            showError(e);
        }
    }

    private void showStatus(String message, boolean success) {
        statusLabel.setText(message);
        statusLabel.setBackground(success ? OK : new Color(0xF5B7B1));
    }

    private void showError(SQLException e) {
        JOptionPane.showMessageDialog(
            this,
            "Database Error:\n" + e.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE
        );
    }
}