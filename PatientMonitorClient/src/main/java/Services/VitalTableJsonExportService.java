package Services;

import Models.Vitals.VitalRecord;
import Models.Vitals.VitalRecordIO;
import Utilities.LanguageManager;

import javax.swing.*;
import java.io.FileWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class VitalTableJsonExportService {

    // export minute averaged vital history for one patient as JSON
    public static void exportJson(int patientId) {

        // Filter by patient
        List<VitalRecord> records = VitalRecordIO.loadAll()
                .stream()
                .filter(r -> r.getPatientId() == patientId)
                .collect(Collectors.toList());

        if (records.isEmpty()) {
            JOptionPane.showMessageDialog(
                    null,
                    LanguageManager.t("export.noMinuteData"),
                    LanguageManager.t("export.vitalHistory"),
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("vital_history_patient_" + patientId + ".json"));

        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return;

        try (FileWriter w = new FileWriter(chooser.getSelectedFile())) {

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            w.write("{\n");
            w.write("  \"patientId\": " + patientId + ",\n");
            w.write("  \"units\": {\n");
            w.write("    \"heartRate\": \"bpm\",\n");
            w.write("    \"respRate\": \"breaths/min\",\n");
            w.write("    \"temperature\": \"°C\",\n");
            w.write("    \"spo2\": \"%\"\n");
            w.write("  },\n");
            w.write("  \"records\": [\n");

            for (int i = 0; i < records.size(); i++) {
                VitalRecord r = records.get(i);

                w.write(String.format(
                        "    {\"time\":\"%s\",\"hr\":%.2f,\"resp\":%.2f,\"temp\":%.2f,\"spo2\":%.2f}",
                        r.getTimestamp().format(fmt),
                        r.getAvgHeartRate(),
                        r.getAvgRespRate(),
                        r.getAvgTemperature(),
                        r.getAvgSpO2()
                ));

                if (i < records.size() - 1) w.write(",");
                w.write("\n");
            }

            w.write("  ]\n");
            w.write("}\n");

            JOptionPane.showMessageDialog(
                    null,
                    LanguageManager.t("export.completeMessage"),
                    LanguageManager.t("export.completeTitle"),
                    JOptionPane.INFORMATION_MESSAGE
            );

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    null,
                    LanguageManager.t("export.failedMessage"),
                    LanguageManager.t("export.failedTitle"),
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }
}
