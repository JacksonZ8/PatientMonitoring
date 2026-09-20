package Models.Patients;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AddedPatientDBTest {

    private Patient alice;
    private Patient bob;
    private Patient charlie;

    @BeforeEach
    void setUp() {
        // reset static DB before every test
        AddedPatientDB.replaceAll(List.of());

        alice = new Patient();
        alice.setGivenName("Alice");
        alice.setFamilyName("Smith");
        alice.setSticky(false);

        bob = new Patient();
        bob.setGivenName("Bob");
        bob.setFamilyName("Jones");
        bob.setSticky(true);

        charlie = new Patient();
        charlie.setGivenName("Charlie");
        charlie.setFamilyName("Brown");
        charlie.setSticky(false);
    }

    @Test
    void addPatientStoresPatient() {
        AddedPatientDB.addPatient(alice);

        List<Patient> all = AddedPatientDB.getAll();
        assertEquals(1, all.size());
        assertEquals("Alice Smith", all.get(0).getName());
    }

    @Test
    void removePatientRemovesPatient() {
        AddedPatientDB.addPatient(alice);
        AddedPatientDB.addPatient(bob);

        AddedPatientDB.removePatient(alice);

        List<Patient> all = AddedPatientDB.getAll();
        assertEquals(1, all.size());
        assertEquals("Bob Jones", all.get(0).getName());
    }

    @Test
    void replaceAllOverwritesDatabase() {
        AddedPatientDB.addPatient(alice);

        AddedPatientDB.replaceAll(List.of(bob, charlie));

        List<Patient> all = AddedPatientDB.getAll();
        assertEquals(2, all.size());
        assertTrue(all.contains(bob));
        assertTrue(all.contains(charlie));
    }

    @Test
    void searchFindsPatientsByNameCaseInsensitive() {
        AddedPatientDB.replaceAll(List.of(alice, bob, charlie));

        List<Patient> result = AddedPatientDB.search("ali");

        assertEquals(1, result.size());
        assertEquals("Alice Smith", result.get(0).getName());
    }

    @Test
    void searchWithEmptyQueryReturnsAll() {
        AddedPatientDB.replaceAll(List.of(alice, bob));

        List<Patient> result = AddedPatientDB.search("");

        assertEquals(2, result.size());
    }

    @Test
    void getSortedPutsStickyPatientsFirstThenAlphabetical() {
        AddedPatientDB.replaceAll(List.of(alice, bob, charlie));

        List<Patient> sorted = AddedPatientDB.getSorted(AddedPatientDB.getAll());

        // Sticky patient first
        assertEquals("Bob Jones", sorted.get(0).getName());

        // Remaining sorted alphabetically
        assertEquals("Alice Smith", sorted.get(1).getName());
        assertEquals("Charlie Brown", sorted.get(2).getName());
    }
}
