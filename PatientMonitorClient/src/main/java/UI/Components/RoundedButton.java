package UI.Components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class RoundedButton extends JButton {

    private int radius = 20;
    private Color normalColor = new Color(70, 130, 250);
    private Color hoverColor = new Color(100, 150, 255);
    private Color pressColor = new Color(50, 110, 220);
    private Color textColor = Color.WHITE;

    private float animProgress = 0f;   // used for hover animation
    private Timer hoverTimer;
    private boolean hovering = false;

    // Creates a rounded button with hover and press animations.
    public RoundedButton(String text) {
        super(text);
        setFont(new Font("Arial", Font.BOLD, 14));
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Hover Animation
        hoverTimer = new Timer(15, e -> {
            if (hovering && animProgress < 1f) {
                animProgress += 0.1f;
            } else if (!hovering && animProgress > 0f) {
                animProgress -= 0.1f;
            }
            repaint();
        });

        hoverTimer.start();

        // Hover Detection
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovering = true;
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovering = false;
            }

            @Override
            public void mousePressed(MouseEvent e) {
                setBackground(pressColor);
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                repaint();
            }
        });
    }

    // Sets the corner radius of the rounded button.
    public void setRadius(int radius) {
        this.radius = radius;
        repaint();
    }

    // Sets the button colours for normal, hover, and pressed states.
    public void setButtonColor(Color normal, Color hover, Color press) {
        this.normalColor = normal;
        this.hoverColor = hover;
        this.pressColor = press;
        repaint();
    }

    // Sets the colour of the button text.
    public void setTextColor(Color c) {
        this.textColor = c;
        repaint();
    }

    // Paints the rounded button background and centered text with animation.
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // Interpolate between normal & hover color
        Color mix = blend(normalColor, hoverColor, animProgress);
        g2.setColor(mix);

        // Draw rounded background
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);

        // Draw text
        g2.setFont(getFont());
        g2.setColor(textColor);

        FontMetrics fm = g2.getFontMetrics();
        int textX = (getWidth() - fm.stringWidth(getText())) / 2;
        int textY = (getHeight() + fm.getAscent()) / 2 - 2;

        g2.drawString(getText(), textX, textY);

        g2.dispose();
    }

    // Returns a preferred size based on the button text and padding.
    @Override
    public Dimension getPreferredSize() {
        // Auto-size based on text + padding
        FontMetrics fm = getFontMetrics(getFont());
        int w = fm.stringWidth(getText()) + 30;
        int h = fm.getHeight() + 12;
        return new Dimension(w, h);
    }

    // Blends two colours together based on the given ratio.
    private Color blend(Color c1, Color c2, float ratio) {
        ratio = Math.min(1f, Math.max(0f, ratio));
        int r = (int) (c1.getRed() * (1 - ratio) + c2.getRed() * ratio);
        int g = (int) (c1.getGreen() * (1 - ratio) + c2.getGreen() * ratio);
        int b = (int) (c1.getBlue() * (1 - ratio) + c2.getBlue() * ratio);
        return new Color(r, g, b);
    }

    // Sets all button colours including text colour in one call.
    public void setColors(Color normal, Color hover, Color press, Color text) {
        this.normalColor = normal;
        this.hoverColor = hover;
        this.pressColor = press;
        this.textColor = text;
        repaint();
    }

}