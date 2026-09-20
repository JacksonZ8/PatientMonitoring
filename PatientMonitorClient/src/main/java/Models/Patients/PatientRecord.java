package Models.Patients;

import java.io.Serializable;

public class PatientRecord implements Serializable {

    private String patientName;
    private String recordId;
    private String diagnosis;
    private String date;

    // Creates a patient record containing identifying and diagnostic information
    public PatientRecord(String patientName, String recordId, String diagnosis, String date) {
        this.patientName = patientName;
        this.recordId = recordId;
        this.diagnosis = diagnosis;
        this.date = date;
    }

    // Getters
    public String getPatientName() { return patientName; }
    public String getRecordId() { return recordId; }
    public String getDiagnosis() { return diagnosis; }
    public String getDate() { return date; }

    // Checks whether this record matches the given search query.
    public boolean matches(String q) {
        q = q.toLowerCase();
        return patientName.toLowerCase().contains(q)
                || recordId.toLowerCase().contains(q)
                || diagnosis.toLowerCase().contains(q);
    }

    // Returns a string representation of the patient record.
    @Override
    public String toString() {
        return patientName + " (" + recordId + ")";
    }
}