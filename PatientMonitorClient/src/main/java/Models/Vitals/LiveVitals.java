package Models.Vitals;

import Services.PatientSimulatorService;
import Utilities.LanguageManager;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;

public class LiveVitals {

    // Global shared registry
        private static final ConcurrentHashMap<Integer, LiveVitals> SHARED = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, PatientSimulatorService> SIMS = new ConcurrentHashMap<>();

    private static final ScheduledExecutorService EXEC = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "LiveVitals-SimLoop");
        t.setDaemon(true);
        return t;
    });

    private static volatile boolean loopStarted = false;

     // Get (or create) a globally shared LiveVitals instance for a patientId.
     // This also ensures there is exactly one simulator loop updating all shared vitals once per second.

    public static LiveVitals getShared(int patientId, String baselineBloodPressure) {
        LiveVitals v = SHARED.computeIfAbsent(patientId, LiveVitals::new);

        // Align baseline BP if provided
        if (baselineBloodPressure != null && !baselineBloodPressure.isBlank()) {
            v.setBloodPressure(baselineBloodPressure);
        }

        // Ensure simulator exists for this patient
        SIMS.computeIfAbsent(patientId, id -> new PatientSimulatorService(v));

        // Start one global loop (once)
        if (!loopStarted) {
            synchronized (LiveVitals.class) {
                if (!loopStarted) {
                    loopStarted = true;
                    EXEC.scheduleAtFixedRate(() -> {
                        try {
                            for (PatientSimulatorService sim : SIMS.values()) {
                                sim.update(1);
                            }
                        } catch (Exception ignored) {
                        }
                    }, 0, 1, TimeUnit.SECONDS);
                }
            }
        }

        return v;
    }

    public static void removeShared(int patientId) {
        SHARED.remove(patientId);
        SIMS.remove(patientId);
    }

    private final int patientId;

    private double heartRate = 75;
    private double respRate = 16;
    private double temperature = 36.9;
    private double spo2 = 98;
    private String bloodPressure = "124/76";

    public LiveVitals(int patientId) {
        this.patientId = patientId;
    }

    public int getPatientId() { return patientId; }

    public double getHeartRate() { return heartRate; }
    public void setHeartRate(double heartRate) { this.heartRate = heartRate; }

    public double getRespRate() { return respRate; }
    public void setRespRate(double respRate) { this.respRate = respRate; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public double getSpO2() { return spo2; }
    public void setSpO2(double spo2) { this.spo2 = spo2; }

    public String getBloodPressure() { return bloodPressure; }
    public void setBloodPressure(String bloodPressure) {
        this.bloodPressure = bloodPressure;
    }

    // any vital abnormal (warning-level thresholds).
    public boolean hasAbnormalVitals() {
        return isHeartRateAbnormal() || isTemperatureAbnormal() || isBloodPressureAbnormal();
    }

    // warning-level threshold.
    public List<String> getWarningCauses() {
        List<String> causes = new ArrayList<>();

        if (isHeartRateAbnormal()) {
            causes.add(LanguageManager.t("alertCause.hrWarning") + " (HR=" + String.format("%.0f", heartRate) + ", " + LanguageManager.t("alertCause.normal") + " 66–88)");
        }
        if (isTemperatureAbnormal()) {
            causes.add(LanguageManager.t("alertCause.tempWarning") + " (Temp=" + String.format("%.1f", temperature) + ", " + LanguageManager.t("alertCause.normal") + " 36.5–37.4)");
        }
        if (isBloodPressureAbnormal()) {
            causes.add(LanguageManager.t("alertCause.bpWarning") + " (BP=" + bloodPressure + ", " + LanguageManager.t("alertCause.normal") + " 109–139 / 66–86)");
        }

        return causes;
    }

    // danger-level threshold.
    public List<String> getDangerCauses() {
        List<String> causes = new ArrayList<>();

        if (heartRate < 50 || heartRate > 110) {
            causes.add(LanguageManager.t("alertCause.hrDanger") + " (HR=" + String.format("%.0f", heartRate) + ", " + LanguageManager.t("alertCause.danger") + " <50 or >110)");
        }
        if (temperature < 35.5 || temperature > 38.5) {
            causes.add(LanguageManager.t("alertCause.tempDanger") + " (Temp=" + String.format("%.1f", temperature) + ", " + LanguageManager.t("alertCause.danger") + " <35.5 or >38.5)");
        }

        if (bloodPressure != null && !bloodPressure.isEmpty()) {
            try {
                String[] parts = bloodPressure.split("/");
                int sys = Integer.parseInt(parts[0].trim());
                int dia = Integer.parseInt(parts[1].trim());
                if (sys < 90 || sys > 160 || dia < 55 || dia > 110) {
                    causes.add(LanguageManager.t("alertCause.bpDanger") + " (BP=" + bloodPressure + ", " + LanguageManager.t("alertCause.sysDiaDanger") + ")");
                }
            } catch (Exception ignored) {
            }
        }

        return causes;
    }

    // return causes according to severity.
    public List<String> getAlertCauses(VitalsSeverity sev) {
        if (sev == VitalsSeverity.DANGER) return getDangerCauses();
        if (sev == VitalsSeverity.WARNING) return getWarningCauses();
        return new ArrayList<>();
    }

    public enum VitalsSeverity { NORMAL, WARNING, DANGER }

    // Checks whether the heart rate is outside normal limits.
    public boolean isHeartRateAbnormal() {
        return heartRate < 65.56 || heartRate > 87.64;
    }

    // checks whether the temperature is outside normal limits.
    public boolean isTemperatureAbnormal() {
        return temperature < 36.46 || temperature > 37.36;
    }

    // checks whether the blood pressure is outside normal limits.
    public boolean isBloodPressureAbnormal() {
        if (bloodPressure == null || bloodPressure.isEmpty()) return false;
        try {
            String[] parts = bloodPressure.split("/");
            int sys = Integer.parseInt(parts[0].trim());
            int dia = Integer.parseInt(parts[1].trim());
            return sys < 108 || sys > 139 || dia < 66 || dia > 86;
        } catch (Exception e) {
            return false;
        }
    }

    // determines the overall severity level based on all vital signs.
    public VitalsSeverity getVitalsSeverity() {
        boolean dangerHR = heartRate < 50 || heartRate > 110;
        boolean dangerTemp = temperature < 35.5 || temperature > 38.5;

        boolean dangerBP = false;
        if (bloodPressure != null && !bloodPressure.isEmpty()) {
            try {
                String[] parts = bloodPressure.split("/");
                int sys = Integer.parseInt(parts[0].trim());
                int dia = Integer.parseInt(parts[1].trim());
                dangerBP = (sys < 90 || sys > 160 || dia < 55 || dia > 110);
            } catch (Exception ignored) {}
        }

        if (dangerHR || dangerTemp || dangerBP) return VitalsSeverity.DANGER;

        return hasAbnormalVitals() ? VitalsSeverity.WARNING : VitalsSeverity.NORMAL;
    }

    // clears all shared vitals and simulator instances.
    public static void clearAllShared() {
        SHARED.clear();
        SIMS.clear();
    }

}
