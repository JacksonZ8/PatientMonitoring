package Services;

import Models.Patients.Patient;
import Utilities.LanguageManager;

import javax.swing.*;

public class PatientDischargeService {

    private static final PatientService PATIENT_SERVICE = new PatientService();

    public static void discharge(Patient patient, String diagnosis) {

        if (patient == null) return;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                PATIENT_SERVICE.discharge(patient, diagnosis);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();

                    AppEvents.firePatientsChanged();
                    AppEvents.fireRecordsChanged();

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(
                            null,
                            LanguageManager.t("patient.dischargeFailedPrefix") + ex.getMessage(),
                            LanguageManager.t("patient.dischargeError"),
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        };

        worker.execute();
    }
}
