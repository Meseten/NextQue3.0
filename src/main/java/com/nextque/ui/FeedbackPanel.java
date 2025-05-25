// Filename: FeedbackPanel.java
package com.nextque.ui;

import com.nextque.db.DatabaseManager;
import com.nextque.model.Feedback;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
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
        setBorder(UITheme.BORDER_PANEL_PADDING); // Outer padding for the whole panel

        initComponents();
        layoutComponents();
        attachListeners();
    }

    private void initComponents() {
        titleLabel = new JLabel("Share Your Feedback", SwingConstants.CENTER);
        titleLabel.setFont(UITheme.FONT_TITLE_H1);
        titleLabel.setForeground(UITheme.COLOR_PRIMARY_NAVY); // Changed from COLOR_PRIMARY_DARK
        titleLabel.setIcon(UITheme.getIcon("feedback_chat.svg", 32, 32)); // Ensure icon exists
        titleLabel.setIconTextGap(10);

        ticketNumberField = new JTextField(18);
        ticketNumberField.setEditable(false);
        ticketNumberField.setFont(UITheme.FONT_INPUT);
        ticketNumberField.setBackground(UITheme.COLOR_BACKGROUND_SECTION); // Changed from COLOR_BACKGROUND_DARK
        ticketNumberField.setPreferredSize(new Dimension(0, 35)); // Height, width by layout

        Integer[] ratings = {1, 2, 3, 4, 5};
        ratingComboBox = new JComboBox<>(ratings);
        ratingComboBox.setSelectedItem(5);
        ratingComboBox.setFont(UITheme.FONT_INPUT);
        ((JLabel)ratingComboBox.getRenderer()).setHorizontalAlignment(SwingConstants.CENTER); // Center items
        ratingComboBox.setPreferredSize(new Dimension(100, 35));


        commentsArea = new JTextArea(6, 30);
        commentsArea.setLineWrap(true);
        commentsArea.setWrapStyleWord(true);
        commentsArea.setFont(UITheme.FONT_INPUT);
        commentsArea.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER),
                new EmptyBorder(5,5,5,5) // Padding inside text area
        ));

        submitButton = new JButton("Submit Feedback");
        UITheme.stylePrimaryButton(submitButton);
        submitButton.setIcon(UITheme.getIcon("submit_send.svg")); // Ensure icon exists
        submitButton.setEnabled(false); // Enabled when ticket number is set
    }

    private void layoutComponents() {
        setLayout(new GridBagLayout()); // Main panel uses GridBagLayout to center the card
        GridBagConstraints gbc = new GridBagConstraints();

        // Main content panel with a card-like feel
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBackground(UITheme.COLOR_BACKGROUND_PANEL);
        contentPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UITheme.COLOR_BORDER, 1, true), // Card border
                new EmptyBorder(20, 25, 20, 25) // Padding inside the card
        ));
        contentPanel.putClientProperty(com.formdev.flatlaf.FlatClientProperties.STYLE, "arc: 12"); // Rounded corners for the card

        GridBagConstraints innerGbc = new GridBagConstraints();
        innerGbc.insets = new Insets(10, 8, 10, 8); // Default insets for items within the card
        innerGbc.anchor = GridBagConstraints.WEST;
        innerGbc.fill = GridBagConstraints.HORIZONTAL;

        // Title inside the card
        innerGbc.gridx = 0; innerGbc.gridy = 0; innerGbc.gridwidth = 2; innerGbc.anchor = GridBagConstraints.CENTER;
        innerGbc.insets = new Insets(0,0,20,0); // Bottom margin for title
        contentPanel.add(titleLabel, innerGbc);
        innerGbc.insets = new Insets(10, 8, 10, 8); // Reset insets
        innerGbc.gridwidth = 1; // Reset gridwidth
        innerGbc.anchor = GridBagConstraints.EAST; // Align labels to the right by default

        // Ticket Number
        JLabel ticketLabel = new JLabel("Ticket Number:");
        ticketLabel.setFont(UITheme.FONT_LABEL);
        innerGbc.gridx = 0; innerGbc.gridy = 1; innerGbc.weightx = 0.3; // Label column weight
        contentPanel.add(ticketLabel, innerGbc);

        innerGbc.gridx = 1; innerGbc.gridy = 1; innerGbc.weightx = 0.7; innerGbc.anchor = GridBagConstraints.WEST; // Field column weight
        contentPanel.add(ticketNumberField, innerGbc);

        // Rating
        JLabel ratingLabel = new JLabel("Your Rating (5=Excellent):");
        ratingLabel.setFont(UITheme.FONT_LABEL);
        innerGbc.gridx = 0; innerGbc.gridy = 2; innerGbc.anchor = GridBagConstraints.EAST;
        contentPanel.add(ratingLabel, innerGbc);

        innerGbc.gridx = 1; innerGbc.gridy = 2; innerGbc.anchor = GridBagConstraints.WEST;
        contentPanel.add(ratingComboBox, innerGbc);

        // Comments
        JLabel commentsLabel = new JLabel("Comments/Suggestions:");
        commentsLabel.setFont(UITheme.FONT_LABEL);
        innerGbc.gridx = 0; innerGbc.gridy = 3; innerGbc.anchor = GridBagConstraints.NORTHEAST; // Align to top-right of its cell
        contentPanel.add(commentsLabel, innerGbc);

        innerGbc.gridx = 1; innerGbc.gridy = 3;
        innerGbc.fill = GridBagConstraints.BOTH; // Allow comments area to expand
        innerGbc.weighty = 1.0; // Allow comments area to grow vertically
        innerGbc.anchor = GridBagConstraints.WEST;
        JScrollPane commentsScrollPane = new JScrollPane(commentsArea);
        commentsScrollPane.getViewport().setBackground(UITheme.COLOR_BACKGROUND_PANEL); // Match text area bg
        contentPanel.add(commentsScrollPane, innerGbc);

        innerGbc.fill = GridBagConstraints.HORIZONTAL; // Reset fill for button
        innerGbc.weighty = 0; // Reset weighty

        // Submit Button
        innerGbc.gridx = 0; innerGbc.gridy = 4; innerGbc.gridwidth = 2;
        innerGbc.anchor = GridBagConstraints.CENTER;
        innerGbc.fill = GridBagConstraints.NONE; // Button should not expand horizontally
        innerGbc.insets = new Insets(20, 8, 0, 8); // Top margin for button
        contentPanel.add(submitButton, innerGbc);

        // Add the contentPanel to the main FeedbackPanel (this)
        // This setup centers the contentPanel (card) within the FeedbackPanel.
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1.0; // Allow centering horizontally
        gbc.weighty = 1.0; // Allow centering vertically
        gbc.fill = GridBagConstraints.NONE; // Don't expand card beyond preferred size
        gbc.anchor = GridBagConstraints.CENTER;
        add(contentPanel, gbc);
    }

    public void prepareForFeedback(String ticketNumber) {
        ticketNumberField.setText(ticketNumber);
        ratingComboBox.setSelectedItem(5); // Default to highest rating
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
            if (ratingObj == null) { // Should not happen with Integer array but good practice
                JOptionPane.showMessageDialog(this, "Please select a rating.", "Rating Required", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int rating = ratingObj;
            String comments = commentsArea.getText().trim(); // Trim comments

            Feedback feedback = new Feedback(ticketNum, rating, comments);
            dbManager.saveFeedback(feedback);

            JOptionPane.showMessageDialog(this,
                    "<html>Thank you for your valuable feedback regarding ticket <b>" + ticketNum + "</b>!</html>",
                    "Feedback Submitted",
                    JOptionPane.INFORMATION_MESSAGE,
                    UITheme.getIcon("feedback_submitted.svg", 32, 32)); // Ensure icon exists

            // Reset fields
            ticketNumberField.setText("");
            commentsArea.setText("");
            ratingComboBox.setSelectedItem(5);
            submitButton.setEnabled(false);

            // Try to switch to customer kiosk or a neutral tab
            // This helps navigate the user away from the feedback form after submission.
            Container parent = getParent();
            while (parent != null && !(parent instanceof JTabbedPane)) {
                parent = parent.getParent(); // Traverse up to find JTabbedPane
            }

            if (parent instanceof JTabbedPane) {
                JTabbedPane parentTabs = (JTabbedPane) parent;
                int customerPanelIndex = -1;
                int displayPanelIndex = -1;

                for(int i=0; i < parentTabs.getTabCount(); i++) {
                    Component tabComponent = parentTabs.getComponentAt(i);
                    // Check the name of the component or its class.
                    // Assuming CustomerPanel and DisplayPanel are direct children of tabs.
                    if (tabComponent instanceof CustomerPanel) {
                        customerPanelIndex = i;
                        break; // Prefer CustomerPanel
                    } else if (tabComponent instanceof DisplayPanel) {
                        displayPanelIndex = i;
                    }
                }
                if (customerPanelIndex != -1) {
                    parentTabs.setSelectedIndex(customerPanelIndex);
                } else if (displayPanelIndex != -1) {
                    parentTabs.setSelectedIndex(displayPanelIndex); // Fallback to DisplayPanel
                }
            }
        });
    }
}
