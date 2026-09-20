package NetWork;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class SessionTest {

    @BeforeEach
    void resetSession() {
        Session.clear();
    }

    @Test
    void defaultSessionValues() {
        assertEquals("demo", Session.getDoctorEmail());
        assertEquals("demo", Session.getDoctorFullName());
        assertEquals("", Session.getDoctorRole());
    }

    @Test
    void setDoctorEmailStoresTrimmedEmail() {
        Session.setDoctorEmail("  doctor@test.com  ");
        assertEquals("doctor@test.com", Session.getDoctorEmail());
    }

    @Test
    void blankEmailDoesNotOverrideExisting() {
        Session.setDoctorEmail("doctor@test.com");
        Session.setDoctorEmail("   ");

        assertEquals("doctor@test.com", Session.getDoctorEmail());
    }

    @Test
    void setDoctorNameStoresGivenAndFamilyNames() {
        Session.setDoctorName("John", "Doe");

        assertEquals("John Doe", Session.getDoctorFullName());
    }

    @Test
    void nullNamesAreHandledSafely() {
        Session.setDoctorName(null, null);

        assertEquals("demo", Session.getDoctorFullName());
    }

    @Test
    void fullNameFallsBackToEmailWhenNamesEmpty() {
        Session.setDoctorEmail("doctor@test.com");
        Session.setDoctorName("", "");

        assertEquals("doctor@test.com", Session.getDoctorFullName());
    }

    @Test
    void setDoctorRoleStoresTrimmedRole() {
        Session.setDoctorRole("  Clinician  ");

        assertEquals("Clinician", Session.getDoctorRole());
    }

    @Test
    void clearResetsAllSessionState() {
        Session.setDoctorEmail("doctor@test.com");
        Session.setDoctorName("John", "Doe");
        Session.setDoctorRole("Admin");

        Session.clear();

        assertEquals("demo", Session.getDoctorEmail());
        assertEquals("demo", Session.getDoctorFullName());
        assertEquals("", Session.getDoctorRole());
    }
}
