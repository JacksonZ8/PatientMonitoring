package Services;

import Models.Patients.Patient;
import Models.Vitals.VitalRecord;
import Models.Vitals.VitalRecordIO;
import Utilities.LanguageManager;

import java.awt.*;
import java.awt.print.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;


// Service that prints a one-page vitals summary report
public class VitalsPdfReportService {

    // Export/print a vitals summary report for the last N hours
    public static void exportPdfForLastHours(Patient patient, int hours) {

        // Create print job
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName(LanguageManager.t("pdf.jobName"));

        // Define how the page is rendered
        job.setPrintable((g, pf, pageIndex) -> {

            // Only one page supported
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;

            // Prepare 2D graphics and move origin into printable area
            Graphics2D g2 = (Graphics2D) g;
            g2.translate(pf.getImageableX(), pf.getImageableY());

            // Enable anti-aliasing for cleaner text/shapes
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            // Auto-scale content to fit the printable height

            double pageHeight = pf.getImageableHeight();
            double contentHeightEstimate = 1100;

            double scale = Math.min(1.0, pageHeight / contentHeightEstimate);
            g2.scale(scale, scale);

            // Compute scaled page width and initialise cursor position
            int pageWidth = (int) (pf.getImageableWidth() / scale);
            int y = 40;

            // Theme colors
            Color textPrimary = new Color(30, 41, 59);
            Color textMuted = new Color(100, 116, 139);
            Color blue = new Color(37, 99, 235);
            Color border = new Color(220, 225, 230);
            Color bgCard = new Color(248, 250, 252);

            // Draw title
            g2.setFont(new Font("Arial", Font.BOLD, 36));
            g2.setColor(textPrimary);
            g2.drawString(LanguageManager.t("pdf.summaryReport"), 0, y);
            y += 48;

            // Draw metadata
            g2.setFont(new Font("Arial", Font.PLAIN, 14));
            g2.setColor(textMuted);

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            g2.drawString(LanguageManager.t("pdf.generated") + ": " + LocalDateTime.now().format(fmt), 0, y);
            y += 20;
            g2.drawString(LanguageManager.t("pdf.rangeLast") + " " + hours + " " + LanguageManager.t("live.hours"), 0, y);
            y += 36;

            // Draw overview card
            int overviewHeight = 140;
            drawCard(g2, 0, y, pageWidth, overviewHeight, bgCard, border);

            g2.setFont(new Font("Arial", Font.BOLD, 22));
            g2.setColor(blue);
            g2.drawString(LanguageManager.t("pdf.overview"), 24, y + 38);

            g2.setFont(new Font("Arial", Font.PLAIN, 16));
            g2.setColor(textPrimary);

            drawWrappedText(
                    g2,
                    LanguageManager.t("pdf.overviewText"),
                    24,
                    y + 78,
                    pageWidth - 48,
                    22
            );

            y += overviewHeight + 40;

            // Load vitals records for the requested time window
            List<VitalRecord> records =
                    VitalRecordIO.loadLastHours(patient.getId(), hours);

            // Handle no-data case
            if (records.isEmpty()) {
                g2.drawString(LanguageManager.t("pdf.noData"), 0, y);
                return Printable.PAGE_EXISTS;
            }

            // Compute heart rate stats
            double hrMean = records.stream().mapToDouble(VitalRecord::getAvgHeartRate).average().orElse(0);
            double hrMin = records.stream().mapToDouble(VitalRecord::getAvgHeartRate).min().orElse(0);
            double hrMax = records.stream().mapToDouble(VitalRecord::getAvgHeartRate).max().orElse(0);

            // Compute respiratory rate stats
            double rrMean = records.stream().mapToDouble(VitalRecord::getAvgRespRate).average().orElse(0);
            double rrMin = records.stream().mapToDouble(VitalRecord::getAvgRespRate).min().orElse(0);
            double rrMax = records.stream().mapToDouble(VitalRecord::getAvgRespRate).max().orElse(0);

            // Compute temperature stats
            double tMean = records.stream().mapToDouble(VitalRecord::getAvgTemperature).average().orElse(0);
            double tMin = records.stream().mapToDouble(VitalRecord::getAvgTemperature).min().orElse(0);
            double tMax = records.stream().mapToDouble(VitalRecord::getAvgTemperature).max().orElse(0);

            // Compute SpO2 stats
            double sMean = records.stream().mapToDouble(VitalRecord::getAvgSpO2).average().orElse(0);
            double sMin = records.stream().mapToDouble(VitalRecord::getAvgSpO2).min().orElse(0);
            double sMax = records.stream().mapToDouble(VitalRecord::getAvgSpO2).max().orElse(0);

            // Draw patient vitals summary card
            int vitalsHeight = 600;
            drawCard(g2, 0, y, pageWidth, vitalsHeight, bgCard, border);

            g2.setFont(new Font("Arial", Font.BOLD, 22));
            g2.setColor(textPrimary);
            g2.drawString(LanguageManager.t("pdf.vitalSummary"), 24, y + 38);

            // Draw patient name
            g2.setFont(new Font("Arial", Font.BOLD, 17));
            g2.drawString(LanguageManager.t("pdf.patient") + ": " + patient.getName(), 24, y + 72);

            // Draw each vital block
            int py = y + 118;

            py = drawVitalBlock(g2, LanguageManager.t("live.heartRate"), "bpm", hrMean, hrMin, hrMax, 24, py, textPrimary, textMuted);
            py = drawVitalBlock(g2, LanguageManager.t("pdf.respiratoryRate"), "/min", rrMean, rrMin, rrMax, 24, py, textPrimary, textMuted);
            py = drawVitalBlock(g2, LanguageManager.t("pdf.temperature"), "°C", tMean, tMin, tMax, 24, py, textPrimary, textMuted);
            py = drawVitalBlock(g2, LanguageManager.t("live.spo2"), "%", sMean, sMin, sMax, 24, py, textPrimary, textMuted);

            y += vitalsHeight + 36;

            // Draw footer
            g2.setFont(new Font("Arial", Font.ITALIC, 12));
            g2.setColor(textMuted);
            g2.drawString(LanguageManager.t("pdf.footer"), 0, y);

            return Printable.PAGE_EXISTS;
        });
        // Show print dialog
        if (job.printDialog()) {
            try {
                job.print();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Draw a rounded rectangle "card" background with border

    private static void drawCard(Graphics2D g, int x, int y, int w, int h, Color bg, Color border) {
        g.setColor(bg);
        g.fillRoundRect(x, y, w, h, 18, 18);
        g.setColor(border);
        g.drawRoundRect(x, y, w, h, 18, 18);
    }
    // Draw a vital section with mean/min/max and return next Y position
    private static int drawVitalBlock(
            Graphics2D g,
            String title,
            String unit,
            double mean,
            double min,
            double max,
            int x,
            int y,
            Color primary,
            Color muted
    ) {
        g.setFont(new Font("Arial", Font.BOLD, 17));
        g.setColor(primary);
        g.drawString(title, x, y);

        g.setFont(new Font("Arial", Font.PLAIN, 15));
        g.setColor(muted);
        y += 22;
        g.drawString(String.format("%s: %.1f %s", LanguageManager.t("pdf.mean"), mean, unit), x + 20, y);
        y += 20;
        g.drawString(String.format("%s:  %.1f %s", LanguageManager.t("pdf.min"), min, unit), x + 20, y);
        y += 20;
        g.drawString(String.format("%s:  %.1f %s", LanguageManager.t("pdf.max"), max, unit), x + 20, y);

        // Divider line
        g.setColor(new Color(230, 235, 240));
        g.drawLine(x, y + 12, x + 520, y + 12);

        return y + 44;
    }
    // Draw wrapped text within a fixed width
    private static void drawWrappedText(
            Graphics2D g,
            String text,
            int x,
            int y,
            int width,
            int lineHeight
    ) {
        FontMetrics fm = g.getFontMetrics();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            if (fm.stringWidth(line + word) > width) {
                g.drawString(line.toString(), x, y);
                line = new StringBuilder(word).append(" ");
                y += lineHeight;
            } else {
                line.append(word).append(" ");
            }
        }
        g.drawString(line.toString(), x, y);
    }
}
