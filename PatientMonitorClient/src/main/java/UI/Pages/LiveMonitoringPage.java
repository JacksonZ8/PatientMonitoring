package UI.Pages;

import Models.Vitals.LiveVitals;
import Models.Patients.Patient;
import Models.Vitals.VitalRecord;
import Models.Vitals.VitalRecordIO;
import Services.ECGSimulatorService;
import Services.MinuteAveragingService;
import Services.RespSimulatorService;
import Services.VitalsPdfReportService;
import Services.VitalTableJsonExportService;
import UI.Components.Tiles.WaveformPanel;
import UI.MainWindow;
import UI.PageLifecycle;
import Utilities.LanguageManager;
import Utilities.SettingManager;
import Utilities.SwingStyle;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class LiveMonitoringPage extends JPanel implements PageLifecycle {

    // theme
    private static final Color BG_MAIN = new Color(245, 247, 250);
    private static final Color BG_CARD = Color.WHITE;
    private static final Color BORDER = new Color(220, 225, 230);
    private static final Color DARK_BG = new Color(24, 26, 30);
    private static final Color DARK_CARD = new Color(32, 35, 41);
    private static final Color DARK_FIELD = new Color(45, 48, 56);
    private static final Color DARK_BORDER = new Color(72, 79, 92);

    private static final Color TEXT_PRIMARY = new Color(30, 41, 59);
    private static final Color TEXT_MUTED = new Color(100, 116, 139);
    private static final Color DARK_TEXT = new Color(235, 235, 235);
    private static final Color DARK_MUTED = new Color(170, 170, 170);

    private static final Color BLUE = new Color(37, 99, 235);
    private static final Color GREEN = new Color(22, 163, 74);
    private static final Color RED = new Color(220, 38, 38);
    private static final Color AMBER = new Color(234, 179, 8);

    // data
    private final LiveVitals vitals;
    private final ECGSimulatorService ecgSim;
    private final RespSimulatorService respSim;
    private final MinuteAveragingService averagingService;
    private final MainWindow window;
    private final Patient patient;
    private final boolean darkMode = new SettingManager().isDarkMode();

    private static final int MIN_TIME_WINDOW_SECONDS = 5;
    private static final int MAX_TIME_WINDOW_SECONDS = 300;
    private int timeWindowSeconds = 10;

    private JLabel hrValue, spo2Value, respValue, bpValue;
    private JTextField timeWindowField;
    private JLabel liveClock;

    private WaveformPanel ecgPanel;
    private WaveformPanel respPanel;

    private DefaultTableModel historyModel;
    private Timer liveTimer;
    private Timer clockTimer;

    public LiveMonitoringPage(Patient patient, MainWindow window) {
        this.window = window;
        this.patient = patient;

        vitals = LiveVitals.getShared(patient.getId(), patient.getBloodPressure());
        ecgSim = new ECGSimulatorService();
        respSim = new RespSimulatorService();
        averagingService = new MinuteAveragingService(vitals);

        setLayout(new BorderLayout());
        setBackground(pageBg());

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBackground(pageBg());

        top.add(buildHeader());
        top.add(buildTopBar());

        add(top, BorderLayout.NORTH);
        add(buildMainContent(), BorderLayout.CENTER);

        startLiveLoop();
        startClock();
    }

    // header
    private JPanel buildHeader() {

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(cardBg());
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, borderColor()));
        header.setPreferredSize(new Dimension(0, 56));

        JLabel info = new JLabel(
                LanguageManager.t("live.patient") + ": " + patient.getName() +
                        "    " + LanguageManager.t("live.id") + ": " + patient.getId()
        );
        info.setFont(new Font("Arial", Font.BOLD, 15));
        info.setForeground(primaryText());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        right.setBackground(cardBg());

        JButton digitalTwin = new JButton(LanguageManager.t("live.digitalTwin"));
        styleSecondaryButton(digitalTwin);
        digitalTwin.addActionListener(e -> window.showDigitalTwin(patient));

        Integer[] hours = {1, 2, 4, 6, 12, 24, 48, 72};
        JComboBox<Integer> hourSelect = new JComboBox<>(hours);
        hourSelect.setSelectedItem(24);
        hourSelect.setPreferredSize(new Dimension(86, 34));
        SwingStyle.styleComboBox(hourSelect, darkMode);

        JButton exportPdf = new JButton(LanguageManager.t("live.exportPdf"));
        exportPdf.setBackground(BLUE);
        exportPdf.setForeground(Color.WHITE);
        exportPdf.setOpaque(true);
        exportPdf.setBorderPainted(false);

        exportPdf.addActionListener(e ->
                VitalsPdfReportService.exportPdfForLastHours(
                        patient,
                        (Integer) hourSelect.getSelectedItem()
                )
        );

        JButton exportJson = new JButton(LanguageManager.t("live.exportJson"));
        styleSecondaryButton(exportJson);
        exportJson.addActionListener(e ->
                VitalTableJsonExportService.exportJson(patient.getId())
        );

        right.add(digitalTwin);
        right.add(exportJson);
        right.add(new JLabel(LanguageManager.t("live.last")));
        right.add(hourSelect);
        right.add(new JLabel(LanguageManager.t("live.hours")));
        right.add(exportPdf);

        header.add(info, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);

        return header;
    }

    // top bar
    private JPanel buildTopBar() {

        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(cardBg());
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, borderColor()),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        left.setBackground(cardBg());

        left.add(label(LanguageManager.t("live.timeWindow")));

        timeWindowField = new JTextField(String.valueOf(timeWindowSeconds), 5);
        SwingStyle.styleTextField(timeWindowField, darkMode);
        left.add(timeWindowField);

        JButton apply = new JButton(LanguageManager.t("live.apply"));
        styleSecondaryButton(apply);
        apply.addActionListener(e -> applyTimeWindow());

        left.add(apply);
        left.add(preset("10s", 10));
        left.add(preset("30s", 30));
        left.add(preset("60s", 60));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.setBackground(cardBg());

        liveClock = label("");

        JLabel live = new JLabel(LanguageManager.t("live.live"));
        live.setOpaque(true);
        live.setBackground(darkMode ? new Color(22, 72, 50) : new Color(220, 252, 231));
        live.setForeground(darkMode ? new Color(187, 247, 208) : GREEN);
        live.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        right.add(liveClock);
        right.add(live);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);

        return bar;
    }

    // content
    private JPanel buildMainContent() {

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(pageBg());
        content.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        content.add(buildVitalsRow());
        content.add(Box.createVerticalStrut(22));

        ecgPanel = new WaveformPanel(LanguageManager.t("live.ecgAxis"), RED);
        respPanel = new WaveformPanel(LanguageManager.t("live.respAxis"), GREEN);
        applyWaveTheme(ecgPanel);
        applyWaveTheme(respPanel);

        updateAxes();

        content.add(wrap(ecgPanel, LanguageManager.t("live.ecgWaveform")));
        content.add(Box.createVerticalStrut(22));
        content.add(wrap(respPanel, LanguageManager.t("live.respWaveform")));
        content.add(Box.createVerticalStrut(22));
        content.add(buildHistoryTable());

        return content;
    }

    // vital cards
    private JPanel buildVitalsRow() {

        JPanel row = new JPanel(new GridLayout(1, 4, 18, 0));
        row.setBackground(pageBg());

        hrValue = valueLabel();
        spo2Value = valueLabel();
        respValue = valueLabel();
        bpValue = valueLabel();
        bpValue.setText(patient.getBloodPressure());

        row.add(card(LanguageManager.t("live.heartRate"), hrValue, "bpm"));
        row.add(card(LanguageManager.t("live.spo2"), spo2Value, "%"));
        row.add(card(LanguageManager.t("live.respRate"), respValue, LanguageManager.t("live.breathsPerMin")));
        row.add(card(LanguageManager.t("live.bloodPressure"), bpValue, "mmHg"));

        return row;
    }

    // table
    private JPanel buildHistoryTable() {

        String[] cols = {
                LanguageManager.t("live.col.time"),
                LanguageManager.t("live.col.hr"),
                LanguageManager.t("live.col.resp"),
                LanguageManager.t("live.col.temp"),
                LanguageManager.t("live.col.spo2")
        };
        historyModel = new DefaultTableModel(cols, 0);

        JTable table = new JTable(historyModel);
        table.setRowHeight(26);
        table.setBackground(cardBg());
        table.setForeground(primaryText());
        table.setGridColor(borderColor());
        table.getTableHeader().setBackground(darkMode ? DARK_FIELD : new Color(248, 250, 252));
        table.getTableHeader().setForeground(primaryText());

        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(1000, 190));
        scroll.setBorder(BorderFactory.createLineBorder(borderColor()));
        scroll.setViewportBorder(null);
        scroll.getViewport().setBackground(cardBg());

        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(cardBg());
        p.setBorder(BorderFactory.createLineBorder(borderColor()));

        JLabel title = new JLabel(LanguageManager.t("live.history"));
        title.setFont(new Font("Arial", Font.BOLD, 14));
        title.setForeground(primaryText());

        JLabel source = new JLabel(LanguageManager.t("live.sessionOnly"));
        source.setFont(new Font("Arial", Font.PLAIN, 12));
        source.setForeground(mutedText());

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(cardBg());
        header.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        header.add(title);
        header.add(Box.createVerticalStrut(2));
        header.add(source);

        p.add(header, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);

        return p;
    }

    // live loop
    private void startLiveLoop() {

        stopTimers();

        liveTimer = new Timer(1000, e -> {

            averagingService.sample();

            int hr = (int) vitals.getHeartRate();
            int rr = (int) vitals.getRespRate();
            int spo2 = (int) vitals.getSpO2();

            hrValue.setText(String.valueOf(hr));
            respValue.setText(String.valueOf(rr));
            spo2Value.setText(String.valueOf(spo2));

            hrValue.setForeground(hr > 120 ? RED : hr < 50 ? AMBER : primaryText());
            spo2Value.setForeground(spo2 < 92 ? RED : primaryText());
            respValue.setForeground(rr > 25 || rr < 10 ? AMBER : primaryText());

            ecgPanel.addSamples(ecgSim.nextSamples(25, hr), timeWindowSeconds * 100);
            respPanel.addSamples(respSim.nextSamples(10, rr), timeWindowSeconds * 30);

            refreshHistory();
        });
        liveTimer.start();
    }

    private void refreshHistory() {
        historyModel.setRowCount(0);
        for (VitalRecord r : VitalRecordIO.loadAll()) {
            if (r.getPatientId() != patient.getId()) continue;
            historyModel.addRow(new Object[]{
                    r.getTimestamp().toLocalTime()
                            .format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    String.format("%.1f", r.getAvgHeartRate()),
                    String.format("%.1f", r.getAvgRespRate()),
                    String.format("%.2f", r.getAvgTemperature()),
                    String.format("%.1f", r.getAvgSpO2())
            });
        }
    }

    // helpers
    private JPanel wrap(JPanel p, String title) {
        JPanel c = new JPanel(new BorderLayout());
        c.setBackground(cardBg());
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor()),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        JLabel h = new JLabel(title);
        h.setFont(new Font("Arial", Font.BOLD, 15));
        h.setForeground(primaryText());
        c.add(h, BorderLayout.NORTH);
        c.add(p, BorderLayout.CENTER);
        return c;
    }

    private JLabel valueLabel() {
        JLabel l = new JLabel("--");
        l.setFont(new Font("Arial", Font.BOLD, 36));
        l.setForeground(primaryText());
        return l;
    }

    private JPanel card(String title, JLabel value, String unit) {

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(cardBg());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor()),
                BorderFactory.createEmptyBorder(18, 20, 18, 20)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        titleLabel.setForeground(mutedText());

        JLabel unitLabel = new JLabel(unit);
        unitLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        unitLabel.setForeground(mutedText());

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.add(value);
        center.add(Box.createVerticalStrut(4));
        center.add(unitLabel);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);

        return card;
    }

    private JLabel label(String t) {
        JLabel l = new JLabel(t);
        l.setForeground(mutedText());
        return l;
    }

    private JButton preset(String t, int s) {
        JButton b = new JButton(t);
        styleSecondaryButton(b);
        b.addActionListener(e -> {
            timeWindowSeconds = s;
            timeWindowField.setText(String.valueOf(s));
            updateAxes();
        });
        return b;
    }

    private void applyTimeWindow() {
        try {
            int next = Integer.parseInt(timeWindowField.getText().trim());
            if (next < MIN_TIME_WINDOW_SECONDS || next > MAX_TIME_WINDOW_SECONDS) {
                showInvalidTimeWindow();
                return;
            }
            timeWindowSeconds = next;
        } catch (Exception ex) {
            showInvalidTimeWindow();
            return;
        }
        updateAxes();
    }

    private void showInvalidTimeWindow() {
        timeWindowField.setText(String.valueOf(timeWindowSeconds));
        JOptionPane.showMessageDialog(
                this,
                LanguageManager.t("live.invalidTimeWindow"),
                LanguageManager.t("live.invalidTimeWindowTitle"),
                JOptionPane.WARNING_MESSAGE
        );
    }

    private void updateAxes() {
        ecgPanel.setAxis(-2, 2, timeWindowSeconds);
        respPanel.setAxis(-1.5, 1.5, timeWindowSeconds);
    }

    private Color pageBg() {
        return darkMode ? DARK_BG : BG_MAIN;
    }

    private Color cardBg() {
        return darkMode ? DARK_CARD : BG_CARD;
    }

    private Color borderColor() {
        return darkMode ? DARK_BORDER : BORDER;
    }

    private Color primaryText() {
        return darkMode ? DARK_TEXT : TEXT_PRIMARY;
    }

    private Color mutedText() {
        return darkMode ? DARK_MUTED : TEXT_MUTED;
    }

    private void styleSecondaryButton(JButton button) {
        button.setFocusPainted(false);
        button.setForeground(primaryText());
        button.setBackground(darkMode ? DARK_FIELD : Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor()),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));
    }

    private void applyWaveTheme(WaveformPanel panel) {
        panel.setChartColors(
                cardBg(),
                darkMode ? new Color(49, 55, 66) : new Color(235, 235, 235),
                darkMode ? DARK_MUTED : Color.GRAY
        );
    }

    private void startClock() {
        if (clockTimer != null) clockTimer.stop();
        clockTimer = new Timer(1000, e ->
                liveClock.setText(
                        LocalTime.now()
                                .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                )
        );
        clockTimer.start();
    }

    private void stopTimers() {
        if (liveTimer != null) {
            liveTimer.stop();
            liveTimer = null;
        }
        if (clockTimer != null) {
            clockTimer.stop();
            clockTimer = null;
        }
    }

    @Override
    public void onShown() {
        startLiveLoop();
        startClock();
    }

    @Override
    public void onHidden() {
        stopTimers();
    }

    @Override
    public void disposePage() {
        stopTimers();
    }

    @Override
    public void removeNotify() {
        stopTimers();
        super.removeNotify();
    }
}
