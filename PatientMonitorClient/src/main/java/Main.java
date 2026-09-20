import UI.MainWindow;

public class  Main {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            String autoLang = System.getenv("PATIENT_AUTO_LANG");
            if (autoLang != null && !autoLang.isBlank()) {
                new Utilities.SettingManager().setLanguage(autoLang.trim());
                Utilities.LanguageManager.setLanguage(autoLang.trim());
            }

            MainWindow window = new MainWindow();
            if ("true".equalsIgnoreCase(System.getenv("PATIENT_AUTO_DEMO"))) {
                NetWork.Session.clear();
                NetWork.Session.setDoctorEmail("demo");
                NetWork.Session.setDoctorName("Demo", "");
                NetWork.Session.setDoctorRole("");
                window.getTopBar().updateDoctorInfo("Demo", "");
                window.onDoctorLoggedIn();

                String autoPage = System.getenv("PATIENT_AUTO_PAGE");
                if (autoPage != null && !autoPage.isBlank()) {
                    showAutoPage(window, autoPage.trim());
                }
            }
        });
    }

    private static void showAutoPage(MainWindow window, String page) {
        if ("digitalTwin".equalsIgnoreCase(page) || "digital-twin".equalsIgnoreCase(page)) {
            Models.Patients.AddedPatientDB.ensureDemoPatients();
            java.util.List<Models.Patients.Patient> patients = Models.Patients.AddedPatientDB.getAll();
            if (!patients.isEmpty()) {
                window.showDigitalTwin(patients.get(0));
            }
            return;
        }

        if ("live".equalsIgnoreCase(page) || "liveMonitoring".equalsIgnoreCase(page) || "live-monitoring".equalsIgnoreCase(page)) {
            Models.Patients.AddedPatientDB.ensureDemoPatients();
            java.util.List<Models.Patients.Patient> patients = Models.Patients.AddedPatientDB.getAll();
            if (!patients.isEmpty()) {
                window.showLiveMonitoring(patients.get(0));
            }
            return;
        }

        if ("account".equalsIgnoreCase(page)) {
            window.showPage(MainWindow.PAGE_ACCOUNT);
            return;
        }

        if ("settings".equalsIgnoreCase(page)) {
            window.showPage(MainWindow.PAGE_SETTINGS);
            return;
        }

        if ("home".equalsIgnoreCase(page)) {
            window.showPage(MainWindow.PAGE_HOME);
        }
    }
}
