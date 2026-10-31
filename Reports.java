package javabasics;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class Reports extends JFrame {
    JTable table;

    public Reports() {
        setTitle("Semester Absence Report - PHINMA COC Carmen Campus");
        setSize(1000, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        buildUI();
    }

    void buildUI() {
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel header = new JPanel(new BorderLayout(5, 5));
        JLabel title = new JLabel(
            "Semester Absence Report (July 7 - October 30, 2026)",
            SwingConstants.CENTER
        );
        title.setFont(new Font("SansSerif", Font.BOLD, 20));

        JLabel info = new JLabel(
            "Student: " + Login.currentStudentName +
            " | Section: " + Login.currentSection
        );

        header.add(title, BorderLayout.NORTH);
        header.add(info, BorderLayout.SOUTH);
        main.add(header, BorderLayout.NORTH);

        String sql =
            "WITH RECURSIVE dates AS (" +
            "SELECT DATE('2026-07-07') AS class_date " +
            "UNION ALL " +
            "SELECT DATE_ADD(class_date, INTERVAL 1 DAY) " +
            "FROM dates WHERE class_date < DATE('2026-10-30')" +
            ") " +
            "SELECT DATE_FORMAT(d.class_date,'%Y-%m-%d') AS class_date, " +
            "DAYNAME(d.class_date) AS class_day, " +
            "sub.subject_name, COALESCE(i.name,'Not assigned') AS instructor_name, " +
            "TIME_FORMAT(s.start_time,'%h:%i %p') AS start_time, " +
            "TIME_FORMAT(s.end_time,'%h:%i %p') AS end_time " +
            "FROM dates d " +
            "JOIN schedules s ON s.section = ? " +
            "AND s.active = 1 " +
            "AND s.day_of_week = DAYNAME(d.class_date) " +
            "JOIN subjects sub ON sub.id = s.subject_id " +
            "LEFT JOIN instructors i ON i.id = s.instructor_id " +
            "LEFT JOIN attendance a ON a.student_barcode = ? " +
            "AND a.schedule_id = s.id " +
            "AND a.attendance_date = d.class_date " +
            "AND a.status IN ('Present','Late') " +
            "WHERE (d.class_date < CURDATE() " +
            "OR (d.class_date = CURDATE() " +
            "AND CURRENT_TIME() >= s.end_time)) " +
            "AND a.id IS NULL " +
            "ORDER BY d.class_date DESC, s.start_time";

        DefaultTableModel model = Login.model(sql, new String[]{
            "Date", "Day", "Subject", "Instructor", "Start Time", "End Time"
        }, Login.currentSection, Login.currentBarcode);

        table = new JTable(model);
        table.setRowHeight(30);
        table.setAutoCreateRowSorter(true);
        main.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton scan = new JButton("Scan Attendance");
        JButton records = new JButton("Records");
        JButton logout = new JButton("Logout");

        buttons.add(scan);
        buttons.add(records);
        buttons.add(logout);
        main.add(buttons, BorderLayout.SOUTH);
        setContentPane(main);

        scan.addActionListener(e -> {
            dispose();
            Login.openScanAttendance();
        });

        records.addActionListener(e -> {
            dispose();
            Login.openRecords();
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