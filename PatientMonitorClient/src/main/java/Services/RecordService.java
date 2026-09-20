package Services;

import Models.Patients.PatientRecord;
import Models.Patients.PatientRecordIO;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class RecordService {
    public List<PatientRecord> loadRecords() {
        return PatientRecordIO.loadRecords();
    }

    public PatientRecordIO.LoadResult loadRecordsWithSource() {
        return PatientRecordIO.loadRecordsWithSource();
    }

    public void saveRecords(List<PatientRecord> records) {
        PatientRecordIO.saveRecords(records);
    }

    public void append(PatientRecord record) {
        List<PatientRecord> records = new ArrayList<>(loadRecords());
        records.add(record);
        saveRecords(records);
    }

    public List<PatientRecord> importCsv(JFrame parent) {
        return PatientRecordIO.importCSV(parent);
    }
}
