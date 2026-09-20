package UI.Components;

import Models.Vitals.AlertRecord;
import Models.Vitals.LiveVitals;
import Services.AlertManager;
import UI.MainWindow;
import Models.Patients.AddedPatientDB;
import Models.Patients.Patient;
import Utilities.LanguageManager;
import Utilities.SettingManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AlertHistorySidebar extends JPanel {

    private static final int SIDEBAR_WIDTH = 450;

    private final MainWindow window;

    private final JLabel titleLabel = new JLabel(LanguageManager.t("alerts.title"));
    private final JLabel subtitleLabel = new JLabel("0 " + LanguageManager.t("alerts.critical") + " · 0 " + LanguageManager.t("alerts.warnings"));

    private final JPanel listPanel = new JPanel();
    private final JScrollPane scrollPane;

    private final Timer refreshTimer;
    private int lastHistorySize = -1;
    private final boolean darkMode = new SettingManager().isDarkMode();

    // keep track of which alerts have been acknowledged
    private final Set<String> acknowledgedKeys = new HashSet<>();

    // Creates the alert sidebar UI and starts a timer to refresh its contents periodically.
    public AlertHistorySidebar(MainWindow window) {
        this.window = window;

        Color sidebarBg = sidebarBg();

        setLayout(new BorderLayout());
        setOpaque(true);
        setBackground(sidebarBg);

        // Full-height divider line on the LEFT of the sidebar
        setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, dividerColor()));

        // Lock width
        setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));
        setMinimumSize(new Dimension(SIDEBAR_WIDTH, 0));
        setMaximumSize(new Dimension(SIDEBAR_WIDTH, Integer.MAX_VALUE));

        add(buildHeader(), BorderLayout.NORTH);

        // List panel blends into sidebar background
        listPanel.setOpaque(true);
        listPanel.setBackground(getBackground());
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBorder(new EmptyBorder(10, 12, 12, 12));

        scrollPane = new JScrollPane(listPanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);

        // IMPORTANT: viewport must be opaque with sidebar background to look continuous
        scrollPane.getViewport().setOpaque(true);
        scrollPane.getViewport().setBackground(getBackground());

        add(scrollPane, BorderLayout.CENTER);

        refreshTimer = new Timer(1000, e -> refreshIfNeeded());
        refreshTimer.start();

        refresh();
    }

    // Builds the header area containing the sidebar title and alert counts.
    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(true);
        header.setBackground(getBackground());

        // Only a bottom separator line
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, dividerColor()),
                new EmptyBorder(14, 14, 14, 14)
        ));

        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(primaryText());

        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        subtitleLabel.setForeground(mutedText());

        header.add(titleLabel);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitleLabel);

        return header;
    }

    // Refreshes the UI only if the alert history size has changed to reduce unnecessary rebuilding.
    private void refreshIfNeeded() {
        List<AlertRecord> history = AlertManager.getInstance().getHistory();
        if (history.size() != lastHistorySize) {
            refresh();
        } else {
            repaint(); // keeps "time ago" feeling alive
        }
    }

    public void dispose() {
        refreshTimer.stop();
    }

    // Rebuilds the sidebar list from alert history and updates the critical/warning counts.
    public void refresh() {
        List<AlertRecord> history = AlertManager.getInstance().getHistory();
        lastHistorySize = history.size();

        // newest first
        Collections.reverse(history);

        // count critical/warning BUT only those not acknowledged
        int critical = 0, warning = 0;
        for (AlertRecord r : history) {
            if (acknowledgedKeys.contains(keyOf(r))) continue;
            if (r.getSeverity() == LiveVitals.VitalsSeverity.DANGER) critical++;
            if (r.getSeverity() == LiveVitals.VitalsSeverity.WARNING) warning++;
        }
        subtitleLabel.setText(critical + " " + LanguageManager.t("alerts.critical") + " · " + warning + " " + LanguageManager.t("alerts.warnings"));

        listPanel.removeAll();

        int max = 30;
        int shown = 0;

        for (AlertRecord r : history) {
            if (shown >= max) break;
            if (acknowledgedKeys.contains(keyOf(r))) continue;

            JPanel card = buildCard(r);
            listPanel.add(card);
            listPanel.add(Box.createVerticalStrut(12));
            shown++;
        }

        if (shown == 0) {
            JLabel none = new JLabel(LanguageManager.t("alerts.none"));
            none.setFont(new Font("Arial", Font.PLAIN, 12));
            none.setForeground(mutedText());
            none.setBorder(new EmptyBorder(8, 4, 0, 0));
            none.setAlignmentX(Component.LEFT_ALIGNMENT);
            listPanel.add(none);
        }

        // IMPORTANT: pushes extra vertical space to the bottom so cards don't stretch
        listPanel.add(Box.createVerticalGlue());

        listPanel.revalidate();
        listPanel.repaint();
    }

    // Creates a styled alert card panel for a single alert record.
    private JPanel buildCard(AlertRecord r) {
        LiveVitals.VitalsSeverity sev = r.getSeverity();

        Color border;
        Color bg;
        Color tagColor;

        if (sev == LiveVitals.VitalsSeverity.DANGER) {
            border = new Color(235, 84, 84);
            bg = darkMode ? new Color(58, 38, 44) : new Color(255, 245, 245);
            tagColor = new Color(235, 84, 84);
        } else if (sev == LiveVitals.VitalsSeverity.WARNING) {
            border = new Color(230, 163, 57);
            bg = darkMode ? new Color(58, 49, 32) : new Color(255, 250, 237);
            tagColor = new Color(230, 163, 57);
        } else {
            border = darkMode ? dividerColor() : new Color(210, 215, 222);
            bg = darkMode ? new Color(32, 35, 41) : Color.WHITE;
            tagColor = new Color(120, 125, 135);
        }

        JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(true);
        card.setBackground(bg);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(border, 1, true),
                new EmptyBorder(12, 12, 12, 12)
        ));

        // Fixed height
        int CARD_HEIGHT = 185;
        card.setPreferredSize(new Dimension(Short.MAX_VALUE, CARD_HEIGHT));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, CARD_HEIGHT));
        card.setMinimumSize(new Dimension(0, CARD_HEIGHT));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        //top row
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tag = new JLabel(sev == LiveVitals.VitalsSeverity.DANGER ? LanguageManager.t("alerts.criticalTag") :
                sev == LiveVitals.VitalsSeverity.WARNING ? LanguageManager.t("alerts.warningTag") : LanguageManager.t("alerts.infoTag"));
        tag.setFont(new Font("Arial", Font.BOLD, 12));
        tag.setForeground(tagColor);

        JLabel timeAgo = new JLabel(formatTimeAgo(r.getTimestamp()));
        timeAgo.setFont(new Font("Arial", Font.PLAIN, 12));
        timeAgo.setForeground(mutedText());

        topRow.add(tag, BorderLayout.WEST);
        topRow.add(timeAgo, BorderLayout.EAST);

        //middle
        JPanel mid = new JPanel();
        mid.setOpaque(false);
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        mid.setAlignmentX(Component.LEFT_ALIGNMENT);

        List<String> causes = (r.getCauses() == null) ? new ArrayList<>() : r.getCauses();
        String headline = causes.isEmpty() ? LanguageManager.t("alerts.defaultHeadline") : causes.get(0);

        JLabel headlineLabel = new JLabel(headline);
        headlineLabel.setFont(new Font("Arial", Font.BOLD, 13));
        headlineLabel.setForeground(primaryText());
        headlineLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel patientLine = new JLabel(LanguageManager.t("alerts.patient") + " " + r.getPatientName() + " · " + LanguageManager.t("live.id") + " " + r.getPatientId());
        patientLine.setFont(new Font("Arial", Font.PLAIN, 12));
        patientLine.setForeground(mutedText());
        patientLine.setAlignmentX(Component.LEFT_ALIGNMENT);

        mid.add(headlineLabel);
        mid.add(Box.createVerticalStrut(4));
        mid.add(patientLine);

        if (causes.size() > 1) {
            JLabel extra = new JLabel(String.join(", ", causes.subList(1, causes.size())));
            extra.setFont(new Font("Arial", Font.PLAIN, 11));
            extra.setForeground(mutedText());
            extra.setBorder(new EmptyBorder(6, 0, 0, 0));
            extra.setAlignmentX(Component.LEFT_ALIGNMENT);
            mid.add(extra);
        }

        //bottom row buttons
        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.X_AXIS));
        bottom.setBorder(new EmptyBorder(10, 0, 0, 0));
        bottom.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton viewBtn = new JButton(LanguageManager.t("alerts.viewPatient"));
        JButton ackBtn = new JButton(LanguageManager.t("alerts.acknowledge"));

        styleButton(viewBtn, true, tagColor);
        styleButton(ackBtn, false, tagColor);

        // View Patient (unchanged from earlier working version)
        viewBtn.addActionListener(e -> {
            Patient p = findPatientById(r.getPatientId());
            if (p != null) {
                window.showLiveMonitoring(p);
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        LanguageManager.t("alerts.notFound"),
                        LanguageManager.t("alerts.viewPatient"),
                        JOptionPane.WARNING_MESSAGE
                );
            }
        });

        //Acknowledge hides it from the sidebar immediately
        ackBtn.addActionListener(e -> {
            acknowledgedKeys.add(keyOf(r));
            refresh();
        });

        bottom.add(viewBtn);
        bottom.add(Box.createHorizontalStrut(10));
        bottom.add(ackBtn);
        bottom.add(Box.createHorizontalGlue());

        // --- Stack inside the card ---
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setAlignmentX(Component.LEFT_ALIGNMENT);

        center.add(topRow);
        center.add(Box.createVerticalStrut(6));
        center.add(mid);
        center.add(bottom);

        card.add(center, BorderLayout.CENTER);

        return card;
    }

    // Applies consistent styling to the sidebar action buttons.
    private void styleButton(JButton btn, boolean filled, Color accent) {
        btn.setFocusPainted(false);
        btn.setFont(new Font("Arial", Font.BOLD, 12));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(120, 30));

        if (filled) {
            btn.setBackground(accent);
            btn.setForeground(Color.WHITE);
            btn.setBorder(new LineBorder(accent, 1, true));
        } else {
            btn.setBackground(darkMode ? new Color(32, 35, 41) : Color.WHITE);
            btn.setForeground(accent);
            btn.setBorder(new LineBorder(accent, 1, true));
        }

        btn.setOpaque(true);
    }

    private Color sidebarBg() {
        return darkMode ? new Color(24, 26, 30) : new Color(246, 247, 249);
    }

    private Color dividerColor() {
        return darkMode ? new Color(70, 78, 91) : new Color(230, 232, 236);
    }

    private Color primaryText() {
        return darkMode ? new Color(238, 241, 245) : new Color(20, 20, 20);
    }

    private Color mutedText() {
        return darkMode ? new Color(188, 196, 207) : new Color(120, 125, 135);
    }

    // Searches the in-memory patient database for a patient matching the given ID.
    private Patient findPatientById(int id) {
        for (Patient p : AddedPatientDB.getAll()) {
            if (p.getId() == id) return p;
        }
        return null;
    }

    // Generates a stable key used to uniquely identify an alert record for acknowledgement.
    private String keyOf(AlertRecord r) {
        long ms = (r.getTimestamp() == null) ? 0L : r.getTimestamp().toEpochMilli();
        return r.getPatientId() + "|" + r.getSeverity() + "|" + ms;
    }

    // Formats a timestamp into a short human-readable "time ago" string.
    private String formatTimeAgo(Instant ts) {
        if (ts == null) return "";
        long s = Duration.between(ts, Instant.now()).getSeconds();
        if (s < 0) s = 0;
        if (s < 60) return s + LanguageManager.t("alerts.secondsAgo");
        long m = s / 60;
        if (m < 60) return m + LanguageManager.t("alerts.minutesAgo");
        long h = m / 60;
        if (h < 24) return h + LanguageManager.t("alerts.hoursAgo");
        long d = h / 24;
        return d + LanguageManager.t("alerts.daysAgo");
    }
}
