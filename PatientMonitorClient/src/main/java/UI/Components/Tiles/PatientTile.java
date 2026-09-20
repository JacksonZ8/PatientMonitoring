package UI.Components.Tiles;

import Models.Vitals.LiveVitals;
import Models.Patients.Patient;
import Services.PatientDischargeService;
import Services.AlertManager;
import UI.Components.RoundedButton;
import UI.MainWindow;
import UI.Components.StickyButton;
import UI.Pages.HomePage;
import Utilities.LanguageManager;
import Utilities.SettingManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Collections;

public class PatientTile extends BaseTile {

    private final Patient patient;

    // UI labels (dynamic)
    private JLabel hrLabel;
    private JLabel tempLabel;
    private JLabel spo2Label;
    private JLabel rrLabel;

    // UI labels (static)
    private JLabel genderLabel;
    private JLabel ageLabel;
    private JLabel bpLabel;

    private Timer refreshTimer;
    private Timer flashTimer;
    private boolean flashOn = false;
    private final boolean darkMode = new SettingManager().isDarkMode();

    private static final Color BG_NORMAL = Color.WHITE;
    private static final Color BG_FLASH = new Color(255, 240, 240);
    private static final Color BG_DARK = new Color(32, 35, 41);
    private static final Color BG_DARK_FLASH = new Color(58, 42, 47);
    private static final Color TEXT_LIGHT = new Color(31, 41, 55);
    private static final Color MUTED_LIGHT = new Color(75, 85, 99);
    private static final Color TEXT_DARK = new Color(238, 241, 245);
    private static final Color MUTED_DARK = new Color(188, 196, 207);

    // styling
    private static final Color BORDER_NORMAL = new Color(220, 225, 230);
    private static final Color BORDER_DARK = new Color(70, 78, 91);
    private static final Color BORDER_WARN = new Color(245, 158, 11);
    private static final Color BORDER_DANGER = new Color(220, 38, 38);

    // Creates a UI tile that displays a patient's details and live vitals with alert highlighting.
    public PatientTile(Patient patient, MainWindow window, HomePage homePage) {
        super(370, 320, 30, true);
        this.patient = patient;

        setLayout(new BorderLayout());
        setBackground(tileBg());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(normalBorder(), 2),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        // top: name + stick
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel nameLabel = new JLabel(patient.getName());
        nameLabel.setFont(new Font("Arial", Font.BOLD, 20));
        nameLabel.setForeground(primaryText());
        top.add(nameLabel, BorderLayout.WEST);

        StickyButton star = new StickyButton(patient, homePage);
        top.add(star, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);

        // center: dynamic vitals (left) + static info (right)
        JPanel center = new JPanel(new GridLayout(1, 2, 15, 0));
        center.setOpaque(false);

        center.add(buildDynamicVitalsPanel());
        center.add(buildStaticInfoPanel(patient));

        add(center, BorderLayout.CENTER);

        // bottom: discharge button
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);

        RoundedButton dischargeBtn = new RoundedButton(LanguageManager.t("patient.discharge"));
        dischargeBtn.setColors(
                new Color(190, 55, 55),
                new Color(220, 70, 70),
                new Color(150, 40, 40),
                Color.WHITE
        );
        dischargeBtn.addActionListener(e -> {

            String reason = JOptionPane.showInputDialog(
                    this,
                    LanguageManager.t("patient.dischargePrompt"),
                    LanguageManager.t("patient.dischargeTitle"),
                    JOptionPane.PLAIN_MESSAGE
            );

            if (reason == null || reason.trim().isEmpty()) return;

            // stop any active alert sound for this patient first
            AlertManager.getInstance().updateAlert(
                    patient,
                    LiveVitals.VitalsSeverity.NORMAL,
                    Collections.emptyList()
            );

            // stop timers before discharge (optional safety)
            stopRefreshTimer();

            PatientDischargeService.discharge(patient, reason);
        });

        bottom.add(dischargeBtn);
        add(bottom, BorderLayout.SOUTH);

        // click to show detail
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isRightMouseButton(e)) {
                    window.showLiveMonitoring(patient);
                }
            }
        });

        // start live updates from shared LiveVitals
        startLiveRefresh(patient);

        // If HomePage refresh removes this tile, stop timers + clear alert to avoid "ghost" beeps
        addHierarchyListener(ev -> {
            if ((ev.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0) {
                if (!isDisplayable()) {
                    stopRefreshTimer();
                }
            }
        });
    }

    // Builds the left-side panel that displays continuously updated vital labels.
    private JPanel buildDynamicVitalsPanel() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(LanguageManager.t("live.vitals"));
        title.setFont(new Font("Arial", Font.BOLD, 14));
        title.setForeground(primaryText());
        p.add(title);
        p.add(Box.createVerticalStrut(10));

        hrLabel = label(LanguageManager.t("live.hrShort") + ": -- bpm");
        tempLabel = label(LanguageManager.t("live.tempShort") + ": -- °C");
        spo2Label = label(LanguageManager.t("live.spo2") + ": -- %");
        rrLabel = label(LanguageManager.t("live.respShort") + ": -- /min");

        p.add(hrLabel);
        p.add(Box.createVerticalStrut(6));
        p.add(tempLabel);
        p.add(Box.createVerticalStrut(6));
        p.add(spo2Label);
        p.add(Box.createVerticalStrut(6));
        p.add(rrLabel);

        return p;
    }

    // Builds the right side panel that displays static patient demographic information.
    private JPanel buildStaticInfoPanel(Patient patient) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(LanguageManager.t("live.patientInfo"));
        title.setFont(new Font("Arial", Font.BOLD, 14));
        title.setForeground(primaryText());
        p.add(title);
        p.add(Box.createVerticalStrut(10));

        genderLabel = label(LanguageManager.t("patient.gender") + ": " + safe(patient.getGender()));
        ageLabel = label(LanguageManager.t("patient.age") + ": " + patient.getAge());
        bpLabel = label(LanguageManager.t("live.bpShort") + ": " + safe(patient.getBloodPressure()));

        p.add(genderLabel);
        p.add(Box.createVerticalStrut(6));
        p.add(ageLabel);
        p.add(Box.createVerticalStrut(6));
        p.add(bpLabel);

        return p;
    }

    // Starts a timer that refreshes UI labels from shared LiveVitals once per second.
    private void startLiveRefresh(Patient patient) {
        LiveVitals live = LiveVitals.getShared(patient.getId(), patient.getBloodPressure());

        refreshTimer = new Timer(1000, e -> {
            // read shared simulated vitals
            int hr = (int) Math.round(live.getHeartRate());
            double temp = live.getTemperature();
            int spo2 = (int) Math.round(live.getSpO2());
            int rr = (int) Math.round(live.getRespRate());

            hrLabel.setText(LanguageManager.t("live.hrShort") + ": " + hr + " bpm");
            tempLabel.setText(String.format("%s: %.1f °C", LanguageManager.t("live.tempShort"), temp));
            spo2Label.setText(LanguageManager.t("live.spo2") + ": " + spo2 + " %");
            rrLabel.setText(LanguageManager.t("live.respShort") + ": " + rr + " /min");

            // BP may be updated by simulator; keep UI synced
            bpLabel.setText(LanguageManager.t("live.bpShort") + ": " + safe(live.getBloodPressure()));


            // severity highlight + flash
            LiveVitals.VitalsSeverity sev = live.getVitalsSeverity();
            Color borderColor =
                    (sev == LiveVitals.VitalsSeverity.DANGER) ? BORDER_DANGER :
                            (sev == LiveVitals.VitalsSeverity.WARNING) ? BORDER_WARN :
                                    normalBorder();

            // update global alert manager (handles beep cadence + history, de-spammed internally)
            AlertManager.getInstance().updateAlert(
                    patient,
                    sev,
                    live.getAlertCauses(sev)
            );

            if (sev == LiveVitals.VitalsSeverity.DANGER) {
                startFlash(BORDER_DANGER, normalBorder());
            } else if (sev == LiveVitals.VitalsSeverity.WARNING) {
                startFlash(BORDER_WARN, normalBorder());
            } else {
                stopFlash(borderColor);
            }
        });

        refreshTimer.start();
    }

    // Stops all timers and resets alert state to prevent lingering UI effects or sounds.
    private void stopRefreshTimer() {
        if (refreshTimer != null) {
            refreshTimer.stop();
            refreshTimer = null;
        }
        if (flashTimer != null) {
            flashTimer.stop();
            flashTimer = null;
        }
        flashOn = false;
        setBackground(tileBg());

        // Clear this patient's alert state when we stop updating (prevents lingering WARNING/DANGER in AlertManager)
        if (patient != null) {
            AlertManager.getInstance().updateAlert(
                    patient,
                    LiveVitals.VitalsSeverity.NORMAL,
                    Collections.emptyList()
            );
        }
    }

    public void dispose() {
        stopRefreshTimer();
    }

    // Starts a flashing border to indicate warning or danger.
    private void startFlash(Color onColor, Color offColor) {
        // If already flashing, keep current timer but allow color to change
        if (flashTimer != null && flashTimer.isRunning()) {
            // Update immediately to new colors by restarting the timer
            flashTimer.stop();
            flashTimer = null;
        }

        flashOn = false;
        flashTimer = new Timer(400, e -> {
            flashOn = !flashOn;

            Color c = flashOn ? onColor : offColor;

            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(c, 2),
                    BorderFactory.createEmptyBorder(20, 20, 20, 20)
            ));

            // Only flash background for DANGER (red). For WARNING keep background stable.
            if (onColor.equals(BORDER_DANGER)) {
                setBackground(flashOn ? flashBg() : tileBg());
            } else {
                setBackground(tileBg());
            }
            repaint();
        });
        flashTimer.start();
    }

    // Stops any flashing behaviour and applies the provided border colour.
    private void stopFlash(Color borderColor) {
        if (flashTimer != null) {
            flashTimer.stop();
            flashTimer = null;
        }
        flashOn = false;

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, 2),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        setBackground(tileBg());
        repaint();
    }

    // Creates a consistently styled label used within the tile.
    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Arial", Font.PLAIN, 16));
        l.setForeground(mutedText());
        return l;
    }

    private Color tileBg() {
        return darkMode ? BG_DARK : BG_NORMAL;
    }

    private Color flashBg() {
        return darkMode ? BG_DARK_FLASH : BG_FLASH;
    }

    private Color normalBorder() {
        return darkMode ? BORDER_DARK : BORDER_NORMAL;
    }

    private Color primaryText() {
        return darkMode ? TEXT_DARK : TEXT_LIGHT;
    }

    private Color mutedText() {
        return darkMode ? MUTED_DARK : MUTED_LIGHT;
    }

    // Returns a safe placeholder string when a value is null or blank.
    private String safe(String s) {
        return (s == null || s.isBlank()) ? "--" : s;
    }
}
