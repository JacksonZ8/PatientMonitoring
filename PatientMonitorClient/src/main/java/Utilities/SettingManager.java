package Utilities;

import Services.LocalAppStateService;

public class SettingManager {
    private final LocalAppStateService stateService = new LocalAppStateService();

    // Theme
    public boolean isDarkMode() {
        return stateService.load().darkMode;
    }

    public void setDarkMode(boolean enabled) {
        LocalAppStateService.State state = stateService.load();
        state.darkMode = enabled;
        stateService.save(state);
    }

    // Language
    public String getLanguage() {
        return stateService.load().language;
    }

    public void setLanguage(String langCode) {
        // e.g., "en", "zh"
        if (langCode == null || langCode.isBlank()) return;
        LocalAppStateService.State state = stateService.load();
        state.language = langCode.trim();
        stateService.save(state);
    }

    // Avatar
    // absolute path to avatar image, or empty string if none
    public String getAvatarPath() {
        return stateService.load().avatarPath;
    }

    // path absolute file path; empty / null to remove avatar
    public void setAvatarPath(String path) {
        LocalAppStateService.State state = stateService.load();
        if (path == null || path.isBlank()) {
            state.avatarPath = "";
        } else {
            state.avatarPath = path;
        }
        stateService.save(state);
    }

    // Reset
    public void resetToDefaults() {
        stateService.save(new LocalAppStateService.State());
    }
}
