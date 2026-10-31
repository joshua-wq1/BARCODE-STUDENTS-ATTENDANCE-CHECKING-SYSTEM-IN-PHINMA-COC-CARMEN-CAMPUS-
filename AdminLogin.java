package javabasics;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.*;

public class AdminLogin extends JFrame {
    private static final String URL =
        "jdbc:mysql://localhost:3306/attendance_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    private final JTextField usernameField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);

    public AdminLogin() {
        setTitle("Admin Login - PHINMA COC Carmen Campus");
        setSize(430, 330);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        buildForm();
    }

    private void buildForm() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(25, 30, 25, 30));
        root.setBackground(new Color(245, 247, 250));
        setContentPane(root);

        JLabel title = new JLabel("Administrator Login", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 23));
        title.setForeground(new Color(24, 59, 86));
        root.add(title, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(4, 1, 6, 6));
        form.setOpaque(false);

        form.add(new JLabel("Username"));
        form.add(usernameField);
        form.add(new JLabel("Password"));
        form.add(passwordField);
        root.add(form, BorderLayout.CENTER);

        JButton loginButton = new JButton("Login");
        JButton backButton = new JButton("Back to Main Menu");

        loginButton.addActionListener(e -> login());
        passwordField.addActionListener(e -> login());
        backButton.addActionListener(e -> {
            dispose();
            new Main().setVisible(true);
        });

        JPanel buttons = new JPanel(new GridLayout(1, 2, 10, 0));
        buttons.setOpaque(false);
        buttons.add(loginButton);
        buttons.add(backButton);
        root.add(buttons, BorderLayout.SOUTH);
    }

    private Connection connect() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException(
                "MySQL Connector/J driver was not found. " +
                "Add the mysql-connector-j JAR to NetBeans Libraries.", e);
        }

        return DriverManager.getConnection(URL, DB_USER, DB_PASS);
    }

    private void login() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter both username and password.",
                "Missing Information", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "SELECT username FROM users " +
                     "WHERE username=? AND password=SHA2(?,256) " +
                     "AND role='admin'";

        try (Connection con = connect();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String adminUsername = rs.getString("username");
                    JOptionPane.showMessageDialog(this,
                        "Admin login successful.",
                        "Welcome", JOptionPane.INFORMATION_MESSAGE);

                    new AdminDashboard(adminUsername).setVisible(true);
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Invalid username or password, or this account " +
                        "does not have the admin role.",
                        "Login Failed", JOptionPane.ERROR_MESSAGE);
                    passwordField.setText("");
                }
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                "Database error:\n" + e.getMessage(),
                "Connection Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();

        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminLogin().setVisible(true));
    }
}