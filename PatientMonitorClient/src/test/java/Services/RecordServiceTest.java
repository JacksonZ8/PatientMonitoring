package Services;

import Models.Patients.PatientRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RecordServiceTest {
    private static final String TEST_HOME =
            System.getProperty("java.io.tmpdir") + "/record_service_test";

    @BeforeEach
    void setup() {
        System.setProperty("user.home", TEST_HOME);
        File dir = new File(TEST_HOME + "/.patientmonitor");
        if (!dir.exists()) dir.mkdirs();

        File f = new File(dir, "patient_records.json");
        if (f.exists()) f.delete();
    }

    @Test
    void appendPreservesExistingRecords() {
        RecordService service = new RecordService();

        service.saveRecords(List.of(new PatientRecord("Alice", "R001", "Flu", "2026-01-01")));
        service.append(new PatientRecord("Bob", "R002", "Asthma", "2026-01-02"));

        List<PatientRecord> loaded = service.loadRecords();
        assertEquals(2, loaded.size());
        assertEquals("Alice", loaded.get(0).getPatientName());
        assertEquals("Bob", loaded.get(1).getPatientName());
    }
}
