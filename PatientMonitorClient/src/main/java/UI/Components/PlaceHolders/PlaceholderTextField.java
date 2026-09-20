package UI.Components.PlaceHolders;

import javax.swing.*;
import java.awt.*;

public class PlaceholderTextField extends JTextField {

    private final String placeholder;

    // Creates a text field with placeholder text.
    public PlaceholderTextField(String placeholder) {
        super();
        this.placeholder = placeholder == null ? "" : placeholder;
        setOpaque(false);
    }

    // Paints the placeholder text when the field is empty and not focused.
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (placeholder.isEmpty()) return;

        String text = super.getText();
        if (text != null && !text.isEmpty()) return;
        if (isFocusOwner()) return;

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(getFont());
            g2.setColor(new Color(156, 163, 175)); // light gray placeholder

            Insets insets = getInsets();
            FontMetrics fm = g2.getFontMetrics();
            int x = insets.left;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

            g2.drawString(placeholder, x, y);
        } finally {
            g2.dispose();
        }
    }
}
