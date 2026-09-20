package UI.Components.Tiles;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedList;
import java.util.List;

public class WaveformPanel extends JPanel {

    private final List<Double> samples = new LinkedList<>();
    private final Color baseColor;
    private Color gridColor = new Color(235, 235, 235);
    private Color axisColor = Color.GRAY;
    private boolean alarm = false;

    // axis
    private double yMin = -2;
    private double yMax = 2;
    private int secondsVisible = 10;

    // Creates a waveform panel for displaying time-series data with optional alarm highlighting.
    public WaveformPanel(String title, Color baseColor) {
        this.baseColor = baseColor;
        setPreferredSize(new Dimension(1000, 300));
        setBackground(Color.WHITE);
    }

    public void setChartColors(Color background, Color grid, Color axis) {
        setBackground(background);
        this.gridColor = grid;
        this.axisColor = axis;
        repaint();
    }

    // Appends new samples to the waveform while enforcing a maximum buffer size.
    public void addSamples(List<Double> data, int maxSamples) {
        samples.addAll(data);
        while (samples.size() > maxSamples) samples.remove(0);
        repaint();
    }

    // Clears all waveform samples from the panel.
    public void clear() {
        samples.clear();
        repaint();
    }

    // Enables or disables alarm mode for visual emphasis.
    public void setAlarm(boolean alarm) {
        this.alarm = alarm;
        repaint();
    }

    // Configures the vertical axis range and visible time window.
    public void setAxis(double yMin, double yMax, int secondsVisible) {
        this.yMin = yMin;
        this.yMax = yMax;
        this.secondsVisible = secondsVisible;
    }

    // Paints the waveform, grid, axes, and alarm indicators.
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        int left = 50;
        int bottom = 30;

        /* ===== GRID ===== */
        g2.setColor(gridColor);
        for (int x = left; x < w; x += 50)
            g2.drawLine(x, 0, x, h - bottom);
        for (int y = 0; y < h - bottom; y += 40)
            g2.drawLine(left, y, w, y);

        g2.setColor(axisColor);
        g2.drawLine(left, 0, left, h - bottom);
        g2.drawLine(left, h - bottom, w, h - bottom);

        g2.drawString(String.format("%.1f", yMax), 5, 12);
        g2.drawString("0", 15, (h - bottom) / 2);
        g2.drawString(String.format("%.1f", yMin), 5, h - bottom - 2);
        g2.drawString("0", left - 5, h - 8);
        g2.drawString(secondsVisible + " s", w - 30, h - 8);

        if (samples.size() < 2) return;

        //waveform
        g2.setColor(alarm ? Color.RED : baseColor);

        double range = yMax - yMin;

        for (int i = 1; i < samples.size(); i++) {
            int x1 = left + (i - 1) * (w - left) / samples.size();
            int x2 = left + i * (w - left) / samples.size();

            double v1 = samples.get(i - 1);
            double v2 = samples.get(i);

            int y1 = (int) ((yMax - v1) / range * (h - bottom));
            int y2 = (int) ((yMax - v2) / range * (h - bottom));

            g2.drawLine(x1, y1, x2, y2);
        }

        // alarm boarder
        if (alarm) {
            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(2));
            g2.drawRect(1, 1, w - 3, h - 3);
        }
    }
}
