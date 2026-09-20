package UI.Components;

import javax.swing.*;
import java.awt.*;

// for setting image to background
public class ImagePanel extends JPanel {
    private Image image;

    // Creates a panel that displays the given image as its background.
    public ImagePanel(Image img) {
        this.image = img;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    // Paints the background image scaled to the panel size.
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image != null) {
            g.drawImage(image, 0, 0, getWidth(), getHeight(), this);
        }
    }
}
