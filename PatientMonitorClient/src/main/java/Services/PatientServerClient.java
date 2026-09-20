package Services;

import Models.Patients.Patient;
import NetWork.ServerConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public class PatientServerClient {

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();

    public static List<Patient> fetchPatients(String doctorUsername) throws Exception {
        String doctor = URLEncoder.encode(doctorUsername == null ? "demo" : doctorUsername, StandardCharsets.UTF_8);
        String url = ServerConfig.url("/api/patients?doctor=" + doctor);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException(readServerError(resp.body(), "fetchPatients failed: HTTP " + resp.statusCode()));
        }

        Type listType = new TypeToken<List<Patient>>(){}.getType();
        return gson.fromJson(resp.body(), listType);
    }

    private static String readServerError(String body, String fallback) {
        try {
            ServerError error = gson.fromJson(body, ServerError.class);
            if (error != null && error.message != null && !error.message.isBlank()) {
                return error.message;
            }
        } catch (Exception ignored) {
        }
        return body == null || body.isBlank() ? fallback : body;
    }

    private static class ServerError {
        String message;
    }
}
