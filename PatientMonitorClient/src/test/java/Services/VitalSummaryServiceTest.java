package Services;

import Models.Vitals.VitalRecord;
import Models.Vitals.VitalRecordIO;
import Models.Vitals.VitalSummary24h;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class VitalSummaryServiceTest {

    @BeforeEach
    void clearRecords() {
        // Clear all records before each test
        VitalRecordIO.loadAll().forEach(r ->
                VitalRecordIO.clearForPatient(r.getPatientId())
        );
    }

    @Test
    void returnsNullWhenNoRecordsExist() {
        VitalSummary24h summary =
                VitalSummaryService.generateSummaryForLastHours(24);

        assertNull(summary, "Summary should be null when no records exist");
    }

    @Test
    void returnsNullWhenNoRecordsWithinTimeWindow() {
        // Create an old record (older than 24 hours)
        VitalRecord oldRecord = new VitalRecord(
                1, 70, 14, 36.5, 98
        );

        VitalRecordIO.append(oldRecord);

        // Summary for last 0 hours -> nothing should match
        VitalSummary24h summary =
                VitalSummaryService.generateSummaryForLastHours(0);

        assertNull(summary, "Summary should be null when no records fall in window");
    }

    @Test
    void computesCorrectAverages() {
        // Add multiple records within window
        VitalRecordIO.append(new VitalRecord(1, 60, 12, 36.0, 97));
        VitalRecordIO.append(new VitalRecord(1, 80, 16, 37.0, 99));

        VitalSummary24h summary =
                VitalSummaryService.generateSummaryForLastHours(24);

        assertNotNull(summary);

        assertEquals(70.0, summary.getMeanHR(), 0.001);
        assertEquals(14.0, summary.getMeanRR(), 0.001);
        assertEquals(36.5, summary.getMeanTemp(), 0.001);
        assertEquals(98.0, summary.getMeanSpO2(), 0.001);
    }

    @Test
    void startAndEndTimestampsAreCorrect() {
        VitalRecord r1 = new VitalRecord(1, 60, 12, 36.0, 97);
        pause();
        VitalRecord r2 = new VitalRecord(1, 70, 14, 36.5, 98);
        pause();
        VitalRecord r3 = new VitalRecord(1, 80, 16, 37.0, 99);

        VitalRecordIO.append(r1);
        VitalRecordIO.append(r2);
        VitalRecordIO.append(r3);

        VitalSummary24h summary =
                VitalSummaryService.generateSummaryForLastHours(24);

        assertEquals(r1.getTimestamp(), summary.getStart());
        assertEquals(r3.getTimestamp(), summary.getEnd());
    }

    // Small helper to ensure timestamp ordering
    private void pause() {
        try {
            Thread.sleep(2);
        } catch (InterruptedException ignored) {}
    }
}
