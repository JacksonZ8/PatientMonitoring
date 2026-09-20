package Models.Vitals;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Immutable alert history record
// Stores the patient id/name, the severity (from LiveVitals), the human-readable causes, and a timestamp
public class AlertRecord {

    private final int patientId;
    private final String patientName;
    private final LiveVitals.VitalsSeverity severity;
    private final List<String> causes;
    private final Instant timestamp;

    // creates an immutable alert record for a patient at a specific point in time.
    public AlertRecord(int patientId, String patientName, LiveVitals.VitalsSeverity severity, List<String> causes, Instant timestamp) {
        this.patientId = patientId;
        this.patientName = patientName;
        this.severity = (severity == null) ? LiveVitals.VitalsSeverity.NORMAL : severity;
        this.causes = (causes == null) ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(causes));
        this.timestamp = (timestamp == null) ? Instant.now() : timestamp;
    }

    // returns the unique identifier of the patient associated with this alert.
    public int getPatientId() {
        return patientId;
    }

    // returns the name of the patient associated with this alert.
    public String getPatientName() {
        return patientName;
    }

    // returns the severity level of this alert.
    public LiveVitals.VitalsSeverity getSeverity() {
        return severity;
    }

    // returns an unmodifiable list of human-readable causes for this alert.
    public List<String> getCauses() {
        return causes;
    }

    // returns the timestamp indicating when this alert was recorded.
    public Instant getTimestamp() {
        return timestamp;
    }

    // returns a string representation of this alert record for debugging or logging.
    @Override
    public String toString() {
        return "AlertRecord{" +
                "patientId=" + patientId +
                ", patientName='" + patientName + '\'' +
                ", severity=" + severity +
                ", causes=" + causes +
                ", timestamp=" + timestamp +
                '}';
    }
}
