package Services;

import Models.Vitals.LiveVitals;
import java.util.Random;

// Service that simulates realistic patient vital sign changes
public class PatientSimulatorService {

    // Reference to live vitals for this patient
    private final LiveVitals vitals;
    // Random generator for noise and events
    private final Random rand = new Random();

    // Remaining duration of an active danger episode (in ticks)
    private int dangerTicksRemaining = 0;
    // Type of danger episode: 0=HR, 1=Temp, 2=BP
    private int episodeType = 0; // 0=HR, 1=Temp, 2=BP

    // Create simulator for a specific patient's vitals
    public PatientSimulatorService(LiveVitals vitals) {
        this.vitals = vitals;
    }

    // Update vitals each simulation step
    public void update(double dt) {

        // Baseline vitals with small noise
        double hrBase   = 76.60 + rand.nextGaussian() * 3.5;
        double tempBase = 36.91 + rand.nextGaussian() * 0.12;
        int sysBase     = (int) Math.round(123.83 + rand.nextGaussian() * 6.0);
        int diaBase     = (int) Math.round(76.39  + rand.nextGaussian() * 4.0);

        // Stable SpO2 and respiratory rate
        vitals.setSpO2(98 + rand.nextGaussian() * 0.6);
        vitals.setRespRate(16 + rand.nextGaussian() * 0.5);

        // Start a danger episode with a low probability if none active
        if (dangerTicksRemaining <= 0) {
            // ~0.5% chance per second
            if (rand.nextDouble() < 0.005) {
                dangerTicksRemaining = 4 + rand.nextInt(7); // 4–10 seconds of DANGER
                episodeType = rand.nextInt(3);
            }
        } else {
            // Decrease remaining duration of danger episode
            dangerTicksRemaining--;
        }

        // Apply danger spikes or baseline vitals
        if (dangerTicksRemaining > 0) {
            if (episodeType == 0) {
                // Heart rate danger spike
                vitals.setHeartRate(118 + rand.nextGaussian() * 4);
                vitals.setTemperature(tempBase);
            } else if (episodeType == 1) {
                // Temperature danger spike
                vitals.setHeartRate(90 + rand.nextGaussian() * 5);
                vitals.setTemperature(38.6 + rand.nextGaussian() * 0.15);
            } else {
                // Blood pressure danger spike
                vitals.setHeartRate(hrBase);
                vitals.setTemperature(tempBase);
                sysBase = (int) Math.round(165 + rand.nextGaussian() * 6);
                diaBase = (int) Math.round(105 + rand.nextGaussian() * 5);
            }
        } else {
            // Normal vitals outside danger episodes
            vitals.setHeartRate(hrBase);
            vitals.setTemperature(tempBase);
        }

        // Clamp blood pressure to physiological limits
        sysBase = Math.max(80, Math.min(180, sysBase));
        diaBase = Math.max(50, Math.min(120, diaBase));
        // Update blood pressure string
        vitals.setBloodPressure(sysBase + "/" + diaBase);
    }
}
