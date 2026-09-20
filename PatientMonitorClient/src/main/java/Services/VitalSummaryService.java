package Services;

import Models.Vitals.VitalRecord;
import Models.Vitals.VitalRecordIO;
import Models.Vitals.VitalSummary24h;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class VitalSummaryService {

    public static VitalSummary24h generateSummaryForLastHours(int hours) {

        List<VitalRecord> all = VitalRecordIO.loadAll();
        if (all.isEmpty()) return null;

        LocalDateTime cutoff = LocalDateTime.now().minusHours(hours);

        List<VitalRecord> filtered = all.stream()
                .filter(r -> r.getTimestamp().isAfter(cutoff))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) return null;

        double hr = 0, rr = 0, temp = 0, spo2 = 0;

        for (VitalRecord r : filtered) {
            hr   += r.getAvgHeartRate();
            rr   += r.getAvgRespRate();
            temp += r.getAvgTemperature();
            spo2 += r.getAvgSpO2();
        }

        int n = filtered.size();

        return new VitalSummary24h(
                filtered.get(0).getTimestamp(),
                filtered.get(n - 1).getTimestamp(),
                hr / n,
                rr / n,
                temp / n,
                spo2 / n
        );
    }
}
