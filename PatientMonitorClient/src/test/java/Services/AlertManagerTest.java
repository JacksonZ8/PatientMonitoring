package Services;

import Models.Patients.Patient;
import Models.Vitals.AlertRecord;
import Models.Vitals.LiveVitals;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AlertManagerTest {

    private AlertManager alertManager;
    private Patient patient;

    @BeforeEach
    void setUp() {
        alertManager = AlertManager.getInstance();

        // reset global state (important for singleton)
        alertManager.disableAlerts();
        alertManager.clearHistory();

        // create patient using real model pattern
        patient = new Patient();
        patient.setId(1);
        patient.setGivenName("John");
        patient.setFamilyName("Doe");
    }

    @Test
    void alertsAreIgnoredWhenDisabled() {
        alertManager.updateAlert(
                patient,
                LiveVitals.VitalsSeverity.DANGER,
                List.of("HR too high")
        );

        assertTrue(
                alertManager.getHistory().isEmpty(),
                "No alerts should be logged when alerts are disabled"
        );
    }

    @Test
    void alertIsLoggedWhenEnabled() {
        alertManager.enableAlerts();

        alertManager.updateAlert(
                patient,
                LiveVitals.VitalsSeverity.WARNING,
                List.of("Temperature elevated")
        );

        List<AlertRecord> history = alertManager.getHistory();
        assertEquals(1, history.size());

        AlertRecord record = history.get(0);

        assertEquals(1, record.getPatientId());
        assertEquals("John Doe", record.getPatientName());
        assertEquals(LiveVitals.VitalsSeverity.WARNING, record.getSeverity());
        assertTrue(record.getCauses().contains("Temperature elevated"));
        assertNotNull(record.getTimestamp());
    }

    @Test
    void sameSeverityDoesNotSpamHistory() {
        alertManager.enableAlerts();

        alertManager.updateAlert(
                patient,
                LiveVitals.VitalsSeverity.DANGER,
                List.of("SpO2 low")
        );

        alertManager.updateAlert(
                patient,
                LiveVitals.VitalsSeverity.DANGER,
                List.of("Still low")
        );

        assertEquals(
                1,
                alertManager.getHistory().size(),
                "History should only log when severity changes"
        );
    }

    @Test
    void resolvingAlertDoesNotCreateNewHistoryEntry() {
        alertManager.enableAlerts();

        alertManager.updateAlert(
                patient,
                LiveVitals.VitalsSeverity.WARNING,
                List.of("Resp rate high")
        );

        alertManager.updateAlert(
                patient,
                LiveVitals.VitalsSeverity.NORMAL,
                List.of()
        );

        assertEquals(
                1,
                alertManager.getHistory().size(),
                "Resolving alert should not log a new record"
        );
    }

    @Test
    void disableAlertsStopsFurtherLogging() {
        alertManager.enableAlerts();

        alertManager.updateAlert(
                patient,
                LiveVitals.VitalsSeverity.DANGER,
                List.of("Critical")
        );

        alertManager.disableAlerts();

        alertManager.updateAlert(
                patient,
                LiveVitals.VitalsSeverity.WARNING,
                List.of("Ignored")
        );

        assertEquals(
                1,
                alertManager.getHistory().size(),
                "No alerts should be logged after alerts are disabled"
        );
    }

    @Test
    void alertManagerIsSingleton() {
        AlertManager a = AlertManager.getInstance();
        AlertManager b = AlertManager.getInstance();

        assertSame(a, b);
    }
}
