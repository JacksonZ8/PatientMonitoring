package UI.Pages;

import Models.Patients.AddedPatientDB;
import Models.Vitals.LiveVitals;
import Models.Patients.Patient;
import UI.Components.DigitalTwinPanel;
import UI.Components.Tiles.BaseTile;
import UI.MainWindow;
import UI.PageLifecycle;
import NetWork.Session;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class DigitalTwinPage extends JPanel implements PageLifecycle {

    private final MainWindow window;
    private int currentIndex = 0;
    private Patient currentPatient;

    // Shared globally
    private LiveVitals liveVitals;

    // Only used to refresh UI (simulation runs globally)
    private Timer liveTimer;

    private final DigitalTwinPanel digitalTwinPanel;
    private final BaseTile twinTile;

    public DigitalTwinPage(MainWindow window) {
        this.window = window;
        List<Patient> allPatients = currentPatients();

        if (!allPatients.isEmpty()) {
            currentPatient = allPatients.get(0);
        }

        if (currentPatient != null) {
            initLiveVitalsForCurrentPatient();
        }

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        digitalTwinPanel = new DigitalTwinPanel();
        // Ensure dashboard uses the currently logged-in doctor (not demo)
        digitalTwinPanel.setDoctorEmail(Session.getDoctorEmail());
        digitalTwinPanel.setPatientSelectionHandler(this::setPatientFromSelector);

        twinTile = new BaseTile(1200, 750, 30, true);
        twinTile.setLayout(new BorderLayout());
        twinTile.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        twinTile.add(digitalTwinPanel, BorderLayout.CENTER);

        add(twinTile, BorderLayout.CENTER);

        if (currentPatient != null) {
            refreshPatientChoices();
            pushPatientToTwin();
            startLiveLoop();
        } else {
            refreshPatientChoices();
        }
    }

    private void refreshPatientChoices() {
        digitalTwinPanel.setPatients(currentPatients(), currentPatient);
    }

    private void pushPatientToTwin() {
        if (currentPatient == null) return;

        digitalTwinPanel.setPatient(currentPatient);

        if (liveVitals == null) {
            initLiveVitalsForCurrentPatient();
        }

        String bp = liveVitals.getBloodPressure();
        int sys = 120;
        int dia = 80;
        try {
            String[] parts = bp.split("/");
            sys = Integer.parseInt(parts[0].trim());
            dia = Integer.parseInt(parts[1].trim());
        } catch (Exception ignored) {}

        int hr = (int) Math.round(liveVitals.getHeartRate());
        int rr = (int) Math.round(liveVitals.getRespRate());
        int sp = (int) Math.round(liveVitals.getSpO2());
        double temp = liveVitals.getTemperature();

        digitalTwinPanel.setVitals(hr, rr, sp, sys, dia, temp);
    }

    private void initLiveVitalsForCurrentPatient() {
        String baselineBp = currentPatient.getBloodPressure();
        liveVitals = LiveVitals.getShared(currentPatient.getId(), baselineBp);
    }

    private void startLiveLoop() {
        if (liveTimer != null) liveTimer.stop();

        liveTimer = new Timer(1000, e -> pushPatientToTwin());
        liveTimer.start();
    }

    public void setPatient(Patient patient) {
        this.currentPatient = patient;
        this.currentIndex = indexOfPatient(patient);
        initLiveVitalsForCurrentPatient();
        digitalTwinPanel.setDoctorEmail(Session.getDoctorEmail());
        refreshPatientChoices();
        pushPatientToTwin();
        startLiveLoop();
    }

    private void setPatientFromSelector(Patient patient) {
        if (patient == null) return;
        if (currentPatient != null && currentPatient.getId() == patient.getId()) return;
        setPatient(patient);
    }

    public void nextPatient() {
        List<Patient> allPatients = currentPatients();
        if (allPatients.isEmpty()) return;
        currentIndex = (currentIndex + 1) % allPatients.size();
        currentPatient = allPatients.get(currentIndex);
        initLiveVitalsForCurrentPatient();
        digitalTwinPanel.setDoctorEmail(Session.getDoctorEmail());
        pushPatientToTwin();
        startLiveLoop();
    }

    public void previousPatient() {
        List<Patient> allPatients = currentPatients();
        if (allPatients.isEmpty()) return;
        currentIndex = (currentIndex - 1 + allPatients.size()) % allPatients.size();
        currentPatient = allPatients.get(currentIndex);
        initLiveVitalsForCurrentPatient();
        digitalTwinPanel.setDoctorEmail(Session.getDoctorEmail());
        pushPatientToTwin();
        startLiveLoop();
    }

    @Override
    public void onShown() {
        if (currentPatient != null) {
            refreshPatientChoices();
            pushPatientToTwin();
            startLiveLoop();
        }
    }

    @Override
    public void onHidden() {
        if (liveTimer != null) {
            liveTimer.stop();
            liveTimer = null;
        }
    }

    @Override
    public void disposePage() {
        onHidden();
        digitalTwinPanel.dispose();
    }

    private List<Patient> currentPatients() {
        return AddedPatientDB.getAll();
    }

    private int indexOfPatient(Patient patient) {
        if (patient == null) return -1;
        List<Patient> patients = currentPatients();
        for (int i = 0; i < patients.size(); i++) {
            if (patients.get(i).getId() == patient.getId()) return i;
        }
        return -1;
    }

    @Override
    public void removeNotify() {
        onHidden();
        super.removeNotify();
    }
}
