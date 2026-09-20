package Services;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class AccountMetaService {
    private final LocalAppStateService stateService = new LocalAppStateService();

    public AccountMetaService() {
    }

    public static class Meta {
        public LocalDateTime createdAt;
        public LocalDateTime lastLogin;
        public String avatarPath;
        public String role;
    }

    public Meta loadOrInit() {
        Meta m = new Meta();
        LocalAppStateService.State state = stateService.load();

        LocalDateTime now = LocalDateTime.now();
        m.createdAt = parseDT(state.createdAt, null);
        if (m.createdAt == null) {
            m.createdAt = now;
            state.createdAt = m.createdAt.toString();
        }

        m.lastLogin = parseDT(state.lastLogin, now.minusHours(2));
        state.lastLogin = now.toString();

        m.avatarPath = state.avatarPath == null ? "" : state.avatarPath.trim();
        if (m.avatarPath.isBlank()) m.avatarPath = null;

        m.role = state.role == null ? "" : state.role.trim();
        if (m.role.isBlank()) m.role = null;

        stateService.save(state);
        return m;
    }

    public void saveAvatarPath(String pathOrNull) {
        LocalAppStateService.State state = stateService.load();
        state.avatarPath = pathOrNull == null ? "" : pathOrNull;
        stateService.save(state);
    }

    public void saveRole(String roleOrNull) {
        LocalAppStateService.State state = stateService.load();
        state.role = roleOrNull == null ? "" : roleOrNull;
        stateService.save(state);
    }
    
    public Path getDirPath() {
        try { Files.createDirectories(stateService.getStateDir()); } catch (IOException ignored) {}
        return stateService.getStateDir();
    }

    private LocalDateTime parseDT(String s, LocalDateTime fallback) {
        if (s == null || s.isBlank()) return fallback;
        try { return LocalDateTime.parse(s.trim()); } catch (Exception e) { return fallback; }
    }
}
