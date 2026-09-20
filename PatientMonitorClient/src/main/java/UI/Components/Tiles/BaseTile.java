package UI.Components.Tiles;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// Base tile component providing rounded corners, shadow, and hover effects.
public class BaseTile extends JPanel {

    private int radius;
    private int shadowSize = 12;
    private Color tileColor = Color.WHITE;
    private Color baseColor = Color.WHITE;
    private boolean hasHoverEffect;
    private boolean backgroundLocked = false;

    // Creates a base tile with specified size, corner radius, and optional hover effect.
    public BaseTile(int width, int height, int radius, boolean hasHoverEffect) {
        this.radius = radius;
        setOpaque(false);
        setPreferredSize(new Dimension(width, height));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        if (hasHoverEffect) {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (isLight(baseColor)) {
                        tileColor = new Color(240, 240, 240);   // keep your light mode behaviour
                    } else {
                        tileColor = lighten(baseColor, 8);       // subtle lift in dark mode
                    }
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    tileColor = baseColor;
                    repaint();
                }
            });
        }
    }
    // Determines whether a colour is considered light based on average brightness.
    private boolean isLight(Color c) {
        // simple brightness check
        int brightness = (c.getRed() + c.getGreen() + c.getBlue()) / 3;
        return brightness >= 200;
    }
    // Returns a lighter version of the given colour by the specified amount.
    private Color lighten(Color c, int amount) {
        int r = Math.min(255, c.getRed() + amount);
        int g = Math.min(255, c.getGreen() + amount);
        int b = Math.min(255, c.getBlue() + amount);
        return new Color(r, g, b);
    }
    // Paints the tile background with a soft shadow and rounded corners.
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // blurry shadow
        for (int i = 0; i < shadowSize; i++) {
            int alpha = (int) (1 - i * (1 / shadowSize));
            if (alpha < 0) alpha = 0;

            g2.setColor(new Color(0, 0, 0, alpha));
            g2.fillRoundRect(
                    i,
                    i,
                    w - i * 2,
                    h - i * 2,
                    radius + i,
                    radius + i
            );
        }

        // main background
        g2.setColor(tileColor);
        g2.fillRoundRect(
                shadowSize,
                shadowSize,
                w - shadowSize * 2,
                h - shadowSize * 2,
                radius,
                radius
        );

        g2.dispose();
        super.paintComponent(g);
    }

    // Sets the background colour of the tile unless background changes are locked.
    @Override
    public void setBackground(Color bg) {
        if (backgroundLocked) return;
        this.baseColor = bg;
        this.tileColor = bg;
        repaint();
    }
    // Enables or disables locking of background colour updates.
    public void lockBackground(boolean locked) {
        this.backgroundLocked = locked;
    }

    // Forces the tile background colour regardless of lock state.
    public void forceBackground(Color bg) {
        this.baseColor = bg;
        this.tileColor = bg;
        repaint();
    }
}
