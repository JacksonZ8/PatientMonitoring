package Models.Vitals;

import java.time.LocalDateTime;

public class VitalSummary24h {

    private final LocalDateTime start;
    private final LocalDateTime end;

    private final double meanHR;
    private final double meanRR;
    private final double meanTemp;
    private final double meanSpO2;

    // Creates a 24-hour summary of averaged vital signs over a specified time window.
    public VitalSummary24h(
            LocalDateTime start,
            LocalDateTime end,
            double meanHR,
            double meanRR,
            double meanTemp,
            double meanSpO2
    ) {
        this.start = start;
        this.end = end;
        this.meanHR = meanHR;
        this.meanRR = meanRR;
        this.meanTemp = meanTemp;
        this.meanSpO2 = meanSpO2;
    }

    // Returns the start time of the 24 hours summary window.
    public LocalDateTime getStart() { return start; }
    // Returns the end time of the 24 hours summary window.
    public LocalDateTime getEnd() { return end; }

    // Returns the mean heart rate over the summary period
    public double getMeanHR() { return meanHR; }
    // Returns the mean respiratory rate over the summary period.
    public double getMeanRR() { return meanRR; }
    // Returns the mean temperature over the summary period.
    public double getMeanTemp() { return meanTemp; }
    // Returns the mean oxygen saturation (SpO₂) over the summary period.
    public double getMeanSpO2() { return meanSpO2; }
}
