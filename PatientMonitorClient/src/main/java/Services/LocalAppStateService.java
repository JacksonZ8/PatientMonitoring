package Services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class LocalAppStateService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path STATE_FILE = Path.of(System.getProperty("user.home"), ".patientmonitor", "app-state.json");

    public static class State {
        public boolean darkMode = false;
        public String language = "en";
        public String avatarPath = "";
        public String role = "";
        public String createdAt = "";
        public String lastLogin = "";
        public String lastServerUrl = "";
        public WindowState window = new WindowState();
    }

    public static class WindowState {
        public int width = 1280;
        public int height = 820;
        public boolean maximized = true;
    }

    public synchronized State load() {
        if (!Files.exists(STATE_FILE)) return new State();

        try (FileReader reader = new FileReader(STATE_FILE.toFile())) {
            State state = GSON.fromJson(reader, State.class);
            if (state == null) state = new State();
            if (state.window == null) state.window = new WindowState();
            if (state.language == null || state.language.isBlank()) state.language = "en";
            return state;
        } catch (Exception e) {
            return new State();
        }
    }

    public synchronized void save(State state) {
        try {
            Files.createDirectories(STATE_FILE.getParent());
            try (FileWriter writer = new FileWriter(STATE_FILE.toFile())) {
                GSON.toJson(state == null ? new State() : state, writer);
            }
        } catch (Exception ignored) {
        }
    }

    public Path getStateDir() {
        return STATE_FILE.getParent();
    }
}
