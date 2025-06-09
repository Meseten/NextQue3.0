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

    public static final Color COLOR_PRIMARY_NAVY = new Color(24, 49, 83);
    public static final Color COLOR_PRIMARY_STEEL_BLUE = new Color(70, 130, 180);
    public static final Color COLOR_PRIMARY_LIGHT_SKY = new Color(173, 216, 230);
    public static final Color COLOR_ACCENT_GOLD = new Color(212, 175, 55);
    public static final Color COLOR_BACKGROUND_MAIN = new Color(237, 241, 245);
    public static final Color COLOR_BACKGROUND_PANEL = Color.WHITE;
    public static final Color COLOR_BACKGROUND_SECTION = new Color(248, 249, 250);
    public static final Color COLOR_TEXT_DARK = new Color(34, 47, 62);
    public static final Color COLOR_TEXT_MEDIUM = new Color(84, 109, 122);
    public static final Color COLOR_TEXT_LIGHT = new Color(128, 142, 155);
    public static final Color COLOR_TEXT_ON_PRIMARY = Color.WHITE;
    public static final Color COLOR_BORDER = new Color(205, 211, 218);
    public static final Color COLOR_SUCCESS = new Color(30, 150, 75);
    public static final Color COLOR_INFO = new Color(20, 130, 200);
    public static final Color COLOR_DANGER = new Color(200, 40, 55);

    public static final String FONT_FAMILY_PRIMARY = "Inter";
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

    public static final Border BORDER_PANEL_PADDING = new EmptyBorder(20, 25, 20, 25);
    public static final Border BORDER_SECTION_PADDING = new EmptyBorder(15, 20, 15, 20);
    public static final Border BORDER_BUTTON_ROUNDED = new EmptyBorder(10, 20, 10, 20);

    public static Icon getIcon(String name, int width, int height) {
        String resourcePath = "/com/nextque/ui/icons/" + name;
        try {
            URL iconUrl = UITheme.class.getResource(resourcePath);
            if (iconUrl != null) {
                return new FlatSVGIcon(iconUrl).derive(width, height);
            } else {
                System.err.println("Warning: SVG Icon resource '" + resourcePath + "' not found.");
                return null;
            }
        } catch (Exception e) {
            System.err.println("Error loading SVG Icon '" + name + "': " + e.getMessage());
            return null;
        }
    }
     public static Icon getIcon(String name) {
        return getIcon(name, 16, 16);
    }

    public static Font getFont(String family, int style, int size) {
        if (!isFontAvailable(family) && !family.equals(FONT_FAMILY_FALLBACK)) {
            return new Font(FONT_FAMILY_FALLBACK, style, size);
        }
        return new Font(family, style, size);
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
        UIManager.put("Button.arc", 12);
        UIManager.put("Component.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("Table.striped", true);
        UIManager.put("Table.alternateRowColor", COLOR_BACKGROUND_SECTION);
        UIManager.put("Component.focusWidth", 2);
        UIManager.put("Component.focusedBorderColor", COLOR_PRIMARY_STEEL_BLUE);
        UIManager.put("Component.borderColor", COLOR_BORDER);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.trackArc", 999);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ToolTip.background", COLOR_TEXT_DARK);
        UIManager.put("ToolTip.foreground", Color.WHITE);
        UIManager.put("ToolTip.font", getFont(FONT_FAMILY_PRIMARY, Font.PLAIN, 12));
        UIManager.put("List.selectionBackground", UITheme.COLOR_PRIMARY_LIGHT_SKY);
        UIManager.put("List.selectionForeground", UITheme.COLOR_PRIMARY_NAVY);
        UIManager.put("OptionPane.messageFont", FONT_GENERAL_REGULAR);
        UIManager.put("OptionPane.buttonFont", FONT_BUTTON);
    }

    private static void styleButton(JButton button, Color background, Color foreground) {
        button.setFont(FONT_BUTTON);
        button.setBackground(background);
        button.setForeground(foreground);
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BORDER_BUTTON_ROUNDED);
    }

    public static void stylePrimaryButton(JButton button) {
        styleButton(button, COLOR_PRIMARY_STEEL_BLUE, COLOR_TEXT_ON_PRIMARY);
    }

    public static void styleSecondaryButton(JButton button) {
        button.setFont(FONT_BUTTON);
        button.setBackground(COLOR_BACKGROUND_SECTION);
        button.setForeground(COLOR_TEXT_DARK);
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        button.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(9, 19, 9, 19)
        ));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public static void styleDangerButton(JButton button) {
        styleButton(button, COLOR_DANGER, COLOR_TEXT_ON_PRIMARY);
    }

    public static void styleSuccessButton(JButton button) {
        styleButton(button, COLOR_SUCCESS, COLOR_TEXT_ON_PRIMARY);
    }

    public static void styleInfoButton(JButton button) {
        styleButton(button, COLOR_INFO, COLOR_TEXT_ON_PRIMARY);
    }

    private static String toHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    public static String COLOR_ACCENT_GOLD_HEX() { return toHex(COLOR_ACCENT_GOLD); }
    public static String COLOR_PRIMARY_NAVY_HEX() { return toHex(COLOR_PRIMARY_NAVY); }
    public static String COLOR_PRIMARY_STEEL_BLUE_HEX() { return toHex(COLOR_PRIMARY_STEEL_BLUE); }
    public static String COLOR_DANGER_HEX() { return toHex(COLOR_DANGER); }
    public static String COLOR_TEXT_MEDIUM_HEX() { return toHex(COLOR_TEXT_MEDIUM); }
    public static String COLOR_TEXT_LIGHT_HEX() { return toHex(COLOR_TEXT_LIGHT); }
}
