package Models;

public class PatientRecord {
    private String patientName;
    private String recordId;
    private String diagnosis;
    private String date;

    public String getPatientName() { return patientName; }
    public String getRecordId() { return recordId; }
    public String getDiagnosis() { return diagnosis; }
    public String getDate() { return date; }

    public void setPatientName(String patientName) { this.patientName = patientName; }
    public void setRecordId(String recordId) { this.recordId = recordId; }
    public void setDiagnosis(String diagnosis) { this.diagnosis = diagnosis; }
    public void setDate(String date) { this.date = date; }
}
