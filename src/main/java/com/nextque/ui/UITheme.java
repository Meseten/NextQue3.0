// Filename: UITheme.java
package com.nextque.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.net.URL;

public class UITheme {

    // --- New Color Palette: Sophisticated Blues, Grays, and a Subtle Gold Accent ---
    public static final Color COLOR_PRIMARY_NAVY = new Color(24, 49, 83);      // Deep Navy Blue (for headers, accents)
    public static final Color COLOR_PRIMARY_STEEL_BLUE = new Color(70, 130, 180); // Steel Blue (buttons, highlights)
    public static final Color COLOR_PRIMARY_LIGHT_SKY = new Color(173, 216, 230); // Light Sky Blue (subtle backgrounds, highlights)

    public static final Color COLOR_ACCENT_GOLD = new Color(212, 175, 55);     // Muted Gold (for important highlights, icons)
    public static final Color COLOR_ACCENT_PALE_GOLD = new Color(240, 225, 170); // Pale Gold

    // --- Neutral Colors ---
    public static final Color COLOR_BACKGROUND_MAIN = new Color(237, 241, 245); // Lightest Gray-Blue (main window bg)
    public static final Color COLOR_BACKGROUND_PANEL = Color.WHITE;             // White for content panels/cards
    public static final Color COLOR_BACKGROUND_SECTION = new Color(248, 249, 250); // Very Light Gray for sections within panels

    public static final Color COLOR_TEXT_DARK = new Color(34, 47, 62);        // Dark Slate Gray for primary text
    public static final Color COLOR_TEXT_MEDIUM = new Color(84, 109, 122);      // Medium Slate Gray for secondary text
    public static final Color COLOR_TEXT_LIGHT = new Color(128, 142, 155);      // Light Slate Gray for less important text
    public static final Color COLOR_TEXT_ON_NAVY = Color.WHITE;
    public static final Color COLOR_TEXT_ON_STEEL_BLUE = Color.WHITE;
    public static final Color COLOR_TEXT_ON_GOLD = new Color(50,50,50);   // Dark text on gold for readability
    public static final Color COLOR_TEXT_ON_PRIMARY = Color.WHITE; // Initialized: For text on danger, success, info buttons etc.

    public static final Color COLOR_BORDER = new Color(205, 211, 218);        // Softer Gray Border
    public static final Color COLOR_BORDER_INPUT_FOCUS = COLOR_PRIMARY_STEEL_BLUE;

    // --- Semantic Colors (Refined) ---
    public static final Color COLOR_SUCCESS = new Color(30, 150, 75);         // Refined Green
    public static final Color COLOR_INFO = new Color(20, 130, 200);         // Refined Blue
    public static final Color COLOR_WARNING = new Color(255, 180, 0);         // Refined Yellow/Orange
    public static final Color COLOR_DANGER = new Color(200, 40, 55);          // Refined Red

    // --- Typography ---
    public static final String FONT_FAMILY_PRIMARY = "Inter"; // Consider making this public if needed elsewhere, or ensure font is bundled
    private static final String FONT_FAMILY_FALLBACK = "Arial";

    public static final Font FONT_TITLE_H1 = getFont(FONT_FAMILY_PRIMARY, Font.BOLD, 28);
    public static final Font FONT_TITLE_H2 = getFont(FONT_FAMILY_PRIMARY, Font.BOLD, 22);
    public static final Font FONT_TITLE_H3 = getFont(FONT_FAMILY_PRIMARY, Font.BOLD, 17);

    public static final Font FONT_GENERAL_BOLD = getFont(FONT_FAMILY_PRIMARY, Font.BOLD, 13);
    public static final Font FONT_GENERAL_REGULAR = getFont(FONT_FAMILY_PRIMARY, Font.PLAIN, 13);

    public static final Font FONT_LABEL = getFont(FONT_FAMILY_PRIMARY, Font.PLAIN, 13);
    public static final Font FONT_INPUT = getFont(FONT_FAMILY_PRIMARY, Font.PLAIN, 13);
    public static final Font FONT_BUTTON = getFont(FONT_FAMILY_PRIMARY, Font.BOLD, 13);

    public static final Font FONT_TABLE_HEADER = getFont(FONT_FAMILY_PRIMARY, Font.BOLD, 12);
    public static final Font FONT_TABLE_CELL = getFont(FONT_FAMILY_PRIMARY, Font.PLAIN, 12);
    public static final Font FONT_LIST_ITEM = getFont(FONT_FAMILY_PRIMARY, Font.PLAIN, 13);


    // --- Borders ---
    public static final Border BORDER_PANEL_PADDING = new EmptyBorder(20, 25, 20, 25);
    public static final Border BORDER_SECTION_PADDING = new EmptyBorder(15, 20, 15, 20);
    public static final Border BORDER_COMPONENT_PADDING = new EmptyBorder(8, 10, 8, 10);
    public static final Border BORDER_BUTTON_ROUNDED = new EmptyBorder(10, 20, 10, 20);
    static Color COLOR_PRIMARY_MEDIUM;


    // --- Icons ---
    public static Icon getIcon(String name, int width, int height) {
        String resourcePath = "/com/nextque/ui/icons/" + name;
        try {
            URL iconUrl = UITheme.class.getResource(resourcePath);
            if (iconUrl != null) {
                return new FlatSVGIcon(iconUrl).derive(width, height);
            } else {
                System.err.println("Warning: SVG Icon resource '" + resourcePath + "' not found. No icon will be displayed.");
                return null; // Return null if icon is not found
            }
        } catch (Exception e) {
            System.err.println("Error loading SVG Icon '" + name + "': " + e.getMessage());
            return null; // Return null on error as well
        }
    }
     public static Icon getIcon(String name) {
        return getIcon(name, 16, 16); // Default size
    }


    public static Font getFont(String family, int style, int size) { // Made public for wider access if needed
        Font font = new Font(family, style, size);
        // Check if the primary font is available, otherwise use fallback
        if (!isFontAvailable(family) && !family.equals(FONT_FAMILY_FALLBACK)) {
            System.out.println("Warning: Font '" + family + "' not found or not available. Using fallback font '" + FONT_FAMILY_FALLBACK + "'.");
            font = new Font(FONT_FAMILY_FALLBACK, style, size);
        }
        return font;
    }

    private static boolean isFontAvailable(String fontFamily) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontNames = ge.getAvailableFontFamilyNames();
        for (String name : fontNames) {
            if (name.equalsIgnoreCase(fontFamily)) {
                return true;
            }
        }
        return false;
    }

    public static void applyGlobalStyles() {
        // General UI Manager settings for FlatLaf
        UIManager.put("Button.arc", 12);
        UIManager.put("Component.arc", 8); // Arc for panels, text fields etc.
        UIManager.put("TextComponent.arc", 8); // Specifically for text components
        UIManager.put("Table.striped", true);
        UIManager.put("Table.alternateRowColor", COLOR_BACKGROUND_SECTION);
        UIManager.put("Component.focusWidth", 2); // Thickness of focus border
        UIManager.put("Component.focusedBorderColor", COLOR_PRIMARY_STEEL_BLUE); // Color of focus border
        UIManager.put("Component.borderColor", COLOR_BORDER); // Default border color for components
        UIManager.put("ScrollBar.thumbArc", 999); // Makes scrollbar thumb round
        UIManager.put("ScrollBar.trackArc", 999); // Makes scrollbar track round
        UIManager.put("ScrollBar.width", 10); // Scrollbar width

        // OptionPane styling
        UIManager.put("OptionPane.messageFont", FONT_GENERAL_REGULAR);
        UIManager.put("OptionPane.buttonFont", FONT_BUTTON);

        // ToolTip styling
        UIManager.put("ToolTip.background", COLOR_TEXT_DARK);
        UIManager.put("ToolTip.foreground", Color.WHITE);
        UIManager.put("ToolTip.font", getFont(FONT_FAMILY_PRIMARY, Font.PLAIN, 12));

        // For JList selection
        UIManager.put("List.selectionBackground", UITheme.COLOR_PRIMARY_LIGHT_SKY);
        UIManager.put("List.selectionForeground", UITheme.COLOR_PRIMARY_NAVY);
    }

    public static void stylePrimaryButton(JButton button) {
        button.setFont(FONT_BUTTON);
        button.setBackground(COLOR_PRIMARY_STEEL_BLUE);
        button.setForeground(COLOR_TEXT_ON_STEEL_BLUE);
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BORDER_BUTTON_ROUNDED); // Consistent padding
    }

    public static void styleAccentButton(JButton button) {
        button.setFont(FONT_BUTTON);
        button.setBackground(COLOR_ACCENT_GOLD);
        button.setForeground(COLOR_TEXT_ON_GOLD);
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BORDER_BUTTON_ROUNDED); // Consistent padding
    }

    public static void styleSecondaryButton(JButton button) {
        button.setFont(FONT_BUTTON);
        button.setBackground(COLOR_BACKGROUND_SECTION); // Lighter background
        button.setForeground(COLOR_TEXT_DARK);          // Darker text
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        // A slightly different border for secondary, perhaps just a line border with padding
        button.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true), // Subtle border
                new EmptyBorder(9, 19, 9, 19) // Padding (adjust to match visual height of BORDER_BUTTON_ROUNDED)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void styleDangerButton(JButton button) {
        button.setFont(FONT_BUTTON);
        button.setBackground(COLOR_DANGER);
        button.setForeground(COLOR_TEXT_ON_PRIMARY); // Uses the initialized white color
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BORDER_BUTTON_ROUNDED); // Consistent padding
    }

    public static void styleSuccessButton(JButton button) {
        button.setFont(FONT_BUTTON);
        button.setBackground(COLOR_SUCCESS);
        button.setForeground(COLOR_TEXT_ON_PRIMARY); // Uses the initialized white color
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BORDER_BUTTON_ROUNDED); // Consistent padding
    }

    public static void styleInfoButton(JButton button) {
        button.setFont(FONT_BUTTON);
        button.setBackground(COLOR_INFO);
        button.setForeground(COLOR_TEXT_ON_PRIMARY); // Uses the initialized white color
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BORDER_BUTTON_ROUNDED); // Consistent padding
    }

    // --- HEX Helper Methods ---
    public static String COLOR_ACCENT_GOLD_HEX() {
        return String.format("#%02x%02x%02x", COLOR_ACCENT_GOLD.getRed(), COLOR_ACCENT_GOLD.getGreen(), COLOR_ACCENT_GOLD.getBlue());
    }

    public static String COLOR_PRIMARY_NAVY_HEX() {
        return String.format("#%02x%02x%02x", COLOR_PRIMARY_NAVY.getRed(), COLOR_PRIMARY_NAVY.getGreen(), COLOR_PRIMARY_NAVY.getBlue());
    }

    public static String COLOR_PRIMARY_STEEL_BLUE_HEX() {
        return String.format("#%02x%02x%02x", COLOR_PRIMARY_STEEL_BLUE.getRed(), COLOR_PRIMARY_STEEL_BLUE.getGreen(), COLOR_PRIMARY_STEEL_BLUE.getBlue());
    }

    public static String COLOR_DANGER_HEX() {
        return String.format("#%02x%02x%02x", COLOR_DANGER.getRed(), COLOR_DANGER.getGreen(), COLOR_DANGER.getBlue());
    }

    public static String COLOR_TEXT_MEDIUM_HEX() {
        return String.format("#%02x%02x%02x", COLOR_TEXT_MEDIUM.getRed(), COLOR_TEXT_MEDIUM.getGreen(), COLOR_TEXT_MEDIUM.getBlue());
    }

    public static String COLOR_TEXT_LIGHT_HEX() {
        return String.format("#%02x%02x%02x", COLOR_TEXT_LIGHT.getRed(), COLOR_TEXT_LIGHT.getGreen(), COLOR_TEXT_LIGHT.getBlue());
    }
}
