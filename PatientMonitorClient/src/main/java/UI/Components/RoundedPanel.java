package UI.Components;

import javax.swing.*;
import java.awt.*;

public class RoundedPanel extends JPanel {
    private int radius = 20;
    private Color fillColor = new Color(245, 245, 245);

    // Creates a panel with rounded corners using the specified layout manager.
    public RoundedPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);
    }

    // Sets the background fill colour of the rounded panel.
    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
        repaint();
    }

    // Paints the rounded panel background using the current fill colour.
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fillColor);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);

    }
}
