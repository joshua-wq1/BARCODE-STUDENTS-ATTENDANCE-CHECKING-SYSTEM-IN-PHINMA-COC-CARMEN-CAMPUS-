package javabasics;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class Records extends JFrame {
    JTable table;

    public Records() {
        setTitle("Attendance Records - PHINMA COC Carmen Campus");
        setSize(1000, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        buildUI();
    }

    void buildUI() {
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel header = new JPanel(new BorderLayout(5, 5));
        JLabel title = new JLabel("Attendance Records", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));

        JLabel info = new JLabel(
            "Student: " + Login.currentStudentName +
            " | Section: " + Login.currentSection +
            " | Barcode: " + Login.currentBarcode
        );

        header.add(title, BorderLayout.NORTH);
        header.add(info, BorderLayout.SOUTH);
        main.add(header, BorderLayout.NORTH);

        String sql =
            "SELECT DATE_FORMAT(a.attendance_date,'%Y-%m-%d') AS attendance_date, " +
            "DATE_FORMAT(a.attendance_time,'%h:%i %p') AS attendance_time, " +
            "s.day_of_week, sub.subject_name, " +
            "COALESCE(i.name,'Not assigned') AS instructor_name, " +
            "TIME_FORMAT(s.start_time,'%h:%i %p') AS start_time, " +
            "TIME_FORMAT(s.end_time,'%h:%i %p') AS end_time, a.status " +
            "FROM attendance a " +
            "JOIN schedules s ON s.id = a.schedule_id " +
            "JOIN subjects sub ON sub.id = s.subject_id " +
            "LEFT JOIN instructors i ON i.id = s.instructor_id " +
            "WHERE a.student_barcode = ? " +
            "AND a.status IN ('Present','Late') " +
            "ORDER BY a.attendance_date DESC, a.attendance_time DESC";

        DefaultTableModel model = Login.model(sql, new String[]{
            "Date", "Time", "Day", "Subject",
            "Instructor", "Start Time", "End Time", "Status"
        }, Login.currentBarcode);

        table = new JTable(model);
        table.setRowHeight(30);
        table.setAutoCreateRowSorter(true);
        main.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton scan = new JButton("Scan Attendance");
        JButton reports = new JButton("Reports");
        JButton logout = new JButton("Logout");

        buttons.add(scan);
        buttons.add(reports);
        buttons.add(logout);
        main.add(buttons, BorderLayout.SOUTH);
        setContentPane(main);

        scan.addActionListener(e -> {
            dispose();
            Login.openScanAttendance();
        });

        reports.addActionListener(e -> {
            dispose();
            Login.openReports();
        });

        logout.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                this, "Are you sure you want to logout?",
                "Logout", JOptionPane.YES_NO_OPTION
            );
            if (choice == JOptionPane.YES_OPTION) {
                Login.logout(this);
            }
        });
    }
}