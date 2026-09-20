package Models.Vitals;

import java.time.LocalDateTime;

public class VitalRecord {

    private final int patientId;
    private final LocalDateTime timestamp;

    private final double avgHeartRate;
    private final double avgRespRate;
    private final double avgTemperature;
    private final double avgSpO2;

    // creates a snapshot record of averaged vital signs for a patient at the current time.
    public VitalRecord(int patientId, double hr, double rr, double temp, double spo2) {
        this.patientId = patientId;
        this.avgHeartRate = hr;
        this.avgRespRate = rr;
        this.avgTemperature = temp;
        this.avgSpO2 = spo2;
        this.timestamp = LocalDateTime.now();
    }

    // returns the patient ID associated with this vital record.
    public int getPatientId() { return patientId; }
    // returns the timestamp indicating when this record was created.
    public LocalDateTime getTimestamp() { return timestamp; }
    // returns the average heart rate value stored in this record.
    public double getAvgHeartRate() { return avgHeartRate; }
    // returns the average respiratory rate value stored in this record.
    public double getAvgRespRate() { return avgRespRate; }
    // returns the average temperature value stored in this record.
    public double getAvgTemperature() { return avgTemperature; }
    // returns the average oxygen saturation value stored in this record.
    public double getAvgSpO2() { return avgSpO2; }
}
