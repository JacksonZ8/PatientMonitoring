package UI.Components;

import Models.Patients.Patient;
import Services.DigitalTwinNoteService;
import Utilities.LanguageManager;
import Utilities.SettingManager;
import Utilities.SwingStyle;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

public class DigitalTwinPanel extends JPanel {

    private static final Color PAGE_BG = new Color(248, 250, 252);
    private static final Color DARK_PAGE_BG = new Color(24, 26, 30);
    private static final Color PANEL_BG = Color.WHITE;
    private static final Color DARK_PANEL_BG = new Color(32, 35, 41);
    private static final Color PANEL_BLUE = new Color(224, 236, 255);
    private static final Color DARK_PANEL_BLUE = new Color(43, 49, 62);
    private static final Color CHIP_BLUE = new Color(191, 219, 254);
    private static final Color DARK_CHIP_BLUE = new Color(45, 58, 82);
    private static final Color STROKE = new Color(203, 213, 225);
    private static final Color DARK_STROKE = new Color(72, 79, 92);
    private static final Color TEXT = Color.BLACK;
    private static final Color DARK_TEXT = new Color(235, 235, 235);
    private static final Color BUTTON_BLUE = new Color(98, 143, 243);
    private static final Color CARDIAC = new Color(239, 68, 68);
    private static final Color RESP = new Color(96, 165, 250);
    private static final Color METABOLIC = new Color(168, 85, 247);
    private static final Color STABLE = new Color(34, 197, 94);
    private static final Color WARNING = new Color(250, 204, 21);

    private final JComboBox<PatientOption> patientSelect = new JComboBox<>();
    private final JLabel demographics = new JLabel();
    private final JPanel recordsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    private final JTextArea noteText = new JTextArea();
    private final JComboBox<String> noteLevel = new JComboBox<>(new String[]{"Info", "Warning", "Critical"});
    private final JPanel notesPanel = new JPanel();
    private final BodyPanel bodyPanel = new BodyPanel();
    private final WavePanel ecgWave = new WavePanel(CARDIAC, true);
    private final WavePanel respWave = new WavePanel(STABLE, false);
    private final DeviceCard hrDevice = new DeviceCard(CARDIAC);
    private final DeviceCard rrDevice = new DeviceCard(METABOLIC);
    private final DeviceCard spo2Device = new DeviceCard(STABLE);
    private final DeviceCard bpDevice = new DeviceCard(RESP);
    private final DeviceCard tempDevice = new DeviceCard(WARNING);
    private final DeviceCard statusDevice = new DeviceCard(TEXT);
    private final JLabel debugBadge = new JLabel();
    private final DigitalTwinNoteService noteService = new DigitalTwinNoteService();

    private final Deque<Note> notes = new ArrayDeque<>();
    private final boolean darkMode = new SettingManager().isDarkMode();
    private Consumer<Patient> patientSelectionHandler;
    private boolean updatingPatientSelect;
    private Patient patient;
    private int patientId = -1;
    private int hr;
    private int rr;
    private int spo2;
    private int sys;
    private int dia;
    private double temp;

    // Native Swing recreation of the former HTML dashboard layout.
    public DigitalTwinPanel() {
        setLayout(new BorderLayout());
        setBackground(pageBg());
        patientSelect.addActionListener(e -> {
            if (updatingPatientSelect || patientSelectionHandler == null) return;
            Object selected = patientSelect.getSelectedItem();
            if (selected instanceof PatientOption option && option.patient != null) {
                patientSelectionHandler.accept(option.patient);
            }
        });

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(14, 24, 18, 24));

        root.add(buildTitle(), BorderLayout.NORTH);
        root.add(buildDashboardGrid(), BorderLayout.CENTER);
        root.add(buildDebugBadge(), BorderLayout.SOUTH);
        add(root, BorderLayout.CENTER);

        setSelectedPatientId(-1);
        setVitals(0, 0, 0, 0, 0, 0);
    }

    private JComponent buildTitle() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(LanguageManager.t("digitalTwin.dashboardTitle"), SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 28));
        title.setForeground(textColor());
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel(LanguageManager.t("digitalTwin.subtitle"), SwingConstants.CENTER);
        sub.setFont(new Font("Arial", Font.PLAIN, 12));
        sub.setForeground(textColor());
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(6));
        header.add(sub);
        return header;
    }

    private JComponent buildDashboardGrid() {
        JPanel grid = new JPanel(new GridLayout(1, 3, 38, 0));
        grid.setOpaque(false);
        grid.add(buildLeftPanel());
        grid.add(buildCenterPanel());
        grid.add(buildRightPanel());
        return grid;
    }

    private JComponent buildLeftPanel() {
        JPanel panel = dashboardPanel();
        panel.setPreferredSize(new Dimension(300, 640));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(sectionTitle(LanguageManager.t("digitalTwin.patientOverview")));

        patientSelect.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        patientSelect.setFocusable(false);
        styleCombo(patientSelect);
        panel.add(patientSelect);
        panel.add(Box.createVerticalStrut(10));

        panel.add(metricCard(demographics));
        panel.add(Box.createVerticalStrut(12));

        JPanel recordsCard = metricCard(null);
        recordsCard.setLayout(new BorderLayout());
        recordsCard.add(metricTitle(LanguageManager.t("digitalTwin.healthRecords")), BorderLayout.NORTH);
        recordsPanel.setOpaque(false);
        recordsCard.add(recordsPanel, BorderLayout.CENTER);
        panel.add(recordsCard);
        panel.add(Box.createVerticalStrut(12));

        JPanel notesCard = metricCard(null);
        notesCard.setLayout(new BoxLayout(notesCard, BoxLayout.Y_AXIS));
        notesCard.add(metricTitle(LanguageManager.t("digitalTwin.annotations")));
        notesCard.add(buildQuickButtons());

        noteText.setLineWrap(true);
        noteText.setWrapStyleWord(true);
        noteText.setRows(4);
        SwingStyle.styleTextArea(noteText, darkMode);
        notesCard.add(noteText);
        notesCard.add(Box.createVerticalStrut(8));

        JPanel addRow = new JPanel(new GridLayout(1, 2, 8, 0));
        addRow.setOpaque(false);
        styleCombo(noteLevel);
        addRow.add(noteLevel);
        JButton add = primaryButton(LanguageManager.t("digitalTwin.addNote"));
        add.addActionListener(e -> addNote());
        addRow.add(add);
        notesCard.add(addRow);
        notesCard.add(Box.createVerticalStrut(8));

        notesPanel.setOpaque(false);
        notesPanel.setLayout(new BoxLayout(notesPanel, BoxLayout.Y_AXIS));
        notesCard.add(notesPanel);
        panel.add(notesCard);

        return panel;
    }

    private JComponent buildQuickButtons() {
        JPanel row = new JPanel(new GridLayout(1, 3, 8, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(8, 0, 8, 0));
        addQuick(row, LanguageManager.t("digitalTwin.trend"), LanguageManager.t("digitalTwin.trendNote"));
        addQuick(row, LanguageManager.t("digitalTwin.medication"), LanguageManager.t("digitalTwin.medicationNote"));
        addQuick(row, LanguageManager.t("digitalTwin.escalate"), LanguageManager.t("digitalTwin.escalateNote"));
        return row;
    }

    private void addQuick(JPanel row, String label, String text) {
        JButton btn = primaryButton(label);
        btn.setFont(new Font("Arial", Font.PLAIN, 11));
        btn.addActionListener(e -> {
            noteText.setText(text);
            noteText.requestFocusInWindow();
        });
        row.add(btn);
    }

    private JComponent buildCenterPanel() {
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        bodyPanel.setPreferredSize(new Dimension(640, 760));
        bodyPanel.setMinimumSize(new Dimension(420, 600));
        center.add(bodyPanel);
        return center;
    }

    private JComponent buildRightPanel() {
        JPanel panel = dashboardPanel();
        panel.setPreferredSize(new Dimension(360, 640));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(sectionTitle(LanguageManager.t("digitalTwin.liveTelemetry")));
        panel.add(graphCard(LanguageManager.t("digitalTwin.ecg"), ecgWave));
        panel.add(Box.createVerticalStrut(14));
        panel.add(graphCard(LanguageManager.t("digitalTwin.respiration"), respWave));
        panel.add(Box.createVerticalStrut(14));

        JPanel devices = new JPanel(new GridLayout(3, 2, 12, 12));
        devices.setOpaque(false);
        devices.add(hrDevice);
        devices.add(rrDevice);
        devices.add(spo2Device);
        devices.add(bpDevice);
        devices.add(tempDevice);
        devices.add(statusDevice);
        panel.add(devices);
        return panel;
    }

    private JPanel dashboardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(panelBg());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(strokeColor()),
                new EmptyBorder(18, 18, 18, 18)
        ));
        return panel;
    }

    private JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 16));
        label.setForeground(textColor());
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setBorder(new EmptyBorder(0, 0, 14, 0));
        return label;
    }

    private JLabel metricTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        label.setForeground(textColor());
        label.setBorder(new EmptyBorder(0, 0, 8, 0));
        return label;
    }

    private JPanel metricCard(JComponent child) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(metricBg());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(strokeColor()),
                new EmptyBorder(12, 12, 12, 12)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 170));
        if (child != null) card.add(child, BorderLayout.CENTER);
        return card;
    }

    private JComponent graphCard(String title, WavePanel wave) {
        JPanel card = metricCard(null);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 128));
        card.setLayout(new BorderLayout());
        card.add(metricTitle(title), BorderLayout.NORTH);
        card.add(wave, BorderLayout.CENTER);
        return card;
    }

    private JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setForeground(Color.WHITE);
        button.setBackground(BUTTON_BLUE);
        button.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        return button;
    }

    private void styleCombo(JComboBox<?> combo) {
        SwingStyle.styleComboBox(combo, darkMode);
    }

    private JComponent buildDebugBadge() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        debugBadge.setOpaque(true);
        debugBadge.setBackground(darkMode ? DARK_PANEL_BG : PANEL_BG);
        debugBadge.setForeground(textColor());
        debugBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(strokeColor()),
                new EmptyBorder(7, 10, 7, 10)
        ));
        debugBadge.setFont(new Font("Arial", Font.PLAIN, 12));
        row.add(debugBadge);
        return row;
    }

    public void setDoctorEmail(String email) {
        // Kept for compatibility with DigitalTwinPage. Native mode does not poll server HTML.
    }

    public void setPatientSelectionHandler(Consumer<Patient> handler) {
        this.patientSelectionHandler = handler;
    }

    public void setPatients(List<Patient> patients, Patient selectedPatient) {
        updatingPatientSelect = true;
        try {
            patientSelect.removeAllItems();
            if (patients != null) {
                for (Patient p : patients) {
                    patientSelect.addItem(new PatientOption(p));
                }
            }

            if (patientSelect.getItemCount() == 0) {
                patientSelect.addItem(new PatientOption(null));
            } else if (selectedPatient != null) {
                for (int i = 0; i < patientSelect.getItemCount(); i++) {
                    PatientOption option = patientSelect.getItemAt(i);
                    if (option.patient != null && option.patient.getId() == selectedPatient.getId()) {
                        patientSelect.setSelectedIndex(i);
                        break;
                    }
                }
            }
        } finally {
            updatingPatientSelect = false;
        }
    }

    public void refreshDashboardContext() {
        // Kept for compatibility with MainWindow.
    }

    public void dispose() {
        ecgWave.dispose();
        respWave.dispose();
    }

    public void setPatient(Patient patient) {
        int previousPatientId = this.patientId;
        this.patient = patient;
        if (patient != null) {
            setSelectedPatientId(patient.getId());
        } else {
            setSelectedPatientId(-1);
        }
        if (previousPatientId != this.patientId) {
            loadNotesForCurrentPatient();
        }
        refreshPatientPanel();
    }

    public void setSelectedPatientId(int patientId) {
        this.patientId = patientId;
        refreshPatientPanel();
    }

    public void setVitals(int hr, int rr, int spo2, int sys, int dia, double temp) {
        this.hr = Math.max(0, hr);
        this.rr = Math.max(0, rr);
        this.spo2 = Math.max(0, spo2);
        this.sys = Math.max(0, sys);
        this.dia = Math.max(0, dia);
        this.temp = temp;

        hrDevice.setData(LanguageManager.t("live.heartRate"), value(hr), "");
        rrDevice.setData(LanguageManager.t("live.respRate"), value(rr), "");
        spo2Device.setData(LanguageManager.t("live.spo2"), value(spo2), "");
        bpDevice.setData(LanguageManager.t("live.bloodPressure"), sys <= 0 || dia <= 0 ? "--/--" : sys + "/" + dia, "");
        tempDevice.setData(LanguageManager.t("pdf.temperature"), temp <= 0 ? "--.-" : String.format("%.1f", temp), "");
        statusDevice.setData(LanguageManager.t("digitalTwin.status"), statusText(), "");

        ecgWave.setVitals(hr, rr);
        respWave.setVitals(hr, rr);
        bodyPanel.setState(hr, rr, spo2, sys, dia, temp);
        debugBadge.setText(LanguageManager.t("digitalTwin.simBadge") + " id=" + (patientId < 0 ? "--" : patientId));
        repaint();
    }

    private void refreshPatientPanel() {
        String name = patient == null ? LanguageManager.t("digitalTwin.unknown") : patient.getName();
        String gender = patient == null ? "--" : safe(patient.getGender());
        String age = patient == null ? "--" : String.valueOf(patient.getAge());
        String id = patientId < 0 ? "--" : String.valueOf(patientId);
        demographics.setText("<html><b>" + escape(name) + "</b><br>"
                + LanguageManager.t("patient.gender") + ": " + escape(gender) + "<br>"
                + LanguageManager.t("patient.age") + ": " + escape(age) + "<br>"
                + LanguageManager.t("live.id") + ": " + escape(id) + "</html>");

        recordsPanel.removeAll();
        recordsPanel.add(chip("Hypertension", CARDIAC));
        recordsPanel.add(chip("Type 2 Diabetes", METABOLIC));
        recordsPanel.add(chip("Allergy", new Color(100, 116, 139)));
        recordsPanel.revalidate();
        recordsPanel.repaint();

        bodyPanel.setPatientLabels();
        debugBadge.setText(LanguageManager.t("digitalTwin.simBadge") + " id=" + (patientId < 0 ? "--" : patientId));
    }

    private JComponent chip(String text, Color dot) {
        JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        chip.setBackground(chipBg());
        chip.setOpaque(true);
        chip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(strokeColor()),
                new EmptyBorder(5, 8, 5, 8)
        ));
        JLabel d = new JLabel("●");
        d.setForeground(dot);
        JLabel t = new JLabel(text);
        t.setFont(new Font("Arial", Font.PLAIN, 12));
        t.setForeground(textColor());
        chip.add(d);
        chip.add(t);
        return chip;
    }

    private void addNote() {
        String text = noteText.getText().trim();
        if (text.isEmpty()) return;
        String level = String.valueOf(noteLevel.getSelectedItem());
        notes.addFirst(new Note(text, level, LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))));
        while (notes.size() > 4) notes.removeLast();
        saveNotesForCurrentPatient();
        noteText.setText("");
        renderNotes();
    }

    private void loadNotesForCurrentPatient() {
        notes.clear();
        if (patientId >= 0) {
            for (DigitalTwinNoteService.NoteEntry entry : noteService.load(patientId)) {
                if (entry != null && entry.text != null && !entry.text.isBlank()) {
                    notes.addLast(new Note(
                            entry.text,
                            entry.level == null || entry.level.isBlank() ? "Info" : entry.level,
                            entry.time == null || entry.time.isBlank() ? "--:--" : entry.time
                    ));
                }
            }
        }
        renderNotes();
    }

    private void saveNotesForCurrentPatient() {
        if (patientId < 0) return;
        List<DigitalTwinNoteService.NoteEntry> entries = new java.util.ArrayList<>();
        for (Note note : notes) {
            entries.add(new DigitalTwinNoteService.NoteEntry(note.text, note.level, note.time));
        }
        noteService.save(patientId, entries);
    }

    private void renderNotes() {
        notesPanel.removeAll();
        if (notes.isEmpty()) {
            JLabel empty = new JLabel(LanguageManager.t("digitalTwin.noNotes"));
            empty.setFont(new Font("Arial", Font.PLAIN, 13));
            notesPanel.add(empty);
        } else {
            for (Note note : notes) {
                JPanel card = new JPanel(new BorderLayout());
                card.setBackground(metricBg());
                card.setBorder(new EmptyBorder(6, 0, 6, 0));
                JLabel body = new JLabel("<html><b style='color:" + htmlColor(levelColor(note.level)) + "'>"
                        + escape(note.level) + "</b> <span style='font-size:10px'>" + note.time + "</span><br>"
                        + escape(note.text) + "</html>");
                body.setFont(new Font("Arial", Font.PLAIN, 12));
                body.setForeground(textColor());
                card.add(body, BorderLayout.CENTER);
                notesPanel.add(card);
            }
        }
        notesPanel.revalidate();
        notesPanel.repaint();
    }

    private String statusText() {
        int score = 0;
        if (spo2 > 0 && spo2 < 92) score++;
        if (hr > 120 || (hr > 0 && hr < 55)) score++;
        if (rr > 24 || (rr > 0 && rr < 10)) score++;
        if (temp > 38.5) score++;
        if (sys > 160 || (sys > 0 && sys < 95)) score++;
        if (score >= 3) return LanguageManager.t("digitalTwin.critical");
        if (score == 2) return LanguageManager.t("digitalTwin.warning");
        return LanguageManager.t("digitalTwin.stable");
    }

    private String value(int n) {
        return n <= 0 ? "--" : String.valueOf(n);
    }

    private String safe(String s) {
        return (s == null || s.isBlank()) ? "--" : s;
    }

    private String escape(String s) {
        return safe(s).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private Color levelColor(String level) {
        if ("Critical".equalsIgnoreCase(level)) return CARDIAC;
        if ("Warning".equalsIgnoreCase(level)) return WARNING;
        return RESP;
    }

    private String htmlColor(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private Color pageBg() {
        return darkMode ? DARK_PAGE_BG : PAGE_BG;
    }

    private Color panelBg() {
        return darkMode ? DARK_PANEL_BG : PANEL_BG;
    }

    private Color metricBg() {
        return darkMode ? DARK_PANEL_BLUE : PANEL_BLUE;
    }

    private Color chipBg() {
        return darkMode ? DARK_CHIP_BLUE : CHIP_BLUE;
    }

    private Color strokeColor() {
        return darkMode ? DARK_STROKE : STROKE;
    }

    private Color textColor() {
        return darkMode ? DARK_TEXT : TEXT;
    }

    private record Note(String text, String level, String time) {}

    private static class PatientOption {
        final Patient patient;

        PatientOption(Patient patient) {
            this.patient = patient;
        }

        @Override
        public String toString() {
            if (patient == null) return "--";
            return patient.getName() + " (ID: " + patient.getId() + ")";
        }
    }

    private static class DeviceCard extends JPanel {
        private final JLabel label = new JLabel();
        private final JLabel value = new JLabel();
        private final Color color;

        DeviceCard(Color color) {
            this.color = color;
            setLayout(new BorderLayout());
            boolean dark = new SettingManager().isDarkMode();
            setBackground(dark ? DARK_CHIP_BLUE : CHIP_BLUE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(dark ? DARK_STROKE : STROKE),
                    new EmptyBorder(12, 12, 12, 12)
            ));
            label.setFont(new Font("Arial", Font.PLAIN, 12));
            label.setForeground(dark ? DARK_TEXT : TEXT);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            value.setFont(new Font("Arial", Font.BOLD, 30));
            value.setForeground(color);
            value.setHorizontalAlignment(SwingConstants.CENTER);
            add(label, BorderLayout.NORTH);
            add(value, BorderLayout.CENTER);
        }

        void setData(String labelText, String valueText, String unit) {
            label.setText(labelText);
            value.setText(valueText + unit);
        }
    }

    private static class WavePanel extends JPanel {
        private final Color color;
        private final boolean ecg;
        private final Timer timer;
        private double phase;
        private int hr = 80;
        private int rr = 16;

        WavePanel(Color color, boolean ecg) {
            this.color = color;
            this.ecg = ecg;
            setOpaque(false);
            setPreferredSize(new Dimension(300, 90));
            timer = new Timer(ecg ? 40 : 80, e -> {
                phase += ecg ? Math.max(1, hr) / 25.0 : Math.max(1, rr) / 8.0;
                repaint();
            });
            timer.start();
        }

        void dispose() {
            timer.stop();
        }

        void setVitals(int hr, int rr) {
            this.hr = hr <= 0 ? 80 : hr;
            this.rr = rr <= 0 ? 16 : rr;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2f));
            Path2D path = new Path2D.Double();
            int mid = getHeight() / 2;
            path.moveTo(0, mid);
            for (int x = 0; x < getWidth(); x++) {
                double y = ecg ? ecgSample((x + phase) % 80, mid) : mid + Math.sin((x + phase) * 0.04) * 18;
                path.lineTo(x, y);
            }
            g2.draw(path);
            g2.dispose();
        }

        private double ecgSample(double x, int mid) {
            double amp = 28 + (Math.min(140, hr) - 60) / 10.0;
            double qrs = Math.exp(-Math.pow(x - 30, 2) / 24) * amp;
            double s = -Math.exp(-Math.pow(x - 36, 2) / 34) * (amp / 3);
            double p = Math.exp(-Math.pow(x - 18, 2) / 72) * (amp / 8);
            double t = Math.exp(-Math.pow(x - 55, 2) / 60) * (amp / 6);
            return mid - (qrs + s + p + t);
        }
    }

    private static class BodyPanel extends JPanel {
        private BufferedImage bodyImage;
        private int hr;
        private int rr;
        private int spo2;
        private int sys;
        private int dia;
        private double temp;

        BodyPanel() {
            setOpaque(false);
            try {
                bodyImage = ImageIO.read(BodyPanel.class.getResource("/digital_twin/body.png"));
            } catch (Exception ignored) {}
        }

        void setState(int hr, int rr, int spo2, int sys, int dia, double temp) {
            this.hr = hr;
            this.rr = rr;
            this.spo2 = spo2;
            this.sys = sys;
            this.dia = dia;
            this.temp = temp;
            repaint();
        }

        void setPatientLabels() {
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            int imgH = Math.min(760, h);
            int imgW = Math.min(520, Math.round(imgH * 0.62f));
            if (imgW > w) {
                imgW = w;
                imgH = Math.round(imgW / 0.62f);
            }
            int x = (w - imgW) / 2;
            int y = (h - imgH) / 2;

            if (bodyImage != null) {
                g2.drawImage(bodyImage, x, y, imgW, imgH, null);
            } else {
                drawFallbackBody(g2, x, y, imgW, imgH);
            }

            double sx = imgW / 660.0;
            double sy = imgH / 1066.0;
            Shape oldClip = g2.getClip();
            AffineTransform oldTransform = g2.getTransform();
            g2.translate(x, y);
            g2.scale(sx, sy);

            g2.setColor(new Color(RESP.getRed(), RESP.getGreen(), RESP.getBlue(), 90));
            g2.fill(new Ellipse2D.Double(200, 190, 170, 270));
            g2.fill(new Ellipse2D.Double(290, 190, 170, 270));

            g2.setColor(new Color(CARDIAC.getRed(), CARDIAC.getGreen(), CARDIAC.getBlue(), 120));
            Path2D heart = new Path2D.Double();
            heart.moveTo(330, 280);
            heart.curveTo(305, 245, 260, 245, 260, 290);
            heart.curveTo(260, 340, 330, 380, 330, 380);
            heart.curveTo(330, 380, 400, 340, 400, 290);
            heart.curveTo(400, 245, 355, 245, 330, 280);
            heart.closePath();
            g2.fill(heart);

            drawCondition(g2, 330, 360, 80, 360, CARDIAC, LanguageManager.t("live.hrShort") + " " + (hr <= 0 ? "--" : hr));
            drawCondition(g2, 300, 260, 80, 220, RESP, LanguageManager.t("live.respShort") + " " + (rr <= 0 ? "--" : rr));
            drawCondition(g2, 330, 600, 80, 600, METABOLIC, "SpO2 " + (spo2 <= 0 ? "--" : spo2));

            g2.setTransform(oldTransform);
            g2.setClip(oldClip);
            g2.dispose();
        }

        private void drawFallbackBody(Graphics2D g2, int x, int y, int w, int h) {
            g2.setColor(new Color(226, 232, 240));
            g2.fillOval(x + w / 2 - 42, y + 20, 84, 84);
            g2.fillRoundRect(x + w / 2 - 82, y + 130, 164, 300, 80, 80);
        }

        private void drawCondition(Graphics2D g2, int x1, int y1, int x2, int y2, Color color, String text) {
            g2.setColor(color);
            Stroke old = g2.getStroke();
            g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10, new float[]{4, 4}, 0));
            g2.drawLine(x1, y1, x2, y2);
            g2.setStroke(old);
            boolean dark = new SettingManager().isDarkMode();
            g2.setColor(dark ? DARK_PANEL_BG : Color.WHITE);
            g2.fillRoundRect(10, y2 - 20, 220, 28, 8, 8);
            g2.setColor(color);
            g2.setFont(new Font("Arial", Font.BOLD, 13));
            g2.drawString(text, 20, y2);
        }
    }
}
