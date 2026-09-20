package Models.Patients;

import java.util.ArrayList;
import java.util.List;

 // This class acts as a shared registry used by the UI and services
 // to access the list of patients currently loaded into the system.

public class AddedPatientDB {
    private static final List<Patient> patients = new ArrayList<>();
    private static boolean demoLoaded = false;

    public static void ensureDemoPatients() {
        if (demoLoaded || !patients.isEmpty()) return;

        patients.add(new Patient(1, "Raymond", "Ren", "Male", 82, "120/80"));
        patients.add(new Patient(2, "Jackson", "Zhou", "Male", 90, "110/75"));
        patients.add(new Patient(3, "David", "Wong", "Male", 76, "160/95"));
        patients.add(new Patient(4, "Xuan", "Li Feng", "Female", 68, "120/80"));
        patients.add(new Patient(5, "Martin", "Holloway", "Male", 73, "110/75"));
        patients.add(new Patient(6, "Harry", "Tan", "Male", 64, "135/86"));

        demoLoaded = true;
    }

    // Adds a patient to the shared in-memory patient list.
    public static void addPatient(Patient p) {
        if (p == null) return;
        patients.add(p);
    }

    // Removes a patient from the shared patient list.
    public static void removePatient(Patient p) {
        patients.remove(p);
    }

    // Returns a copy of all patients currently stored in the database.
    public static List<Patient> getAll() {
        return new ArrayList<>(patients);
    }

    // Replaces the current patient list with a new list.
    public static void replaceAll(List<Patient> newList) {
        List<Integer> stickyIds = new ArrayList<>();
        for (Patient patient : patients) {
            if (patient.isSticky()) stickyIds.add(patient.getId());
        }

        patients.clear();
        if (newList != null) {
            patients.addAll(newList);
            for (Patient patient : patients) {
                patient.setSticky(patient.isSticky() || stickyIds.contains(patient.getId()));
            }
        }

        demoLoaded = false;
    }

    // Searches for patients whose names contain the given query string.
    public static List<Patient> search(String query) {
        if (query == null || query.isEmpty()) return getAll();
        String q = query.toLowerCase();

        List<Patient> result = new ArrayList<>();
        for (Patient p : patients) {
            if (p.getName().toLowerCase().contains(q)) result.add(p);
        }
        return result;
    }

    // Returns a sorted copy of the given patient list, prioritising sticky patients.
    public static List<Patient> getSorted(List<Patient> list) {
        List<Patient> sorted = new ArrayList<>(list);
        sorted.sort((a, b) -> {
            if (a.isSticky() && !b.isSticky()) return -1;
            if (!a.isSticky() && b.isSticky()) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });
        return sorted;
    }
}
