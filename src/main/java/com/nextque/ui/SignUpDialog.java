package com.nextque.ui;

import com.nextque.auth.AuthService;
import com.nextque.model.User;
import com.nextque.model.UserRole;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;

public class SignUpDialog extends JDialog {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JTextField fullNameField;
    private JButton signUpButton;
    private JButton cancelButton;
    private AuthService authService;
    private boolean signedUpSuccessfully = false;

    public SignUpDialog(Dialog parent, AuthService authService) {
        super(parent, "Create New Account - NextQue", true);
        this.authService = authService;
        initComponents();
        layoutComponents();
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        fullNameField = new JTextField(25);
        fullNameField.setFont(UITheme.FONT_INPUT);
        fullNameField.putClientProperty("JTextField.placeholderText", "e.g., Juan Dela Cruz");

        usernameField = new JTextField(25);
        usernameField.setFont(UITheme.FONT_INPUT);
        usernameField.putClientProperty("JTextField.placeholderText", "Choose a unique username");

        passwordField = new JPasswordField(25);
        passwordField.setFont(UITheme.FONT_INPUT);
        passwordField.putClientProperty("JTextField.placeholderText", "Min. 6 characters");

        confirmPasswordField = new JPasswordField(25);
        confirmPasswordField.setFont(UITheme.FONT_INPUT);
        confirmPasswordField.putClientProperty("JTextField.placeholderText", "Re-enter your password");

        signUpButton = new JButton("Create Account");
        UITheme.stylePrimaryButton(signUpButton);

        cancelButton = new JButton("Cancel");
        cancelButton.setFont(UITheme.FONT_BUTTON);
        cancelButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        signUpButton.addActionListener(this::performSignUp);
        confirmPasswordField.addActionListener(this::performSignUp);

        cancelButton.addActionListener(e -> {
            signedUpSuccessfully = false;
            dispose();
        });

        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void performSignUp(ActionEvent e) {
        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        char[] passwordChars = passwordField.getPassword();
        char[] confirmPasswordChars = confirmPasswordField.getPassword();

        if (username.isEmpty() || passwordChars.length == 0 || confirmPasswordChars.length == 0 || fullName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!Arrays.equals(passwordChars, confirmPasswordChars)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.", "Input Error", JOptionPane.ERROR_MESSAGE);
            passwordField.setText("");
            confirmPasswordField.setText("");
            passwordField.requestFocusInWindow();
            return;
        }

        if (passwordChars.length < 6) {
            JOptionPane.showMessageDialog(this, "Password must be at least 6 characters long.", "Password Policy", JOptionPane.WARNING_MESSAGE);
            passwordField.setText("");
            confirmPasswordField.setText("");
            passwordField.requestFocusInWindow();
            return;
        }

        String password = new String(passwordChars);
        User newUser = new User(username, password, UserRole.CUSTOMER, fullName);

        if (authService.isUsernameTaken(username)) {
            JOptionPane.showMessageDialog(this, "Username '" + username + "' is already taken. Please choose another.", "Username Unavailable", JOptionPane.WARNING_MESSAGE);
            usernameField.requestFocusInWindow();
            usernameField.selectAll();
            return;
        }

        if (authService.signUp(newUser)) {
            signedUpSuccessfully = true;
            JOptionPane.showMessageDialog(this,
                    "Account created successfully for " + fullName + "!\nYou can now log in with username: " + username,
                    "Signup Successful",
                    JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Signup failed due to an unexpected error. Please try again later.", "Error", JOptionPane.ERROR_MESSAGE);
        }

        Arrays.fill(passwordChars, '0');
        Arrays.fill(confirmPasswordChars, '0');
    }

    private void layoutComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(15,15));
        mainPanel.setBorder(new EmptyBorder(25, 35, 25, 35));
        mainPanel.setBackground(UITheme.COLOR_BACKGROUND_MAIN);

        JLabel titleLabel = new JLabel("Register for NextQue", SwingConstants.CENTER);
        titleLabel.setFont(UITheme.FONT_TITLE_H2);
        titleLabel.setForeground(UITheme.COLOR_PRIMARY_NAVY);
        titleLabel.setIcon(UITheme.getIcon("signup_user_add.svg", 32, 32));
        titleLabel.setIconTextGap(10);
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 5, 10, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        String[] labels = {"Full Name:", "Username:", "Password:", "Confirm Password:"};
        JTextField[] textFields = {fullNameField, usernameField, passwordField, confirmPasswordField};

        for (int i = 0; i < labels.length; i++) {
            JLabel label = new JLabel(labels[i]);
            label.setFont(UITheme.FONT_LABEL);
            gbc.gridx = 0; gbc.gridy = i; gbc.weightx = 0.3; gbc.anchor = GridBagConstraints.LINE_END;
            fieldsPanel.add(label, gbc);

            gbc.gridx = 1; gbc.gridy = i; gbc.weightx = 0.7; gbc.anchor = GridBagConstraints.LINE_START;
            fieldsPanel.add(textFields[i], gbc);
        }

        mainPanel.add(fieldsPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(cancelButton);
        buttonPanel.add(signUpButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    public boolean wasSignedUpSuccessfully() {
        return signedUpSuccessfully;
    }
}
