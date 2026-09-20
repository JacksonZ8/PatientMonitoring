package Services;

import Models.Vitals.LiveVitals;
import Models.Vitals.VitalRecord;
import Models.Vitals.VitalRecordIO;

import java.util.ArrayList;
import java.util.List;

public class MinuteAveragingService {

    private final LiveVitals vitals;

    private final List<Double> hr   = new ArrayList<>();
    private final List<Double> rr   = new ArrayList<>();
    private final List<Double> temp = new ArrayList<>();
    private final List<Double> spo2 = new ArrayList<>();

    public MinuteAveragingService(LiveVitals vitals) {
        this.vitals = vitals;
    }

    // call once per second from LiveMonitoringPage
    // 60 samples = 1 minute average
    public void sample() {

        hr.add(vitals.getHeartRate());
        rr.add(vitals.getRespRate());
        temp.add(vitals.getTemperature());
        spo2.add(vitals.getSpO2());

        // Wait until we have 60 samples (≈ 1 minute)
        if (hr.size() < 60) return;

        VitalRecord record = new VitalRecord(
                vitals.getPatientId(),
                avg(hr),
                avg(rr),
                avg(temp),
                avg(spo2)
        );

        VitalRecordIO.append(record);

        // Reset buffers for next minute
        hr.clear();
        rr.clear();
        temp.clear();
        spo2.clear();
    }

    private double avg(List<Double> v) {
        return v.stream().mapToDouble(d -> d).average().orElse(0);
    }
}
