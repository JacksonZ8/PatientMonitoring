package Services;

import Models.Vitals.AlertRecord;
import Models.Vitals.LiveVitals;
import Models.Patients.Patient;

import java.time.Instant;
import java.util.*;
import java.util.List;

public class AlertManager {
    private static final AlertManager INSTANCE = new AlertManager();

    public static AlertManager getInstance() {
        return INSTANCE;
    }

    private final Map<Integer, LiveVitals.VitalsSeverity> active = new HashMap<>();
    private final List<AlertRecord> history = new ArrayList<>();

    private javax.swing.Timer soundTimer;
    private LiveVitals.VitalsSeverity currentlyPlaying = LiveVitals.VitalsSeverity.NORMAL;

    //NEW: gate to prevent beeps (and prevent updateAlert from re-arming sound) while logged out
    private boolean alertsEnabled = false;

    private AlertManager() {}

    public synchronized void updateAlert(Patient patient, LiveVitals.VitalsSeverity newSev, List<String> causes) {
        //If alerts disabled (logged out), ignore updates so background timers can't restart sound
        if (!alertsEnabled) return;

        int id = patient.getId();
        LiveVitals.VitalsSeverity oldSev = active.getOrDefault(id, LiveVitals.VitalsSeverity.NORMAL);

        if (newSev == LiveVitals.VitalsSeverity.NORMAL) {
            active.remove(id);
        } else {
            active.put(id, newSev);
        }

        // Log only if severity changed (prevents spam every time blink timer runs)
        if (newSev != oldSev) {
            if (newSev != LiveVitals.VitalsSeverity.NORMAL) {
                history.add(new AlertRecord(id, patient.getName(), newSev, causes, Instant.now()));
            } else {
                // optional log resolved events
                // history.add(new AlertRecord(id, patient.getName(), Patient.VitalsSeverity.NORMAL, List.of("Alert resolved"), Instant.now()));
            }
        }

        refreshSound();
    }

    public synchronized List<AlertRecord> getHistory() {
        return new ArrayList<>(history);
    }

    public synchronized void clearHistory() {
        history.clear();
    }

    //Call on logout
    public synchronized void disableAlerts() {
        alertsEnabled = false;
        active.clear();
        stopSoundTimer();
        currentlyPlaying = LiveVitals.VitalsSeverity.NORMAL;
    }

    // Call after successful login
    public synchronized void enableAlerts() {
        alertsEnabled = true;
    }

    private void refreshSound() {
        if (!alertsEnabled) return;

        LiveVitals.VitalsSeverity highest = getHighestActiveSeverity();

        if (highest == currentlyPlaying) return; // no change

        stopSoundTimer();

        currentlyPlaying = highest;

        if (highest == LiveVitals.VitalsSeverity.DANGER) {
            // fast repeating beep (e.g., every 300ms)
            soundTimer = new javax.swing.Timer(300, e -> playBeep());
            soundTimer.start();
        } else if (highest == LiveVitals.VitalsSeverity.WARNING) {
            // slower repeating beep (e.g., every 700ms)
            soundTimer = new javax.swing.Timer(700, e -> playBeep());
            soundTimer.start();
        }
        // normal no timer
    }

    private LiveVitals.VitalsSeverity getHighestActiveSeverity() {
        boolean anyDanger = active.values().stream().anyMatch(s -> s == LiveVitals.VitalsSeverity.DANGER);
        if (anyDanger) return LiveVitals.VitalsSeverity.DANGER;

        boolean anyWarning = active.values().stream().anyMatch(s -> s == LiveVitals.VitalsSeverity.WARNING);
        if (anyWarning) return LiveVitals.VitalsSeverity.WARNING;

        return LiveVitals.VitalsSeverity.NORMAL;
    }

    private void stopSoundTimer() {
        if (soundTimer != null) {
            soundTimer.stop();
            soundTimer = null;
        }
    }

    private void playBeep() {
        if (!alertsEnabled) return;

        try {
            var is = getClass().getResourceAsStream("/sounds/alert.wav");
            if (is == null) return;

            var audio = javax.sound.sampled.AudioSystem.getAudioInputStream(is);
            var clip = javax.sound.sampled.AudioSystem.getClip();
            clip.open(audio);
            clip.start(); // one shot beep
        } catch (Exception ignored) {}
    }
}
