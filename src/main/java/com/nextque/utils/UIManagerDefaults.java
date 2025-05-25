// Filename: UIManagerDefaults.java
package com.nextque.utils;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.awt.Font;
import java.util.Enumeration;
import javax.swing.plaf.FontUIResource;

/**
 * Utility class to set UI defaults, like Look and Feel and fonts.
 */
public class UIManagerDefaults {

    public static void setLookAndFeel() {
        try {
            // Attempt to set Nimbus Look and Feel for a more modern appearance
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            // If Nimbus is not available, the default L&F will be used.
            System.err.println("Nimbus Look and Feel not found, using default. " + e.getMessage());
            try {
                 // Fallback to system L&F if Nimbus fails
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                System.err.println("Failed to set system Look and Feel: " + ex.getMessage());
            }
        }
    }

    public static void setDefaultFont(Font font) {
        FontUIResource fontResource = new FontUIResource(font);
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                UIManager.put(key, fontResource);
            }
        }
    }
}
