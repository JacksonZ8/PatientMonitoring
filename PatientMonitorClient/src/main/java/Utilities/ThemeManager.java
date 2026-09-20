package Utilities;

import UI.Components.Tiles.BaseTile;
import UI.Components.TopBar;
import UI.Components.RoundedButton;
import UI.Components.RoundedPanel;

import javax.swing.*;
import java.awt.*;

// Applies light / dark theme to the whole UI
public final class ThemeManager {

    // Utility class (no instances)
    private ThemeManager() {}

    // Top bar colors
    private static final Color TOPBAR_BG_LIGHT = new Color(63, 90, 227);
    private static final Color TOPBAR_BG_DARK  = new Color(32, 48, 140);

    // Top bar text is always white
    private static final Color TOPBAR_TEXT = Color.WHITE;

    // Apply theme and refresh UI
    public static void apply(JFrame frame, boolean darkMode) {
        Color appBg = darkMode ? new Color(24, 26, 30) : Color.WHITE;
        Color fg    = darkMode ? new Color(230, 230, 230) : Color.BLACK;
        Color card  = darkMode ? new Color(36, 36, 42) : Color.WHITE;

        // Default Swing component colors
        UIManager.put("Panel.background", appBg);
        UIManager.put("Label.foreground", fg);
        UIManager.put("Button.foreground", fg);
        UIManager.put("CheckBox.foreground", fg);
        UIManager.put("CheckBox.background", appBg);
        UIManager.put("ComboBox.foreground", fg);
        UIManager.put("ComboBox.background", darkMode ? new Color(45, 48, 56) : Color.WHITE);
        UIManager.put("TextField.foreground", fg);
        UIManager.put("TextField.background", darkMode ? new Color(45, 48, 56) : Color.WHITE);

        // Apply colors to custom components
        updateTree(frame.getContentPane(), darkMode, appBg, fg, card);

        frame.revalidate();
        frame.repaint();
    }

    // Recursively apply colors to all components
    private static void updateTree(Component c, boolean darkMode, Color appBg, Color fg, Color card) {

        // Default text color
        if (c instanceof JComponent jc) {
            jc.setForeground(fg);
        }

        // Background colors
        if (c instanceof BaseTile bt) {
            bt.setBackground(card);
        } else if (c instanceof JPanel p) {
            p.setBackground(appBg);
        }

        // Rounded panels
        if (c instanceof RoundedPanel rp) {
            rp.setFillColor(card);
        }

        // Rounded buttons
        if (c instanceof RoundedButton rb) {
            if (darkMode) {
                rb.setColors(
                        new Color(90, 140, 255),
                        new Color(120, 165, 255),
                        new Color(70, 120, 230),
                        Color.WHITE
                );
            } else {
                rb.setColors(
                        new Color(70, 130, 250),
                        new Color(100, 150, 255),
                        new Color(50, 110, 220),
                        Color.WHITE
                );
            }
        }

        // Text fields
        if (c instanceof JTextField tf) {
            tf.setOpaque(false);
            tf.setForeground(darkMode ? new Color(230, 230, 230) : Color.BLACK);
            tf.setCaretColor(darkMode ? new Color(230, 230, 230) : Color.BLACK);
            tf.setBackground(darkMode ? new Color(70, 70, 78) : new Color(245, 245, 245));
        }

        // Visit child components
        if (c instanceof Container container) {
            for (Component child : container.getComponents()) {
                updateTree(child, darkMode, appBg, fg, card);
            }
        }

        // Force top bar colors
        if (c instanceof TopBar tb) {
            tb.setOpaque(true);

            Color bg = darkMode ? TOPBAR_BG_DARK : TOPBAR_BG_LIGHT;
            tb.setBackground(bg);

            // Force ALL nested labels/buttons inside the topbar to white
            setForegroundDeep(tb, TOPBAR_TEXT);
        }
    }

    // Set text color for all nested components
    private static void setForegroundDeep(Container root, Color color) {
        for (Component comp : root.getComponents()) {

            if (comp instanceof JComponent jc) {
                jc.setForeground(color);
            }
            if (comp instanceof AbstractButton b) {
                b.setForeground(color);
            }

            if (comp instanceof Container child) {
                setForegroundDeep(child, color);
            }
        }
    }
}
