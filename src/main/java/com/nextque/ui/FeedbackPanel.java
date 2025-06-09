package com.nextque.ui;

import com.nextque.db.DatabaseManager;
import com.nextque.model.Feedback;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

public class FeedbackPanel extends JPanel {
    private final DatabaseManager dbManager;
    private JTextField ticketNumberField;
    private JComboBox<Integer> ratingComboBox;
    private JTextArea commentsArea;
    private JButton submitButton;
    private JLabel titleLabel;

    public FeedbackPanel(DatabaseManager dbManager) {
        this.dbManager = dbManager;

        setBackground(UITheme.COLOR_BACKGROUND_MAIN);
        setBorder(UITheme.BORDER_PANEL_PADDING);

        initComponents();
        layoutComponents();
        attachListeners();
    }

    private void initComponents() {
        titleLabel = new JLabel("Share Your Feedback", SwingConstants.CENTER);
        titleLabel.setFont(UITheme.FONT_TITLE_H1);
        titleLabel.setForeground(UITheme.COLOR_PRIMARY_NAVY);
        titleLabel.setIcon(UITheme.getIcon("feedback_chat.svg", 32, 32));
        titleLabel.setIconTextGap(10);

        ticketNumberField = new JTextField(18);
        ticketNumberField.setEditable(false);
        ticketNumberField.setFont(UITheme.FONT_INPUT);
        ticketNumberField.setBackground(UITheme.COLOR_BACKGROUND_SECTION);
        ticketNumberField.setPreferredSize(new Dimension(0, 35));

        Integer[] ratings = {1, 2, 3, 4, 5};
        ratingComboBox = new JComboBox<>(ratings);
        ratingComboBox.setSelectedItem(5);
        ratingComboBox.setFont(UITheme.FONT_INPUT);
        ((JLabel)ratingComboBox.getRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
        ratingComboBox.setPreferredSize(new Dimension(100, 35));

        commentsArea = new JTextArea(6, 30);
        commentsArea.setLineWrap(true);
        commentsArea.setWrapStyleWord(true);
        commentsArea.setFont(UITheme.FONT_INPUT);
        commentsArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.COLOR_BORDER),
                new EmptyBorder(5,5,5,5)
        ));

        submitButton = new JButton("Submit Feedback");
        UITheme.stylePrimaryButton(submitButton);
        submitButton.setIcon(UITheme.getIcon("submit_send.svg"));
        submitButton.setEnabled(false);
    }

    private void layoutComponents() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        CardPanel contentPanel = new CardPanel(new GridBagLayout());
        contentPanel.setBorder(new EmptyBorder(20, 25, 20, 25));

        GridBagConstraints innerGbc = new GridBagConstraints();
        innerGbc.insets = new Insets(10, 8, 10, 8);
        innerGbc.anchor = GridBagConstraints.WEST;
        innerGbc.fill = GridBagConstraints.HORIZONTAL;

        innerGbc.gridx = 0; innerGbc.gridy = 0; innerGbc.gridwidth = 2; innerGbc.anchor = GridBagConstraints.CENTER;
        innerGbc.insets = new Insets(0,0,20,0);
        contentPanel.add(titleLabel, innerGbc);
        innerGbc.insets = new Insets(10, 8, 10, 8);
        innerGbc.gridwidth = 1;
        innerGbc.anchor = GridBagConstraints.EAST;

        JLabel ticketLabel = new JLabel("Ticket Number:");
        ticketLabel.setFont(UITheme.FONT_LABEL);
        innerGbc.gridx = 0; innerGbc.gridy = 1; innerGbc.weightx = 0.3;
        contentPanel.add(ticketLabel, innerGbc);

        innerGbc.gridx = 1; innerGbc.gridy = 1; innerGbc.weightx = 0.7; innerGbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(ticketNumberField, innerGbc);

        JLabel ratingLabel = new JLabel("Your Rating (5=Excellent):");
        ratingLabel.setFont(UITheme.FONT_LABEL);
        innerGbc.gridx = 0; innerGbc.gridy = 2; innerGbc.anchor = GridBagConstraints.EAST;
        contentPanel.add(ratingLabel, innerGbc);

        innerGbc.gridx = 1; innerGbc.gridy = 2; innerGbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(ratingComboBox, innerGbc);

        JLabel commentsLabel = new JLabel("Comments/Suggestions:");
        commentsLabel.setFont(UITheme.FONT_LABEL);
        innerGbc.gridx = 0; innerGbc.gridy = 3; innerGbc.anchor = GridBagConstraints.NORTHEAST;
        contentPanel.add(commentsLabel, innerGbc);

        innerGbc.gridx = 1; innerGbc.gridy = 3;
        innerGbc.fill = GridBagConstraints.BOTH;
        innerGbc.weighty = 1.0;
        innerGbc.anchor = GridBagConstraints.WEST;
        JScrollPane commentsScrollPane = new JScrollPane(commentsArea);
        commentsScrollPane.getViewport().setBackground(UITheme.COLOR_BACKGROUND_PANEL);
        contentPanel.add(commentsScrollPane, innerGbc);

        innerGbc.fill = GridBagConstraints.HORIZONTAL;
        innerGbc.weighty = 0;

        innerGbc.gridx = 0; innerGbc.gridy = 4; innerGbc.gridwidth = 2;
        innerGbc.anchor = GridBagConstraints.CENTER;
        innerGbc.fill = GridBagConstraints.NONE;
        innerGbc.insets = new Insets(20, 8, 0, 8);
        contentPanel.add(submitButton, innerGbc);

        add(contentPanel, gbc);
    }

    public void prepareForFeedback(String ticketNumber) {
        ticketNumberField.setText(ticketNumber);
        ratingComboBox.setSelectedItem(5);
        commentsArea.setText("");
        submitButton.setEnabled(ticketNumber != null && !ticketNumber.isEmpty());
        commentsArea.requestFocusInWindow();
    }

    private void attachListeners() {
        submitButton.addActionListener((ActionEvent e) -> {
            String ticketNum = ticketNumberField.getText();
            if (ticketNum.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No ticket number specified for feedback.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Integer ratingObj = (Integer) ratingComboBox.getSelectedItem();
            if (ratingObj == null) {
                JOptionPane.showMessageDialog(this, "Please select a rating.", "Rating Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int rating = ratingObj;
            String comments = commentsArea.getText().trim();

            Feedback feedback = new Feedback(ticketNum, rating, comments);
            dbManager.saveFeedback(feedback);

            JOptionPane.showMessageDialog(this,
                    "<html>Thank you for your valuable feedback regarding ticket <b>" + ticketNum + "</b>!</html>",
                    "Feedback Submitted",
                    JOptionPane.INFORMATION_MESSAGE,
                    UITheme.getIcon("feedback_submitted.svg", 32, 32));

            ticketNumberField.setText("");
            commentsArea.setText("");
            ratingComboBox.setSelectedItem(5);
            submitButton.setEnabled(false);

            Container parent = getParent();
            if (parent instanceof JTabbedPane) {
                JTabbedPane parentTabs = (JTabbedPane) parent;
                for(int i=0; i < parentTabs.getTabCount(); i++) {
                    if (parentTabs.getComponentAt(i) instanceof CustomerPanel) {
                        parentTabs.setSelectedIndex(i);
                        break;
                    }
                }
            }
        });
    }
}
