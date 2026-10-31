package javabasics;

import javax.swing.*;
import java.awt.*;

public class Main extends JFrame {
    public Main() {
        setTitle("Attendance Management System");
        setSize(550, 500);
        setMinimumSize(new Dimension(500, 450));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        buildUI();
    }

    private void buildUI() {
        JPanel main = new JPanel();
        main.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("ATTENDANCE MANAGEMENT SYSTEM");
        title.setFont(new Font("SansSerif", Font.BOLD, 21));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel school = new JLabel("PHINMA COC CARMEN");
        school.setFont(new Font("SansSerif", Font.PLAIN, 14));
        school.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel instruction = new JLabel("Select your account type");
        instruction.setFont(new Font("SansSerif", Font.BOLD, 16));
        instruction.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton studentButton = new JButton("STUDENT LOGIN");
        JButton instructorLoginButton = new JButton("INSTRUCTOR LOGIN");
        JButton adminButton = new JButton("ADMIN LOGIN");
        JButton exitButton = new JButton("EXIT");

        styleButton(studentButton, 55);
        styleButton(instructorLoginButton, 55);
        styleButton(adminButton, 55);
        styleButton(exitButton, 40);

        main.add(title);
        main.add(Box.createVerticalStrut(8));
        main.add(school);
        main.add(Box.createVerticalStrut(40));
        main.add(instruction);
        main.add(Box.createVerticalStrut(20));
        main.add(studentButton);
        main.add(Box.createVerticalStrut(15));
        main.add(instructorLoginButton);
        main.add(Box.createVerticalStrut(15));
        main.add(adminButton);
        main.add(Box.createVerticalStrut(20));
        main.add(exitButton);

        setContentPane(main);

        studentButton.addActionListener(e -> {
            studentButton.setEnabled(false);
            try {
                if (Login.start()) {
                    setVisible(false);
                }
            } finally {
                studentButton.setEnabled(true);
            }
        });

        instructorLoginButton.addActionListener(e -> {
            new InstructorLogin().setVisible(true);
        });

        adminButton.addActionListener(e -> {
            new AdminLogin().setVisible(true);
            dispose();
        });

        exitButton.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to exit?",
                "Exit System",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            );

            if (choice == JOptionPane.YES_OPTION) {
                System.exit(0);
            }
        });
    }

    private void styleButton(JButton button, int height) {
        button.setFont(new Font("SansSerif", Font.BOLD, 16));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        button.setFocusPainted(false);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}