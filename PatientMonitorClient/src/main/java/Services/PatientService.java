package Services;

import Models.Patients.AddedPatientDB;
import Models.Patients.Patient;
import Models.Patients.PatientRecord;
import Models.Vitals.LiveVitals;
import NetWork.ServerConfig;
import NetWork.Session;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

public class PatientService {
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private final RecordService recordService = new RecordService();

    public List<Patient> loadPatients(String doctorEmail) throws Exception {
        return PatientServerClient.fetchPatients(doctorEmail);
    }

    public void addPatient(Patient patient) throws Exception {
        if (patient == null) throw new IllegalArgumentException("Patient is required");

        JsonObject body = new JsonObject();
        body.addProperty("doctor", Session.getDoctorEmail());
        body.addProperty("givenname", patient.getGivenName());
        body.addProperty("familyname", patient.getFamilyName());
        body.addProperty("gender", patient.getGender());
        body.addProperty("age", patient.getAge());
        body.addProperty("bp", patient.getBloodPressure());

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(ServerConfig.url("/api/patient")))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8));
        Session.applyAuth(builder);
        HttpRequest req = builder.build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        ensureOk(resp, "Add patient failed");
    }

    public void discharge(Patient patient, String diagnosis) throws Exception {
        if (patient == null) throw new IllegalArgumentException("Patient is required");

        String doctor = Session.getDoctorEmail();
        String url = ServerConfig.url(
                "/api/patient/discharge?doctor="
                        + URLEncoder.encode(doctor, StandardCharsets.UTF_8)
                        + "&id=" + patient.getId()
        );

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.noBody());
        Session.applyAuth(builder);
        HttpRequest req = builder.build();

        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        ensureOk(resp, "Discharge failed");

        PatientRecord record = new PatientRecord(
                patient.getName(),
                "REC-" + patient.getId(),
                diagnosis,
                LocalDate.now().toString()
        );
        recordService.append(record);

        AddedPatientDB.removePatient(patient);
        LiveVitals.removeShared(patient.getId());
    }

    private void ensureOk(HttpResponse<String> resp, String fallback) {
        if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
            throw new RuntimeException(readServerError(resp.body(), fallback + ": HTTP " + resp.statusCode()));
        }

        try {
            JsonObject out = JsonParser.parseString(resp.body()).getAsJsonObject();
            if (out.has("ok") && !out.get("ok").getAsBoolean()) {
                throw new RuntimeException(out.has("error") ? out.get("error").getAsString() : fallback);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception ignored) {
        }
    }

    private String readServerError(String body, String fallback) {
        try {
            JsonObject out = JsonParser.parseString(body).getAsJsonObject();
            if (out.has("error")) return out.get("error").getAsString();
            if (out.has("message")) return out.get("message").getAsString();
        } catch (Exception ignored) {
        }
        return body == null || body.isBlank() ? fallback : fallback + ": " + body;
    }
}
