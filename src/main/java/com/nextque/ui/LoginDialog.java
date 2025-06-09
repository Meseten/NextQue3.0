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

        initComponents();
        layoutComponents();
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }

    private void initComponents() {
        usernameField = new JTextField(20);
        usernameField.setFont(UITheme.FONT_INPUT);
        usernameField.putClientProperty("JTextField.placeholderText", "Enter your username");

        passwordField = new JPasswordField(20);
        passwordField.setFont(UITheme.FONT_INPUT);
        passwordField.putClientProperty("JTextField.placeholderText", "Enter your password");

        loginButton = new JButton("Login");
        UITheme.stylePrimaryButton(loginButton);

        signUpButton = new JButton("Sign Up");
        UITheme.styleSecondaryButton(signUpButton);

        cancelButton = new JButton("Cancel");
        cancelButton.setFont(UITheme.FONT_BUTTON);
        cancelButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        loginButton.addActionListener(this::performLogin);
        passwordField.addActionListener(this::performLogin);

        cancelButton.addActionListener(e -> {
            authenticated = false;
            dispose();
        });

        signUpButton.addActionListener(e -> {
            SignUpDialog signUpDialog = new SignUpDialog(this, authService);
            signUpDialog.setVisible(true);
        });

        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void performLogin(ActionEvent e) {
        String username = usernameField.getText().trim();
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
            JOptionPane.showMessageDialog(this,
                    "Invalid username or password. Please try again.",
                    "Login Failed",
                    JOptionPane.ERROR_MESSAGE);
            passwordField.setText("");
            passwordField.requestFocusInWindow();
        }
    }

    private void layoutComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(20, 20));
        mainPanel.setBorder(new EmptyBorder(30, 40, 30, 40));
        mainPanel.setBackground(UITheme.COLOR_BACKGROUND_MAIN);

        JLabel titleLabel = new JLabel("NextQue System Login", SwingConstants.CENTER);
        titleLabel.setFont(UITheme.FONT_TITLE_H2);
        titleLabel.setForeground(UITheme.COLOR_PRIMARY_NAVY);
        titleLabel.setIcon(UITheme.getIcon("login_key.svg", 32, 32));
        titleLabel.setIconTextGap(10);
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(UITheme.FONT_LABEL);
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.2; gbc.anchor = GridBagConstraints.LINE_END;
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

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(signUpButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(loginButton);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    public boolean isAuthenticated() {
        return authenticated;
    }
}
