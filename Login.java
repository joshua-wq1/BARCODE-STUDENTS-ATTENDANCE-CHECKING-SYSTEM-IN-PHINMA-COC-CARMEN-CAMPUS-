package javabasics;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class Login {
    static final String URL = "jdbc:mysql://localhost:3306/attendance_db";
    static final String DB_USER = "root";
    static final String DB_PASS = "";

    static Connection db;
    static String currentBarcode = "";
    static String currentStudentName = "";
    static String currentSection = "";
    static JFrame loginFrame;
    static JTextField barcodeField;

    public static boolean start() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            if (db == null || db.isClosed() || !db.isValid(2)) {
                db = DriverManager.getConnection(URL, DB_USER, DB_PASS);
            }

            showLogin();
            return true;
        } catch (ClassNotFoundException e) {
            JOptionPane.showMessageDialog(null,
                "MySQL Connector/J is missing from NetBeans Libraries!\n" + e.getMessage(),
                "Driver Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                "Cannot connect to MySQL:\n" + e.getMessage()
                + "\n\nMake sure MySQL is running in XAMPP and attendance_db exists.",
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    static void showLogin() {
        loginFrame = new JFrame("Student Barcode Login");
        loginFrame.setSize(450, 300);
        loginFrame.setLocationRelativeTo(null);
        loginFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(30, 35, 30, 35));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("PHINMA COC CARMEN");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel instruction = new JLabel("Scan or enter your student barcode");
        instruction.setAlignmentX(Component.CENTER_ALIGNMENT);

        barcodeField = new JTextField();
        barcodeField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JButton verifyButton = new JButton("Verify Barcode");
        verifyButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        verifyButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        JButton backButton = new JButton("Back to Main Menu");
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(title);
        panel.add(Box.createVerticalStrut(20));
        panel.add(instruction);
        panel.add(Box.createVerticalStrut(10));
        panel.add(barcodeField);
        panel.add(Box.createVerticalStrut(15));
        panel.add(verifyButton);
        panel.add(Box.createVerticalStrut(10));
        panel.add(backButton);

        loginFrame.setContentPane(panel);
        verifyButton.addActionListener(e -> verifyBarcode());
        barcodeField.addActionListener(e -> verifyBarcode());

        backButton.addActionListener(e -> {
            loginFrame.dispose();
            new Main().setVisible(true);
        });

        loginFrame.setVisible(true);
        barcodeField.requestFocusInWindow();
    }

    static void verifyBarcode() {
        String barcode = barcodeField.getText().trim();

        if (barcode.isEmpty()) {
            JOptionPane.showMessageDialog(loginFrame,
                "Please scan or enter your student barcode.",
                "Barcode Required", JOptionPane.WARNING_MESSAGE);
            barcodeField.requestFocusInWindow();
            return;
        }

        String sql = "SELECT barcode, name, section FROM students "
                   + "WHERE barcode=? AND active=1";

        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setString(1, barcode);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    currentBarcode = rs.getString("barcode");
                    currentStudentName = rs.getString("name");
                    currentSection = rs.getString("section");

                    JOptionPane.showMessageDialog(loginFrame,
                        "Welcome, " + currentStudentName
                        + "!\nSection: " + currentSection
                        + "\nBarcode: " + currentBarcode,
                        "Login Successful", JOptionPane.INFORMATION_MESSAGE);

                    loginFrame.dispose();
                    new ScanAttendance().setVisible(true);
                } else {
                    JOptionPane.showMessageDialog(loginFrame,
                        "Invalid barcode or inactive student.",
                        "Login Failed", JOptionPane.ERROR_MESSAGE);
                    barcodeField.setText("");
                    barcodeField.requestFocusInWindow();
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(loginFrame,
                "Student lookup failed:\n" + e.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    static DefaultTableModel model(String sql, String[] columns, Object... params) {
        DefaultTableModel tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        try (PreparedStatement ps = db.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object[] row = new Object[columns.length];
                    for (int i = 0; i < columns.length; i++) {
                        row[i] = rs.getObject(i + 1);
                    }
                    tableModel.addRow(row);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                "Could not load table data:\n" + e.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
        }

        return tableModel;
    }

    static void logout(JFrame currentFrame) {
        currentBarcode = "";
        currentStudentName = "";
        currentSection = "";

        if (currentFrame != null) {
            currentFrame.dispose();
        }

        new Main().setVisible(true);
    }
    static void openScanAttendance() {
        new ScanAttendance().setVisible(true);
    }

    static void openRecords() {
        new Records().setVisible(true);
    }

    static void openReports() {
        new Reports().setVisible(true);
    }
}