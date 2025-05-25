// Filename: LoginDialog.java
package com.nextque.ui;

import com.nextque.auth.AuthService;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class LoginDialog extends JDialog {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton cancelButton;
    private JButton signUpButton;
    private AuthService authService;
    private boolean authenticated = false;

    public LoginDialog(Frame parent, AuthService authService) {
        super(parent, "Login - NextQue", true);
        this.authService = authService;

        // Set dialog properties
        // setUndecorated(false); // Default is false, usually fine.
        // For custom shaped window or L&F decorations, you might explore setUndecorated(true)
        // and getRootPane().setWindowDecorationStyle(JRootPane.FRAME);

        initComponents();
        layoutComponents();
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        usernameField = new JTextField(20);
        usernameField.setFont(UITheme.FONT_INPUT);
        usernameField.putClientProperty("JTextField.placeholderText", "Enter your username"); // FlatLaf placeholder

        passwordField = new JPasswordField(20);
        passwordField.setFont(UITheme.FONT_INPUT);
        passwordField.putClientProperty("JTextField.placeholderText", "Enter your password"); // FlatLaf placeholder

        loginButton = new JButton("Login");
        UITheme.stylePrimaryButton(loginButton); // Apply theme style

        signUpButton = new JButton("Sign Up");
        UITheme.styleSecondaryButton(signUpButton); // Apply theme style

        cancelButton = new JButton("Cancel");
        // Using default button style for Cancel, or you can create a styleTertiaryButton if needed
        cancelButton.setFont(UITheme.FONT_BUTTON);
        cancelButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // For a less prominent cancel button, you might use:
        // cancelButton.putClientProperty(com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE, com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        // cancelButton.setForeground(UITheme.COLOR_TEXT_LIGHT);


        loginButton.addActionListener(this::performLogin);
        passwordField.addActionListener(this::performLogin); // Allow login on Enter in password field

        cancelButton.addActionListener(e -> {
            authenticated = false;
            dispose();
        });

        signUpButton.addActionListener(e -> {
            // Assuming SignUpDialog is correctly defined and themed
            SignUpDialog signUpDialog = new SignUpDialog(this, authService);
            signUpDialog.setVisible(true);
            // Optionally, check if signup was successful and then close login, or handle flow within SignUpDialog
        });

        // Close dialog on Escape key
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void performLogin(ActionEvent e) {
        String username = usernameField.getText().trim(); // Trim username
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Username and Password cannot be empty.",
                    "Login Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (authService.login(username, password)) {
            authenticated = true;
            dispose();
        } else {
            // Consider a more subtle error indication, e.g., a label, or a brief shake
            // AnimationUtil.shakeComponent(this); // Placeholder for a shake animation utility
            JOptionPane.showMessageDialog(this,
                    "Invalid username or password. Please try again.",
                    "Login Failed",
                    JOptionPane.ERROR_MESSAGE);
            passwordField.setText(""); // Clear password field
            passwordField.requestFocusInWindow(); // Keep focus on password after failed attempt
        }
    }

    private void layoutComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20)); // Gaps between sections
        mainPanel.setBorder(new EmptyBorder(30, 40, 30, 40)); // Generous padding around the dialog content
        mainPanel.setBackground(UITheme.COLOR_BACKGROUND_MAIN); // Use theme background

        // Header
        JLabel titleLabel = new JLabel("NextQue System Login", SwingConstants.CENTER);
        titleLabel.setFont(UITheme.FONT_TITLE_H2);
        titleLabel.setForeground(UITheme.COLOR_PRIMARY_NAVY); // Corrected from COLOR_PRIMARY_DARK
        titleLabel.setIcon(UITheme.getIcon("login_key.svg", 32, 32)); // Ensure icon exists
        titleLabel.setIconTextGap(10);
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Input fields panel
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setOpaque(false); // Transparent to show mainPanel background
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5); // Padding around components
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0; // Allow fields to expand horizontally

        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(UITheme.FONT_LABEL);
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.2; gbc.anchor = GridBagConstraints.LINE_END; // Align label text to the right
        fieldsPanel.add(userLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.8; gbc.anchor = GridBagConstraints.LINE_START;
        fieldsPanel.add(usernameField, gbc);

        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(UITheme.FONT_LABEL);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.2; gbc.anchor = GridBagConstraints.LINE_END;
        fieldsPanel.add(passLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.8; gbc.anchor = GridBagConstraints.LINE_START;
        fieldsPanel.add(passwordField, gbc);

        mainPanel.add(fieldsPanel, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0)); // Align buttons to the right
        buttonPanel.setOpaque(false);
        buttonPanel.add(signUpButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(loginButton); // Primary action often last or most prominent

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Set the main panel as the content pane of the JDialog
        setContentPane(mainPanel);
    }

    public boolean isAuthenticated() {
        return authenticated;
    }
}
