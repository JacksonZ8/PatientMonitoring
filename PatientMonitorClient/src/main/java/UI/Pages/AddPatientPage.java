package UI.Pages;

import Models.Patients.Patient;
import Services.AppEvents;
import Services.PatientService;
import UI.Components.RoundedButton;
import UI.MainWindow;
import UI.Components.PlaceHolders.PlaceholderTextField;
import UI.Components.Tiles.BaseTile;
import Utilities.LanguageManager;

import javax.swing.*;
import java.awt.*;

public class AddPatientPage extends JPanel {

    private final MainWindow mainWindow;

    private PlaceholderTextField givenNameField;
    private PlaceholderTextField familyNameField;
    private PlaceholderTextField idField;

    private PlaceholderTextField genderField;
    private PlaceholderTextField ageField;
    private PlaceholderTextField bloodPressureField;

    private final PatientService patientService = new PatientService();

    public AddPatientPage(MainWindow mainWindow) {
        this.mainWindow = mainWindow;
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));

        JLabel title = new JLabel(LanguageManager.t("add.title"), SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 28));
        title.setBorder(BorderFactory.createEmptyBorder(30, 0, 30, 0));
        add(title, BorderLayout.NORTH);

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBackground(new Color(245, 245, 245));
        wrapper.add(Box.createVerticalGlue());

        BaseTile form = new BaseTile(720, 800, 45, false);
        form.setBackground(Color.WHITE);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setMaximumSize(new Dimension(720, 800));
        form.setAlignmentX(Component.CENTER_ALIGNMENT);
        form.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));

        givenNameField = field(form, LanguageManager.t("add.givenName"), LanguageManager.t("add.givenPlaceholder"));
        familyNameField = field(form, LanguageManager.t("add.familyName"), LanguageManager.t("add.familyPlaceholder"));
        idField = field(form, LanguageManager.t("add.patientId"), LanguageManager.t("add.idPlaceholder"));

        genderField = field(form, LanguageManager.t("add.gender"), LanguageManager.t("add.genderPlaceholder"));
        ageField = field(form, LanguageManager.t("add.age"), LanguageManager.t("add.agePlaceholder"));
        bloodPressureField = field(form, LanguageManager.t("add.bloodPressure"), LanguageManager.t("add.bpPlaceholder"));

        form.add(Box.createVerticalStrut(25));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        RoundedButton cancelBtn = new RoundedButton(LanguageManager.t("add.cancel"));
        RoundedButton addBtn = new RoundedButton(LanguageManager.t("add.submit"));

        buttonPanel.add(cancelBtn);
        buttonPanel.add(addBtn);
        form.add(buttonPanel);

        wrapper.add(form);
        wrapper.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(wrapper);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);

        cancelBtn.addActionListener(e -> mainWindow.showHomePage());
        addBtn.addActionListener(e -> addPatientToServerAsync(addBtn));
    }

    private PlaceholderTextField field(JPanel parent, String labelText, String placeholder) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Arial", Font.PLAIN, 16));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 10, 5, 0));

        BaseTile tile = new BaseTile(600, 65, 40, false);
        tile.setMaximumSize(new Dimension(600, 65));
        tile.setLayout(new BorderLayout());
        tile.setAlignmentX(Component.LEFT_ALIGNMENT);

        PlaceholderTextField field = new PlaceholderTextField(placeholder);
        field.setFont(new Font("Arial", Font.PLAIN, 16));
        field.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 15));
        field.setOpaque(false);

        tile.add(field, BorderLayout.CENTER);

        parent.add(label);
        parent.add(tile);
        parent.add(Box.createVerticalStrut(15));

        return field;
    }

    private void addPatientToServerAsync(JButton addBtn) {
        String given = safeText(givenNameField.getText());
        String family = safeText(familyNameField.getText());
        String gender = safeText(genderField.getText());
        String ageText = safeText(ageField.getText());
        String bp = safeText(bloodPressureField.getText());

        if (given.isEmpty() || family.isEmpty()) {
            error(LanguageManager.t("add.nameRequired"));
            return;
        }
        if (gender.isEmpty()) {
            error(LanguageManager.t("add.genderRequired"));
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageText);
        } catch (NumberFormatException e) {
            error(LanguageManager.t("add.invalidAge"));
            return;
        }
        if (age < 0 || age > 130) {
            error(LanguageManager.t("add.ageRange"));
            return;
        }

        if (!isValidBloodPressure(bp)) {
            error(LanguageManager.t("add.invalidBp"));
            return;
        }

        addBtn.setEnabled(false);

        final int finalAge = age;
        final String finalBp = bp;
        Patient patient = new Patient(0, given, family, gender, finalAge, finalBp);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                patientService.addPatient(patient);
                return null;
            }

            @Override
            protected void done() {
                addBtn.setEnabled(true);

                try {
                    get();

                    JOptionPane.showMessageDialog(AddPatientPage.this,
                            LanguageManager.t("add.success"),
                            LanguageManager.t("add.successTitle"),
                            JOptionPane.INFORMATION_MESSAGE);

                    clear();
                    AppEvents.firePatientsChanged();
                    mainWindow.showHomePage();

                } catch (Exception ex) {
                    error(LanguageManager.t("add.failedPrefix") + ex.getMessage());
                }
            }
        };

        worker.execute();
    }

    private String safeText(String s) {
        if (s == null) return "";
        s = s.trim();

        if (s.equalsIgnoreCase(LanguageManager.t("add.givenPlaceholder"))) return "";
        if (s.equalsIgnoreCase(LanguageManager.t("add.familyPlaceholder"))) return "";
        if (s.equalsIgnoreCase(LanguageManager.t("add.idPlaceholder"))) return "";
        if (s.equalsIgnoreCase(LanguageManager.t("add.genderPlaceholder"))) return "";
        if (s.equalsIgnoreCase(LanguageManager.t("add.agePlaceholder"))) return "";
        if (s.equalsIgnoreCase(LanguageManager.t("add.bpPlaceholder"))) return "";

        return s;
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, LanguageManager.t("add.inputError"), JOptionPane.ERROR_MESSAGE);
    }

    private boolean isValidBloodPressure(String bp) {
        if (bp == null || !bp.matches("\\d{2,3}/\\d{2,3}")) return false;
        try {
            String[] parts = bp.split("/");
            int sys = Integer.parseInt(parts[0]);
            int dia = Integer.parseInt(parts[1]);
            return sys >= 80 && sys <= 250 && dia >= 40 && dia <= 150 && sys > dia;
        } catch (Exception e) {
            return false;
        }
    }

    private void clear() {
        givenNameField.setText("");
        familyNameField.setText("");
        idField.setText("");
        genderField.setText("");
        ageField.setText("");
        bloodPressureField.setText("");
    }
}
