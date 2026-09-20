package UI.Components.PlaceHolders;

import javax.swing.*;
import java.awt.*;

// when entering password, it is not visible
public class PlaceholderPasswordField extends JPasswordField {
    private String placeholder;

    // Creates a password field with placeholder text and masked input
    public PlaceholderPasswordField(String placeholder) {
        this.placeholder = placeholder;
        setOpaque(false);
        setEchoChar('*');
    }

    // Paints the placeholder text when the field is empty and not focused.
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (getPassword().length == 0 && !isFocusOwner()) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(Color.GRAY);
            g2.setFont(getFont().deriveFont(Font.PLAIN));

            int x = getInsets().left + 8;    // same padding fix
            int y = getHeight() / 2 + getFont().getSize() / 2 - 3;

            g2.drawString(placeholder, x, y);
            g2.dispose();
        }
    }
}
