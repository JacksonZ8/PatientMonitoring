package UI.Components;

import Models.Patients.PatientRecord;
import Models.Patients.PatientRecordIO;
import Models.Patients.PatientRecordRenderer;
import Services.AppEvents;
import Services.RecordService;
import UI.Components.PlaceHolders.PlaceholderTextField;
import Utilities.LanguageManager;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

public class PatientRecordsPanel extends RoundedPanel {

    private DefaultListModel<PatientRecord> recordModel = new DefaultListModel<>();
    private DefaultListModel<PatientRecord> filteredModel = new DefaultListModel<>();
    private JList<PatientRecord> recordList;
    private PlaceholderTextField searchField;
    private JLabel statusLabel;
    private final RecordService recordService = new RecordService();
    private final Runnable recordsChangedListener = this::reloadFromDisk;
    private SwingWorker<PatientRecordIO.LoadResult, Void> loadWorker;

    // Creates the records panel UI with search, import, delete, and clear controls.
    public PatientRecordsPanel(Runnable onBack) {
        super(new BorderLayout(20, 20));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Buttons
        RoundedButton backBtn = new RoundedButton(LanguageManager.t("records.back"));
        backBtn.addActionListener(e -> onBack.run());

        RoundedButton importBtn = new RoundedButton(LanguageManager.t("records.importCsv"));
        RoundedButton deleteBtn = new RoundedButton(LanguageManager.t("records.deleteSelected"));
        RoundedButton clearBtn = new RoundedButton(LanguageManager.t("records.clearAll"));
        applyDangerStyle(deleteBtn);
        applyDangerStyle(clearBtn);

        searchField = new PlaceholderTextField(LanguageManager.t("records.search"));
        searchField.setPreferredSize(new Dimension(200, 34));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topBar.setOpaque(false);
        topBar.add(backBtn);
        topBar.add(importBtn);
        topBar.add(deleteBtn);
        topBar.add(clearBtn);
        topBar.add(searchField);
        statusLabel = new JLabel(LanguageManager.t("records.loading"));
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(100, 116, 139));
        topBar.add(statusLabel);

        // List
        recordList = new JList<>(filteredModel);
        recordList.setCellRenderer(new PatientRecordRenderer());

        recordList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    PatientRecord r = recordList.getSelectedValue();
                    if (r != null) showRecordPreview(r);
                }
            }
        });

        // Search
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            private void filter() {
                applyCurrentFilter();
            }

            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
        });

        // Import CSV
        importBtn.addActionListener(e -> {
            JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
            List<PatientRecord> newOnes = recordService.importCsv(parent);
            for (PatientRecord pr : newOnes)
                recordModel.addElement(pr);

            applyCurrentFilter();
            saveToJson();
        });

        // Delete
        deleteBtn.addActionListener(e -> {
            PatientRecord selected = recordList.getSelectedValue();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, LanguageManager.t("records.selectToDelete"));
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                    this, LanguageManager.t("records.deleteQuestion"), LanguageManager.t("records.confirmDelete"),
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm != JOptionPane.YES_OPTION) return;

            recordModel.removeElement(selected);
            filteredModel.removeElement(selected);
            applyCurrentFilter();
            saveToJson();
        });

        // Clear
        clearBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    LanguageManager.t("records.clearQuestion"),
                    LanguageManager.t("records.clearAll"),
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm != JOptionPane.YES_OPTION) return;

            recordModel.clear();
            filteredModel.clear();
            saveToJson();
        });

        add(topBar, BorderLayout.NORTH);
        add(new JScrollPane(recordList), BorderLayout.CENTER);

        // Load
        reloadFromDisk();
        AppEvents.addRecordsChangedListener(recordsChangedListener);
    }

    private void applyDangerStyle(RoundedButton button) {
        button.setColors(
                new Color(190, 55, 55),
                new Color(220, 70, 70),
                new Color(150, 40, 40),
                Color.WHITE
        );
    }
    // Helpers
    // Copies the full record list into the filtered model (used when clearing search).
    private void resetFilter() {
        filteredModel.clear();
        for (int i = 0; i < recordModel.size(); i++)
            filteredModel.addElement(recordModel.get(i));
    }

    private void applyCurrentFilter() {
        String q = searchField.getText().trim().toLowerCase();
        filteredModel.clear();

        if (q.isEmpty()) {
            resetFilter();
            return;
        }

        for (int i = 0; i < recordModel.size(); i++) {
            PatientRecord r = recordModel.get(i);
            if (r.matches(q)) filteredModel.addElement(r);
        }
    }

    // Loads patient records without blocking the Swing event dispatch thread.
    private void loadFromJsonAsync() {
        if (loadWorker != null && !loadWorker.isDone()) {
            loadWorker.cancel(true);
        }
        statusLabel.setText(LanguageManager.t("records.loading"));

        loadWorker = new SwingWorker<>() {
            @Override
            protected PatientRecordIO.LoadResult doInBackground() {
                return recordService.loadRecordsWithSource();
            }

            @Override
            protected void done() {
                if (isCancelled()) return;
                try {
                    PatientRecordIO.LoadResult result = get();
                    List<PatientRecord> records = result.records();
                    recordModel.clear();
                    for (PatientRecord r : records) {
                        recordModel.addElement(r);
                    }
                    applyCurrentFilter();
                    if (result.source() == PatientRecordIO.Source.SERVER) {
                        statusLabel.setText(records.size() + " " + LanguageManager.t("records.loadedFromServer"));
                    } else {
                        statusLabel.setText(records.size() + " " + LanguageManager.t("records.loadedFromLocal"));
                    }
                } catch (Exception ignored) {
                    statusLabel.setText(LanguageManager.t("records.loadFailed"));
                }
            }
        };
        loadWorker.execute();
    }
    // Saves the current record list model back to the JSON file.
    private void saveToJson() {
        recordService.saveRecords(Collections.list(recordModel.elements()));
    }
    // Shows a pop-up dialog displaying the selected record details.
    private void showRecordPreview(PatientRecord r) {
        String msg = LanguageManager.t("records.name") + ": " + r.getPatientName() + "\n"
                + LanguageManager.t("records.recordId") + ": " + r.getRecordId() + "\n"
                + LanguageManager.t("records.diagnosis") + ": " + r.getDiagnosis() + "\n"
                + LanguageManager.t("records.date") + ": " + r.getDate();

        JOptionPane.showMessageDialog(
                this, msg, LanguageManager.t("records.details"), JOptionPane.INFORMATION_MESSAGE
        );
    }
    // Reloads records from disk and refreshes the displayed list (e.g., after discharge updates).
    public void reloadFromDisk() {
        loadFromJsonAsync();
    }

    public void dispose() {
        AppEvents.removeRecordsChangedListener(recordsChangedListener);
        if (loadWorker != null && !loadWorker.isDone()) {
            loadWorker.cancel(true);
        }
    }
}
