package javabasics;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class AdminDashboard extends JFrame {

    private static final String URL =
            "jdbc:mysql://localhost:3306/attendance_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    private final String adminUsername;

    private JLabel studentsValue;
    private JLabel instructorsValue;
    private JLabel subjectsValue;
    private JLabel schedulesValue;
    private JLabel statusLabel;

    private final Color backgroundColor = new Color(245, 247, 250);
    private final Color primaryColor = new Color(37, 99, 235);
    private final Color textColor = new Color(31, 41, 55);

    public AdminDashboard(String adminUsername) {
        this.adminUsername = adminUsername;
        initializeUI();
        refreshDashboard();
    }

    private Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "MySQL Connector/J is missing from the project libraries.", e);
        }
        return DriverManager.getConnection(URL, DB_USER, DB_PASS);
    }

    private void initializeUI() {
        setTitle("Attendance System - Admin Dashboard");
        setSize(1100, 720);
        setMinimumSize(new java.awt.Dimension(850, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(15, 15));
        getContentPane().setBackground(backgroundColor);

        JPanel header = new JPanel(new BorderLayout(10, 5));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 5));
        heading.setOpaque(false);

        JLabel title = new JLabel("Admin Dashboard");
        title.setFont(new Font("SansSerif", Font.BOLD, 25));
        title.setForeground(textColor);

        JLabel welcome = new JLabel("Welcome, " + adminUsername
                + " | PHINMA COC Carmen Campus");
        welcome.setFont(new Font("SansSerif", Font.PLAIN, 13));
        welcome.setForeground(new Color(100, 116, 139));

        heading.add(title);
        heading.add(welcome);

        JPanel headerButtons = new JPanel(new GridLayout(1, 2, 8, 0));
        headerButtons.setOpaque(false);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshDashboard());

        JButton logoutButton = new JButton("Logout");
        logoutButton.addActionListener(e -> logout());

        headerButtons.add(refreshButton);
        headerButtons.add(logoutButton);

        header.add(heading, BorderLayout.CENTER);
        header.add(headerButtons, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(15, 18));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(5, 22, 10, 22));

        JLabel overviewTitle = new JLabel("System Overview");
        overviewTitle.setFont(new Font("SansSerif", Font.BOLD, 19));
        overviewTitle.setForeground(textColor);

        JPanel overviewSection = new JPanel(new BorderLayout(0, 12));
        overviewSection.setOpaque(false);
        overviewSection.add(overviewTitle, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 4, 14, 14));
        cards.setOpaque(false);

        studentsValue = new JLabel("0", SwingConstants.CENTER);
        instructorsValue = new JLabel("0", SwingConstants.CENTER);
        subjectsValue = new JLabel("0", SwingConstants.CENTER);
        schedulesValue = new JLabel("0", SwingConstants.CENTER);

        cards.add(createStatCard("Active Students", studentsValue,
                new Color(219, 234, 254)));
        cards.add(createStatCard("Active Instructors", instructorsValue,
                new Color(220, 252, 231)));
        cards.add(createStatCard("Active Subjects", subjectsValue,
                new Color(237, 233, 254)));
        cards.add(createStatCard("Today's Classes", schedulesValue,
                new Color(254, 243, 199)));

        overviewSection.add(cards, BorderLayout.CENTER);
        content.add(overviewSection, BorderLayout.NORTH);

        JPanel modules = new JPanel(new BorderLayout(0, 12));
        modules.setOpaque(false);

        JLabel modulesTitle = new JLabel("Management Modules");
        modulesTitle.setFont(new Font("SansSerif", Font.BOLD, 19));
        modulesTitle.setForeground(textColor);
        modules.add(modulesTitle, BorderLayout.NORTH);

        JPanel buttons = new JPanel(new GridLayout(3, 2, 14, 14));
        buttons.setOpaque(false);

        buttons.add(createModuleButton("Student Management",
                "Manage student barcodes, names, sections, and active status."));
        buttons.add(createModuleButton("Instructor Management",
                "Manage instructor codes, accounts, and active status."));
        buttons.add(createModuleButton("Subject Management",
                "Manage subject codes, names, and active status."));
        buttons.add(createModuleButton("Schedule Management",
                "Manage sections, subjects, instructors, days, and class times."));
        buttons.add(createModuleButton("Attendance Monitoring",
                "View attendance records, including Present and Late."));
        buttons.add(createModuleButton("Reports & Analytics",
                "Review semester attendance, absences, and attendance rates."));

        modules.add(buttons, BorderLayout.CENTER);
        content.add(modules, BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);

        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 22, 12, 22));
        statusLabel.setForeground(new Color(100, 116, 139));
        add(statusLabel, BorderLayout.SOUTH);
    }

    private JPanel createStatCard(String heading, JLabel value,
                                  Color cardColor) {
        JPanel card = new JPanel(new BorderLayout(5, 15));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 226, 234)),
                BorderFactory.createEmptyBorder(15, 8, 15, 8)));

        JLabel label = new JLabel(heading, SwingConstants.CENTER);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setForeground(textColor);

        value.setFont(new Font("SansSerif", Font.BOLD, 30));
        value.setForeground(primaryColor);
        value.setOpaque(true);
        value.setBackground(cardColor);

        card.add(label, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JButton createModuleButton(String name, String description) {
        JButton button = new JButton(
                "<html><div style='text-align:center'><b>"
                + name + "</b><br><br>"
                + description + "</div></html>");

        button.setFont(new Font("SansSerif", Font.PLAIN, 13));
        button.setFocusPainted(false);
        button.setBackground(Color.WHITE);
        button.setForeground(textColor);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 218, 230)),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));

        button.addActionListener(e -> openModule(name, description));
        return button;
    }






private void openModule(String name, String description) {
    if ("Student Management".equals(name)) {
        new AdminStudents(adminUsername).setVisible(true);
    } else if ("Instructor Management".equals(name)) {
        new AdminInstructors(adminUsername).setVisible(true);
    } else if ("Subject Management".equals(name)) {
        new AdminSubjects(adminUsername).setVisible(true);
    } else if ("Schedule Management".equals(name)) {
        new AdminSchedules(adminUsername).setVisible(true);
    } else if ("Attendance Monitoring".equals(name)) {
        new AdminAttendance(adminUsername).setVisible(true);
    } else if ("Reports & Analytics".equals(name)) {
        new AdminReports(adminUsername).setVisible(true);
    } else {
        JOptionPane.showMessageDialog(
                this,
                description + "\n\nThis module will be implemented next.",
                name,
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}






    private void refreshDashboard() {
        try (Connection conn = getConnection()) {
            studentsValue.setText(String.valueOf(
                    count(conn, "SELECT COUNT(*) FROM students WHERE active=1")));

            instructorsValue.setText(String.valueOf(
                    count(conn, "SELECT COUNT(*) FROM instructors WHERE active=1")));

            subjectsValue.setText(String.valueOf(
                    count(conn, "SELECT COUNT(*) FROM subjects WHERE active=1")));

            String today = LocalDate.now().getDayOfWeek()
                    .getDisplayName(TextStyle.FULL, Locale.ENGLISH);

            String sql = "SELECT COUNT(*) FROM schedules "
                    + "WHERE active=1 AND day_of_week=?";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, today);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        schedulesValue.setText(String.valueOf(rs.getInt(1)));
                    }
                }
            }

            statusLabel.setText("Database connected | Dashboard refreshed");

        } catch (SQLException e) {
            statusLabel.setText("Database connection or query failed");
            JOptionPane.showMessageDialog(
                    this,
                    "Unable to load dashboard statistics.\n\n"
                    + e.getMessage()
                    + "\n\nCheck XAMPP MySQL and the database tables.",
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private int count(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    private void logout() {

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            dispose();
            new Main().setVisible(true);

        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() ->
                new AdminDashboard("admin").setVisible(true));
    }
}