package UI.Pages;

import Models.Patients.AddedPatientDB;
import Models.Patients.Patient;
import Services.AppEvents;
import Services.PatientService;

import UI.Components.SearchBar;
import UI.Components.Tiles.AddTile;
import UI.Components.WrapLayout;
import UI.MainWindow;
import UI.PageLifecycle;
import UI.Components.Tiles.PatientTile;
import Utilities.SettingManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

import UI.Components.AlertHistorySidebar;
import Utilities.LanguageManager;

public class HomePage extends JPanel implements PageLifecycle {
    private JPanel grid;
    private MainWindow window;
    private String currentFilter = "";
    private JLabel statusLabel;
    private final SettingManager settings = new SettingManager();
    private final PatientService patientService = new PatientService();
    private final Runnable patientsChangedListener = this::reloadFromServerAsync;
    private AlertHistorySidebar alertSidebar;

    public HomePage(MainWindow window) {
        this.window = window;

        setLayout(new BorderLayout());

        boolean darkMode = settings.isDarkMode();
        Color appBg = darkMode ? new Color(18, 18, 20) : new Color(245, 245, 245);
        setBackground(appBg);

        // top (Search bar row)
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        top.setBackground(appBg);

        SearchBar searchBar = new SearchBar();
        top.add(searchBar);

        JButton refreshButton = new JButton(LanguageManager.t("home.refresh"));
        refreshButton.setFocusPainted(false);
        refreshButton.setFont(new Font("Arial", Font.BOLD, 13));
        refreshButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (darkMode) {
            refreshButton.setBackground(new Color(45, 103, 220));
            refreshButton.setForeground(Color.WHITE);
            refreshButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(82, 130, 245), 1),
                    BorderFactory.createEmptyBorder(6, 18, 6, 18)
            ));
        } else {
            refreshButton.setBackground(Color.WHITE);
            refreshButton.setForeground(new Color(37, 99, 235));
            refreshButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(210, 218, 230), 1),
                    BorderFactory.createEmptyBorder(6, 18, 6, 18)
            ));
        }
        refreshButton.addActionListener(e -> reloadFromServerAsync());
        top.add(refreshButton);

        statusLabel = new JLabel(LanguageManager.t("home.loading"));
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        statusLabel.setForeground(darkMode ? new Color(190, 190, 195) : new Color(90, 90, 90));
        top.add(statusLabel);

        // center (Grid + Scroll)
        grid = new JPanel(new WrapLayout(FlowLayout.LEFT, 15, 15));
        grid.setBackground(appBg);
        grid.setBorder(BorderFactory.createEmptyBorder(0, 10, 20, 10));

        JScrollPane scroll = new JScrollPane(grid);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(appBg);

        // Wrap only the main content (top + grid) so the sidebar isn't pushed down by NORTH
        JPanel centerWrapper = new JPanel(new BorderLayout());
        centerWrapper.setBackground(appBg);
        centerWrapper.add(top, BorderLayout.NORTH);
        centerWrapper.add(scroll, BorderLayout.CENTER);

        add(centerWrapper, BorderLayout.CENTER);

        // right (Alert sidebar)
        alertSidebar = new AlertHistorySidebar(window);
        add(alertSidebar, BorderLayout.EAST);

        AppEvents.addPatientsChangedListener(patientsChangedListener);

        reloadFromServerAsync();
        refresh();

        searchBar.addSearchListener(text -> {
            currentFilter = text;
            refresh();
        });
    }

    public void reloadFromServerAsync() {
        final String doctorUsername = NetWork.Session.getDoctorEmail();

        SwingWorker<List<Patient>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Patient> doInBackground() throws Exception {
                return patientService.loadPatients(doctorUsername);
            }

            @Override
            protected void done() {
                try {
                    List<Patient> fromServer = get();
                    AddedPatientDB.replaceAll(fromServer);
                    statusLabel.setText(fromServer.size() + " " + LanguageManager.t("home.loadedSuffix"));
                } catch (Exception e) {
                    AddedPatientDB.ensureDemoPatients();
                    statusLabel.setText(LanguageManager.t("home.offlineDemo"));
                }
                refresh();
            }
        };

        statusLabel.setText(LanguageManager.t("home.loading"));
        worker.execute();
    }

    public void syncPatientsFromServerAsync() {
        reloadFromServerAsync();
    }

    public void refresh() {
        List<Patient> base = currentFilter == null || currentFilter.isEmpty()
                ? AddedPatientDB.getAll()
                : AddedPatientDB.search(currentFilter);
        refreshGrid(base);
    }

    private void refreshGrid(List<Patient> patients) {
        disposePatientTiles();
        grid.removeAll();

        boolean darkMode = settings.isDarkMode();
        Color tileBg = darkMode ? new Color(36, 36, 42) : Color.WHITE;

        for (Patient p : AddedPatientDB.getSorted(patients)) {
            PatientTile t = new PatientTile(p, window, this);
            t.setBackground(tileBg);
            grid.add(t);
        }

        AddTile add = new AddTile(window);
        add.setBackground(tileBg);
        grid.add(add);

        grid.revalidate();
        grid.repaint();
    }

    @Override
    public void onShown() {
        reloadFromServerAsync();
    }

    @Override
    public void disposePage() {
        AppEvents.removePatientsChangedListener(patientsChangedListener);
        disposePatientTiles();
        if (alertSidebar != null) {
            alertSidebar.dispose();
            alertSidebar = null;
        }
    }

    private void disposePatientTiles() {
        if (grid == null) return;
        for (Component component : grid.getComponents()) {
            if (component instanceof PatientTile tile) {
                tile.dispose();
            }
        }
    }
}
