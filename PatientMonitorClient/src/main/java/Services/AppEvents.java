package Services;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class AppEvents {
    private static final List<Runnable> patientListeners = new CopyOnWriteArrayList<>();
    private static final List<Runnable> recordListeners = new CopyOnWriteArrayList<>();
    private static final List<Runnable> settingsListeners = new CopyOnWriteArrayList<>();
    private static final List<Runnable> profileListeners = new CopyOnWriteArrayList<>();

    private AppEvents() {}

    public static void addPatientsChangedListener(Runnable listener) {
        if (listener != null) patientListeners.add(listener);
    }

    public static void removePatientsChangedListener(Runnable listener) {
        patientListeners.remove(listener);
    }

    public static void addRecordsChangedListener(Runnable listener) {
        if (listener != null) recordListeners.add(listener);
    }

    public static void removeRecordsChangedListener(Runnable listener) {
        recordListeners.remove(listener);
    }

    public static void addSettingsChangedListener(Runnable listener) {
        if (listener != null) settingsListeners.add(listener);
    }

    public static void removeSettingsChangedListener(Runnable listener) {
        settingsListeners.remove(listener);
    }

    public static void addProfileChangedListener(Runnable listener) {
        if (listener != null) profileListeners.add(listener);
    }

    public static void removeProfileChangedListener(Runnable listener) {
        profileListeners.remove(listener);
    }

    public static void firePatientsChanged() {
        fire(patientListeners);
    }

    public static void fireRecordsChanged() {
        fire(recordListeners);
    }

    public static void fireSettingsChanged() {
        fire(settingsListeners);
    }

    public static void fireProfileChanged() {
        fire(profileListeners);
    }

    private static void fire(List<Runnable> listeners) {
        Runnable task = () -> {
            for (Runnable listener : listeners) {
                listener.run();
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }
}
