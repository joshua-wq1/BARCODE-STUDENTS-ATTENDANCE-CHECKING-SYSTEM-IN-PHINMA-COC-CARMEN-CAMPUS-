package javabasics;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class InstructorLogin extends JFrame {
    private final JTextField codeField = new JTextField(18);
    private final JPasswordField passwordField = new JPasswordField(18);

    public InstructorLogin() {
        setTitle("Instructor Login - PHINMA COC Carmen Campus");
        setSize(420, 300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        buildUI();
    }

    private void buildUI() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 5, 8, 5);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;

        JLabel title = new JLabel("Instructor Login", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 2;
        panel.add(title, g);

        g.gridwidth = 1;
        g.gridy = 1;
        g.gridx = 0;
        panel.add(new JLabel("Instructor Code:"), g);
        g.gridx = 1;
        panel.add(codeField, g);

        g.gridy = 2;
        g.gridx = 0;
        panel.add(new JLabel("Password:"), g);
        g.gridx = 1;
        panel.add(passwordField, g);

        JButton loginButton = new JButton("Login");
        JButton cancelButton = new JButton("Cancel");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttons.add(loginButton);
        buttons.add(cancelButton);

        g.gridx = 0;
        g.gridy = 3;
        g.gridwidth = 2;
        panel.add(buttons, g);

        setContentPane(panel);

        loginButton.addActionListener(e -> login());
        passwordField.addActionListener(e -> login());
        cancelButton.addActionListener(e -> dispose());
    }

    private void login() {
        String code = codeField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (code.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter your instructor code and password.");
            return;
        }

        String sql = "SELECT instructor_code FROM instructors " +
                     "WHERE instructor_code = ? " +
                     "AND password_hash = SHA2(?, 256) " +
                     "AND active = 1";

try {
    Class.forName("com.mysql.cj.jdbc.Driver");

    if (Login.db == null || Login.db.isClosed()) {
        Login.db = DriverManager.getConnection(
            Login.URL, Login.DB_USER, Login.DB_PASS
        );
    }

            try (PreparedStatement ps = Login.db.prepareStatement(sql)) {
                ps.setString(1, code);
                ps.setString(2, password);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        JOptionPane.showMessageDialog(this, "Login successful!");
                        dispose();
                        new InstructorDashboard(code).setVisible(true);
                    } else {
                        JOptionPane.showMessageDialog(this,
                            "Invalid instructor code or password.",
                            "Login Failed", JOptionPane.ERROR_MESSAGE);
                        passwordField.setText("");
                    }
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Database error: " + ex.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}