package Services;

import Models.DoctorProfile;
import NetWork.ServerConfig;
import NetWork.Session;
import com.google.gson.Gson;

import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

// Loads/saves doctor profile from the backend for real accounts, with local fallback for demo/offline use.
public class DoctorProfileService {
    private static final String DIR_NAME = ".patientmonitor";
    private static final String FILE_NAME = "doctor_profile.properties";

    private static final String K_FIRST = "firstName";
    private static final String K_LAST  = "lastName";
    private static final String K_ID = "idNumber";
    private static final String K_AGE = "age";
    private static final String K_ORGANIZATION = "organization";
    private static final String K_LEGACY_ORGNIZATION = "orgnization";
    private static final String K_EMAIL = "email";

    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();

    private final Path filePath;

    private static class ProfileDto {
        String firstName;
        String lastName;
        String idNumber;
        int age;
        String organization;
        String email;
        String role;
    }

    public DoctorProfileService() {
        Path dir = Path.of(System.getProperty("user.home"), DIR_NAME);
        this.filePath = dir.resolve(FILE_NAME);
    }

    public DoctorProfile load() {
        String email = Session.getDoctorEmail();
        if (isRealAccount(email)) {
            try {
                DoctorProfile remote = loadFromServer(email);
                saveLocal(remote);
                return remote;
            } catch (Exception ignored) {
            }
        }

        return loadLocal();
    }

    public void save(DoctorProfile profile) {
        if (profile == null) return;
        saveLocal(profile);

        String email = profile.getEmail();
        if (!isRealAccount(email)) email = Session.getDoctorEmail();
        if (isRealAccount(email)) {
            profile.setEmail(email);
            try {
                saveToServer(profile);
            } catch (Exception ignored) {
            }
        }
    }

    private DoctorProfile loadFromServer(String email) throws Exception {
        String url = ServerConfig.url("/api/doctor-profile?email=" +
                URLEncoder.encode(email, StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Profile fetch failed: HTTP " + response.statusCode());
        }

        return fromDto(GSON.fromJson(response.body(), ProfileDto.class));
    }

    private void saveToServer(DoctorProfile profile) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ServerConfig.url("/api/doctor-profile")))
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(toDto(profile)), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Profile save failed: HTTP " + response.statusCode());
        }
    }

    private DoctorProfile loadLocal() {
        if (!Files.exists(filePath)) {
            DoctorProfile def = DoctorProfile.defaults();
            saveLocal(def);
            return def;
        }

        Properties p = new Properties();
        try (InputStream in = new FileInputStream(filePath.toFile())) {
            p.load(in);
        } catch (IOException e) {
            return DoctorProfile.defaults();
        }

        DoctorProfile d = new DoctorProfile();
        DoctorProfile def = DoctorProfile.defaults();

        d.setFirstName(p.getProperty(K_FIRST, def.getFirstName()));
        d.setLastName(p.getProperty(K_LAST, def.getLastName()));
        d.setIdNumber(p.getProperty(K_ID, def.getIdNumber()));
        d.setOrgnization(p.getProperty(K_ORGANIZATION, p.getProperty(K_LEGACY_ORGNIZATION, def.getOrgnization())));
        d.setEmail(p.getProperty(K_EMAIL, def.getEmail()));

        int age = def.getAge();
        try {
            age = Integer.parseInt(p.getProperty(K_AGE, String.valueOf(age)).trim());
        } catch (Exception ignored) {}
        d.setAge(age);

        return d;
    }

    private void saveLocal(DoctorProfile d) {
        try {
            Files.createDirectories(filePath.getParent());
        } catch (IOException ignored) {}

        Properties p = new Properties();
        p.setProperty(K_FIRST, safe(d.getFirstName()));
        p.setProperty(K_LAST, safe(d.getLastName()));
        p.setProperty(K_ID, safe(d.getIdNumber()));
        p.setProperty(K_AGE, String.valueOf(d.getAge()));
        p.setProperty(K_ORGANIZATION, safe(d.getOrgnization()));
        p.setProperty(K_EMAIL, safe(d.getEmail()));

        try (OutputStream out = new FileOutputStream(filePath.toFile())) {
            p.store(out, "PatientMonitorClient Doctor Profile fallback cache");
        } catch (IOException ignored) {
        }
    }

    private ProfileDto toDto(DoctorProfile profile) {
        ProfileDto dto = new ProfileDto();
        dto.firstName = safe(profile.getFirstName());
        dto.lastName = safe(profile.getLastName());
        dto.idNumber = safe(profile.getIdNumber());
        dto.age = profile.getAge();
        dto.organization = safe(profile.getOrgnization());
        dto.email = safe(profile.getEmail());
        dto.role = Session.getDoctorRole();
        return dto;
    }

    private DoctorProfile fromDto(ProfileDto dto) {
        if (dto == null) return DoctorProfile.defaults();
        DoctorProfile profile = new DoctorProfile();
        profile.setFirstName(dto.firstName);
        profile.setLastName(dto.lastName);
        profile.setIdNumber(dto.idNumber);
        profile.setAge(dto.age);
        profile.setOrgnization(dto.organization);
        profile.setEmail(dto.email);
        if (dto.role != null && !dto.role.isBlank()) {
            Session.setDoctorRole(dto.role);
        }
        return profile;
    }

    private boolean isRealAccount(String email) {
        return email != null && !email.isBlank() && !"demo".equalsIgnoreCase(email.trim());
    }

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }
}
