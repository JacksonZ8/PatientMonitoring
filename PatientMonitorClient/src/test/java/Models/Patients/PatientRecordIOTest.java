package Models.Patients;

import org.junit.jupiter.api.*;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PatientRecordIOTest {

    private static final String TEST_HOME =
            System.getProperty("java.io.tmpdir") + "/patient_record_test";

    @BeforeEach
    void setup() {
        // Redirect user.home to a temp directory
        System.setProperty("user.home", TEST_HOME);

        File dir = new File(TEST_HOME);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Clean previous test file if exists
        File f = new File(TEST_HOME + "/.patientmonitor/patient_records.json");
        if (f.exists()) {
            f.delete();
        }
    }

    @Test
    void saveAndLoadRecordsWorksCorrectly() {
        List<PatientRecord> records = List.of(
                new PatientRecord("Alice", "R001", "Flu", "2024-01-01"),
                new PatientRecord("Bob", "R002", "Asthma", "2024-01-02")
        );

        PatientRecordIO.saveRecords(records);

        List<PatientRecord> loaded = PatientRecordIO.loadRecords();

        assertEquals(2, loaded.size());

        assertEquals("Alice", loaded.get(0).getPatientName());
        assertEquals("R001", loaded.get(0).getRecordId());
        assertEquals("Flu", loaded.get(0).getDiagnosis());
    }

    @Test
    void loadRecordsReturnsEmptyListWhenFileDoesNotExist() {
        List<PatientRecord> loaded = PatientRecordIO.loadRecords();

        assertNotNull(loaded);
        assertTrue(loaded.isEmpty(), "Should return empty list if file missing");
    }

    @Test
    void patientRecordMatchesSearchCorrectly() {
        PatientRecord record =
                new PatientRecord("John Smith", "REC123", "Diabetes", "2024-02-01");

        assertTrue(record.matches("john"));
        assertTrue(record.matches("rec"));
        assertTrue(record.matches("diab"));

        assertFalse(record.matches("cancer"));
    }

    @Test
    void toStringFormatIsCorrect() {
        PatientRecord record =
                new PatientRecord("Jane Doe", "R999", "Healthy", "2024-01-01");

        assertEquals("Jane Doe (R999)", record.toString());
    }
}
