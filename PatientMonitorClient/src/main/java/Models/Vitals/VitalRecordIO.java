package Models.Vitals;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VitalRecordIO {

    private static final List<VitalRecord> records = new ArrayList<>();

    // Appends a new minute averaged vital record.
    public static void append(VitalRecord r) {
        records.add(r);
    }

    // Returns a copy of all stored vital records.
    public static List<VitalRecord> loadAll() {
        return new ArrayList<>(records);
    }

    // Returns vital records for a patient recorded within the last specified number of hours
    public static List<VitalRecord> loadLastHours(int patientId, int hours) {

        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);

        return records.stream()
                .filter(r -> r.getPatientId() == patientId)
                .filter(r -> r.getTimestamp().isAfter(cutoff))
                .toList();
    }

    // Removes all stored vital records associated with a given patient.
    public static void clearForPatient(int patientId) {
        records.removeIf(r -> r.getPatientId() == patientId);
    }
}
