package Models.Patients;

import NetWork.ServerConfig;
import NetWork.Session;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import javax.swing.*;
import java.io.*;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class PatientRecordIO {
    private static final Path RECORD_FILE = Path.of(
            System.getProperty("user.home"), ".patientmonitor", "patient_records.json"
    );

    private static final Gson gson =
            new GsonBuilder().setPrettyPrinting().create();

    private static final Type LIST_TYPE =
            new TypeToken<List<PatientRecord>>(){}.getType();

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private static class SaveRequest {
        String doctor;
        List<PatientRecord> records;

        SaveRequest(String doctor, List<PatientRecord> records) {
            this.doctor = doctor;
            this.records = records;
        }
    }

    public enum Source {
        SERVER,
        LOCAL_CACHE
    }

    public record LoadResult(List<PatientRecord> records, Source source) {}

    // Loads patient records from backend for real/demo current doctor, falling back to local JSON if offline.
    public static List<PatientRecord> loadRecords() {
        return loadRecordsWithSource().records();
    }

    public static LoadResult loadRecordsWithSource() {
        try {
            List<PatientRecord> remote = loadRecordsFromServer(Session.getDoctorEmail());
            saveLocalRecords(remote);
            return new LoadResult(remote, Source.SERVER);
        } catch (Exception ignored) {
            return new LoadResult(loadLocalRecords(), Source.LOCAL_CACHE);
        }
    }

    // Saves records to backend and local fallback cache.
    public static void saveRecords(List<PatientRecord> list) {
        List<PatientRecord> safeList = list == null ? new ArrayList<>() : list;
        saveLocalRecords(safeList);
        try {
            saveRecordsToServer(Session.getDoctorEmail(), safeList);
        } catch (Exception ignored) {
        }
    }

    private static List<PatientRecord> loadRecordsFromServer(String doctor) throws Exception {
        String encodedDoctor = URLEncoder.encode(
                doctor == null || doctor.isBlank() ? "demo" : doctor.trim(),
                StandardCharsets.UTF_8
        );
        String url = ServerConfig.url("/api/patient-records?doctor=" + encodedDoctor);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .GET();
        Session.applyAuth(builder);
        HttpRequest request = builder.build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Record fetch failed: HTTP " + response.statusCode());
        }

        List<PatientRecord> list = gson.fromJson(response.body(), LIST_TYPE);
        return list == null ? new ArrayList<>() : list;
    }

    private static void saveRecordsToServer(String doctor, List<PatientRecord> records) throws Exception {
        SaveRequest body = new SaveRequest(
                doctor == null || doctor.isBlank() ? "demo" : doctor.trim(),
                records
        );
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(ServerConfig.url("/api/patient-records")))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body), StandardCharsets.UTF_8));
        Session.applyAuth(builder);
        HttpRequest request = builder.build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Record save failed: HTTP " + response.statusCode());
        }
    }

    private static List<PatientRecord> loadLocalRecords() {
        File file = RECORD_FILE.toFile();

        if (!file.exists()) return new ArrayList<>();

        try (FileReader reader = new FileReader(file)) {
            List<PatientRecord> list = gson.fromJson(reader, LIST_TYPE);
            return (list != null) ? list : new ArrayList<>();

        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static void saveLocalRecords(List<PatientRecord> list) {
        try {
            Files.createDirectories(RECORD_FILE.getParent());
        } catch (Exception ignored) {
        }

        try (FileWriter writer = new FileWriter(RECORD_FILE.toFile())) {
            gson.toJson(list, writer);
        } catch (Exception ignored) {
        }
    }

    // Imports patient records from a user-selected CSV file.
    public static List<PatientRecord> importCSV(JFrame parent) {

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select patient CSV");

        List<PatientRecord> imported = new ArrayList<>();

        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {

            try (BufferedReader br = new BufferedReader(new FileReader(chooser.getSelectedFile()))) {

                String line;
                while ((line = br.readLine()) != null) {
                    String[] p = line.split(",");

                    String name = p.length > 0 ? p[0].trim() : "Unknown";
                    String id = p.length > 1 ? p[1].trim() : "N/A";
                    String diag = p.length > 2 ? p[2].trim() : "No diagnosis";
                    String date = p.length > 3 ? p[3].trim() : "Unknown";

                    imported.add(new PatientRecord(name, id, diag, date));
                }

                JOptionPane.showMessageDialog(parent, "Imported " + imported.size() + " records!");

            } catch (Exception e) {
                JOptionPane.showMessageDialog(parent, "CSV read error.", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }

        return imported;
    }
}
