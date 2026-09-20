package Services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DigitalTwinNoteService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type NOTE_MAP_TYPE = new TypeToken<Map<String, List<NoteEntry>>>(){}.getType();
    private static final Path NOTE_FILE = Path.of(
            System.getProperty("user.home"), ".patientmonitor", "digital_twin_notes.json"
    );

    public synchronized List<NoteEntry> load(int patientId) {
        return new ArrayList<>(loadAll().getOrDefault(String.valueOf(patientId), List.of()));
    }

    public synchronized void save(int patientId, List<NoteEntry> notes) {
        Map<String, List<NoteEntry>> all = loadAll();
        all.put(String.valueOf(patientId), new ArrayList<>(notes == null ? List.of() : notes));
        writeAll(all);
    }

    private Map<String, List<NoteEntry>> loadAll() {
        if (!Files.exists(NOTE_FILE)) return new HashMap<>();
        try (FileReader reader = new FileReader(NOTE_FILE.toFile())) {
            Map<String, List<NoteEntry>> data = GSON.fromJson(reader, NOTE_MAP_TYPE);
            return data == null ? new HashMap<>() : data;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private void writeAll(Map<String, List<NoteEntry>> all) {
        try {
            Files.createDirectories(NOTE_FILE.getParent());
            try (FileWriter writer = new FileWriter(NOTE_FILE.toFile())) {
                GSON.toJson(all == null ? Map.of() : all, writer);
            }
        } catch (Exception ignored) {
        }
    }

    public static class NoteEntry {
        public String text;
        public String level;
        public String time;

        public NoteEntry() {}

        public NoteEntry(String text, String level, String time) {
            this.text = text;
            this.level = level;
            this.time = time;
        }
    }
}
