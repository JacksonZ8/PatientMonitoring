package Utilities;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;

public final class SwingStyle {
    public static final Color DARK_FIELD = new Color(45, 48, 56);
    public static final Color DARK_BORDER = new Color(72, 79, 92);
    public static final Color DARK_TEXT = new Color(235, 235, 235);
    public static final Color LIGHT_BORDER = new Color(210, 214, 220);

    private SwingStyle() {}

    public static void styleComboBox(JComboBox<?> combo, boolean dark) {
        Color bg = dark ? DARK_FIELD : Color.WHITE;
        Color fg = dark ? DARK_TEXT : Color.BLACK;
        Color selectedBg = dark ? new Color(62, 67, 78) : new Color(239, 246, 255);
        Color border = dark ? DARK_BORDER : LIGHT_BORDER;

        combo.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton button = new JButton("▾");
                button.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
                button.setForeground(fg);
                button.setBackground(bg);
                button.setOpaque(true);
                button.setContentAreaFilled(true);
                button.setFocusPainted(false);
                return button;
            }
        });
        combo.setOpaque(true);
        combo.setBackground(bg);
        combo.setForeground(fg);
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border),
                BorderFactory.createEmptyBorder(2, 8, 2, 4)
        ));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setForeground(fg);
                label.setBackground(isSelected ? selectedBg : bg);
                return label;
            }
        });
    }

    public static void styleTextField(JTextField field, boolean dark) {
        Color bg = dark ? DARK_FIELD : Color.WHITE;
        Color fg = dark ? DARK_TEXT : Color.BLACK;
        field.setOpaque(true);
        field.setForeground(fg);
        field.setCaretColor(fg);
        field.setBackground(bg);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(dark ? DARK_BORDER : LIGHT_BORDER),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
    }

    public static void styleTextArea(JTextArea area, boolean dark) {
        Color bg = dark ? DARK_FIELD : Color.WHITE;
        Color fg = dark ? DARK_TEXT : Color.BLACK;
        area.setOpaque(true);
        area.setForeground(fg);
        area.setCaretColor(fg);
        area.setBackground(bg);
        area.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(dark ? DARK_BORDER : LIGHT_BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
    }
}
