package UI.Pages;

import NetWork.Session;

import Models.DoctorProfile;
import Services.AccountMetaService;
import Services.AppEvents;
import Services.DoctorProfileService;
import UI.Components.PatientRecordsPanel;
import UI.Components.PlaceHolders.PlaceholderTextField;
import UI.Components.RoundedButton;
import UI.Components.Tiles.BaseTile;
import UI.MainWindow;
import UI.PageLifecycle;
import Utilities.LanguageManager;
import Utilities.SettingManager;
import Utilities.SwingStyle;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

public class AccountPage extends JPanel implements PageLifecycle {

    private final MainWindow window;

    private CardLayout cardLayout;
    private JPanel cardPanel;
    private PatientRecordsPanel recordsPanel;

    private final Color themeBg = new Color(245, 247, 250);
    private static final Color DARK_BG = new Color(24, 26, 30);
    private static final Color DARK_CARD = new Color(32, 35, 41);
    private static final Color DARK_FIELD = new Color(45, 48, 56);
    private static final Color DARK_TEXT = new Color(235, 235, 235);
    private static final Color DARK_MUTED = new Color(170, 170, 170);
    private Container formRoot;

    // Existing profile
    private final DoctorProfileService profileService = new DoctorProfileService();
    private DoctorProfile profile;

    // New meta (createdAt/lastLogin/avatar)
    private final AccountMetaService metaService = new AccountMetaService();
    private AccountMetaService.Meta meta;

    // UI fields (screenshot-like)
    private PlaceholderTextField fullNameField;
    private PlaceholderTextField emailField;
    private PlaceholderTextField organizationField;
    private JComboBox<String> roleCombo;
    private RoundedButton saveBtn;
    private RoundedButton recordsBtn;
    private JButton changePhotoBtn;
    private JButton removePhotoBtn;

    private JLabel headerName;
    private JLabel headerEmail;
    private StatusBadge statusBadge;
    private AvatarCircle avatarCircle;

    private MetadataCard userIdCard;
    private MetadataCard lastLoginCard;
    private MetadataCard createdCard;

    // state for dirty check
    private String originalFullName = "";
    private String originalOrg = "";
    private String originalRole = "";
    private final Runnable profileChangedListener = this::reloadProfile;
    private final Runnable settingsChangedListener = this::applyThemeToInputs;

    public AccountPage(MainWindow window) {
        this.window = window;

        setLayout(new BorderLayout());
        setBackground(themeBg);

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(themeBg);

        JPanel accountMain = buildAccountMain();

        recordsPanel = new PatientRecordsPanel(() ->
                cardLayout.show(cardPanel, "account")
        );

        cardPanel.add(accountMain, "account");
        cardPanel.add(recordsPanel, "records");

        add(cardPanel, BorderLayout.CENTER);
        cardLayout.show(cardPanel, "account");
        AppEvents.addProfileChangedListener(profileChangedListener);
        AppEvents.addSettingsChangedListener(settingsChangedListener);
    }

    // ===================== MAIN ACCOUNT PAGE  =====================
    private JPanel buildAccountMain() {
        boolean dark = new SettingManager().isDarkMode();
        Color bg = dark ? DARK_BG : themeBg;
        Color cardBg = dark ? DARK_CARD : Color.WHITE;
        Color text = dark ? DARK_TEXT : new Color(17, 24, 39);
        Color muted = dark ? DARK_MUTED : new Color(107, 114, 128);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(bg);

        // ---------- Header ----------
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(bg);
        header.setBorder(BorderFactory.createEmptyBorder(30, 60, 18, 60));

        JLabel title = new JLabel(LanguageManager.t("account.title"));
        title.setFont(new Font("Arial", Font.BOLD, 34));
        title.setForeground(text);

        JLabel subtitle = new JLabel(LanguageManager.t("account.subtitle"));
        subtitle.setFont(new Font("Arial", Font.PLAIN, 16));
        subtitle.setForeground(muted);

        header.add(title);
        header.add(Box.createVerticalStrut(8));
        header.add(subtitle);

        root.add(header, BorderLayout.NORTH);

        // ---------- Scrollable content ----------
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(bg);

        // ========== Load data ==========
        this.profile = loadProfileFromDbOrFallback();
        this.meta = metaService.loadOrInit();

        // ---------- Main white card ----------
        BaseTile card = new BaseTile(1200, 600, 45, false);
        card.setMaximumSize(new Dimension(1200, 600));
        card.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.setLayout(new BorderLayout());
        card.setBackground(cardBg);
        card.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        this.formRoot = card;

        // ----- top row inside card: avatar + name/email + buttons + status -----
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        // Left section: avatar (far left) + name/email centered to avatar + buttons below avatar
        JPanel leftProfile = new JPanel(new GridBagLayout());
        leftProfile.setOpaque(false);

        GridBagConstraints lp = new GridBagConstraints();
        lp.insets = new Insets(0, 0, 0, 0);
        lp.fill = GridBagConstraints.NONE;
        lp.anchor = GridBagConstraints.WEST;

        // ---- Avatar ----
        avatarCircle = new AvatarCircle(74);
        avatarCircle.setImagePath(meta.avatarPath);

        lp.gridx = 0;
        lp.gridy = 0;
        lp.weightx = 0;
        lp.weighty = 0;
        leftProfile.add(avatarCircle, lp);

        // ---- Name/Email: vertically centered relative to avatar ----
        JPanel nameCol = new JPanel();
        nameCol.setOpaque(false);
        nameCol.setLayout(new BoxLayout(nameCol, BoxLayout.Y_AXIS));
        nameCol.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        String displayName = Session.getDoctorFullName();
        if (displayName == null || displayName.isBlank() || "demo".equalsIgnoreCase(displayName.trim())) {
            displayName = (profile == null ? "" : profile.getFullName());
        }
        headerName = new JLabel(displayName);
        headerName.setFont(new Font("Arial", Font.BOLD, 22));
        headerName.setForeground(text);

        String displayEmail = Session.getDoctorEmail();
        if (displayEmail == null || displayEmail.isBlank() || "demo".equalsIgnoreCase(displayEmail.trim())) {
            displayEmail = (profile == null ? "" : profile.getEmail());
        }
        headerEmail = new JLabel(displayEmail);
        headerEmail.setFont(new Font("Arial", Font.PLAIN, 14));
        headerEmail.setForeground(muted);

        // Add vertical glue so the block (name+email) is centered within the avatar height
        nameCol.add(Box.createVerticalGlue());
        nameCol.add(headerName);
        nameCol.add(Box.createVerticalStrut(4));
        nameCol.add(headerEmail);
        nameCol.add(Box.createVerticalGlue());

        lp.gridx = 0;
        lp.gridy = 0;
        lp.weightx = 0;
        lp.fill = GridBagConstraints.HORIZONTAL;
        lp.insets = new Insets(0, 90, 0, 0); // gap between avatar and text
        leftProfile.add(nameCol, lp);

        // ---- Buttons: lower, under avatar only (doesn't affect name/email alignment) ----
        JPanel photoBtnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        photoBtnRow.setOpaque(false);
        changePhotoBtn = smallOutlineButton(LanguageManager.t("account.changePhoto"));
        removePhotoBtn = smallOutlineButton(LanguageManager.t("account.remove"));
        photoBtnRow.add(changePhotoBtn);
        photoBtnRow.add(removePhotoBtn);

        lp.gridx = 0;
        lp.gridy = 1;
        lp.weightx = 0;
        lp.fill = GridBagConstraints.NONE;
        lp.insets = new Insets(14, 0, 0, 0); // push buttons down
        leftProfile.add(photoBtnRow, lp);

        // filler so row 1 doesn't stretch text column
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        lp.gridx = 1;
        lp.gridy = 1;
        lp.weightx = 1;
        lp.fill = GridBagConstraints.HORIZONTAL;
        lp.insets = new Insets(14, 0, 0, 0);
        leftProfile.add(filler, lp);

        topRow.add(leftProfile, BorderLayout.WEST);

        JPanel rightTop = new JPanel();
        rightTop.setOpaque(false);
        rightTop.setLayout(new BoxLayout(rightTop, BoxLayout.Y_AXIS));

        JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        badgeRow.setOpaque(false);
        statusBadge = new StatusBadge(LanguageManager.t("account.active"));
        badgeRow.add(statusBadge);

        JPanel rightBtnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        rightBtnRow.setOpaque(false);

        recordsBtn = new RoundedButton(LanguageManager.t("account.patientRecords"));
        recordsBtn.setRadius(18);
        recordsBtn.setFont(new Font("Arial", Font.PLAIN, 14));
        recordsBtn.setColors(
                new Color(243, 244, 246),
                new Color(229, 231, 235),
                new Color(209, 213, 219),
                new Color(31, 41, 55)
        );

        rightBtnRow.add(recordsBtn);

        rightTop.add(badgeRow);
        rightTop.add(rightBtnRow);

        topRow.add(rightTop, BorderLayout.EAST);

        card.add(topRow, BorderLayout.NORTH);

        // ----- form area -----
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 0, 12, 20);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        // Fields mapped to your model:
        // - FULL NAME -> first+last
        // - EMAIL -> email (read-only recommended)
        // - ORGANIZATION -> specialty (closest field you have)
        // - ROLE -> dropdown (not in model; we store only UI state by default)
        // NOTE: PlaceholderTextField's constructor is for the placeholder text, not the initial value.
        // If we pass data into the constructor, it may only paint visually but getText() stays empty,
        // so saving won't persist. Always set placeholder + setText(value).
        fullNameField = new PlaceholderTextField(LanguageManager.t("account.fullNamePlaceholder"));
        fullNameField.setText(headerName == null ? (profile == null ? "" : profile.getFullName()) : headerName.getText());

        emailField = new PlaceholderTextField(LanguageManager.t("account.emailPlaceholder"));
        emailField.setText(profile == null ? "" : profile.getEmail());

        organizationField = new PlaceholderTextField(LanguageManager.t("account.organizationPlaceholder"));
        organizationField.setText(profile == null ? "" : profile.getOrgnization());

        roleCombo = new JComboBox<>(new String[]{
                "Clinician", "Consultant", "Surgeon", "Nurse", "Admin", "Viewer"
        });
        String initialRole = (meta != null && meta.role != null && !meta.role.isBlank()) ? meta.role : "Clinician";
        roleCombo.setSelectedItem(initialRole);

        // Sync role to Session so TopBar can display it
        Session.setDoctorRole(initialRole);
        window.getTopBar().updateDoctorInfo(headerName.getText(), Session.getDoctorRole());
        styleComboBox(roleCombo, dark);

        int row = 0;
        // row 0
        addLabeledInput(form, LanguageManager.t("account.fullName"), fullNameField, gbc, 0, row);
        addLabeledInput(form, LanguageManager.t("account.emailAddress"), emailField, gbc, 1, row++);
        // row 1
        addLabeledInput(form, LanguageManager.t("account.organization"), organizationField, gbc, 0, row);
        addLabeledCombo(form, LanguageManager.t("account.role"), roleCombo, gbc, 1, row++);

        // Save button row
        JPanel saveRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        saveRow.setOpaque(false);
        saveRow.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        saveBtn = new RoundedButton(LanguageManager.t("account.saveChanges"));
        saveBtn.setRadius(18);
        saveBtn.setFont(new Font("Arial", Font.PLAIN, 16));
        saveBtn.setColors(
                new Color(37, 99, 235),
                new Color(59, 130, 246),
                new Color(29, 78, 216),
                Color.WHITE
        );
        saveBtn.setEnabled(false);

        saveRow.add(saveBtn);

        JPanel centerWrap = new JPanel(new BorderLayout());
        centerWrap.setOpaque(false);
        centerWrap.add(form, BorderLayout.CENTER);
        centerWrap.add(saveRow, BorderLayout.SOUTH);

        card.add(centerWrap, BorderLayout.CENTER);

        content.add(card);
        content.add(Box.createVerticalStrut(22));

        // ---------- Metadata card ----------
        BaseTile metaCard = new BaseTile(1200, 260, 45, false);
        metaCard.setMaximumSize(new Dimension(1200, 260));
        metaCard.setAlignmentX(Component.CENTER_ALIGNMENT);
        metaCard.setLayout(new BorderLayout());
        metaCard.setBackground(cardBg);
        metaCard.setBorder(BorderFactory.createEmptyBorder(22, 28, 22, 28));

        JLabel metaTitle = new JLabel(LanguageManager.t("account.metadata"));
        metaTitle.setFont(new Font("Arial", Font.BOLD, 20));
        metaTitle.setForeground(text);
        metaCard.add(metaTitle, BorderLayout.NORTH);

        JPanel metaGrid = new JPanel(new GridLayout(1, 3, 18, 0));
        metaGrid.setOpaque(false);
        metaGrid.setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        String userId = profile.getIdNumber() == null || profile.getIdNumber().isBlank()
                ? "DOC-UNKNOWN" : profile.getIdNumber();

        userIdCard = new MetadataCard(LanguageManager.t("account.userId"), userId, "");
        lastLoginCard = new MetadataCard(LanguageManager.t("account.lastLogin"), humanizeAgo(meta.lastLogin), formatDateTime(meta.lastLogin));
        createdCard = new MetadataCard(LanguageManager.t("account.created"), formatDate(meta.createdAt), daysAgo(meta.createdAt) + " " + LanguageManager.t("account.daysAgo"));

        metaGrid.add(userIdCard);
        metaGrid.add(lastLoginCard);
        metaGrid.add(createdCard);

        metaCard.add(metaGrid, BorderLayout.CENTER);

        content.add(metaCard);
        content.add(Box.createVerticalStrut(40));

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(bg);

        root.add(scroll, BorderLayout.CENTER);

        // ---------- Behaviour ----------
        // email should be read-only like screenshot
        emailField.setEditable(false);
        emailField.setOpaque(false);

        // name should also be read-only (avoid DB write-back complexity)
        fullNameField.setEditable(false);
        fullNameField.setOpaque(false);

        // store original for dirty check
        originalFullName = safe(fullNameField);
        originalOrg = safe(organizationField);
        originalRole = Objects.toString(roleCombo.getSelectedItem(), "");

        // listeners => dirty check
        DocumentListener dl = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { refreshSaveEnabled(); }
            @Override public void removeUpdate(DocumentEvent e) { refreshSaveEnabled(); }
            @Override public void changedUpdate(DocumentEvent e) { refreshSaveEnabled(); }
        };
        fullNameField.getDocument().addDocumentListener(dl);
        organizationField.getDocument().addDocumentListener(dl);
        roleCombo.addActionListener(e -> refreshSaveEnabled());

        saveBtn.addActionListener(e -> saveDoctorProfileFromScreenshotForm());

        recordsBtn.addActionListener(e -> {
            recordsPanel.reloadFromDisk();
            cardLayout.show(cardPanel, "records");
        });

        changePhotoBtn.addActionListener(e -> onChangePhoto());
        removePhotoBtn.addActionListener(e -> onRemovePhoto());

        fixAccountTheme(root, dark);
        return root;
    }

    // ===================== DB -> DoctorProfile loading =====================
    private DoctorProfile loadProfileFromDbOrFallback() {
        // Client app should not query the server DB directly.
        // Use the locally stored profile as base, and fill missing identity from Session.
        DoctorProfile base = profileService.load();
        if (base == null) base = DoctorProfile.defaults();

        try {
            String email = Session.getDoctorEmail();
            if (email != null && !email.isBlank() && !"demo".equalsIgnoreCase(email.trim())) {
                // Email from login/session should be the source of truth for display.
                base.setEmail(email.trim());

                // If we don't have a stable idNumber yet, derive one from the email hash (display-only).
                if (base.getIdNumber() == null || base.getIdNumber().isBlank()) {
                    base.setIdNumber("DOC-" + Math.abs(email.trim().hashCode()));
                }
            }
        } catch (Exception ignored) {}

        return base;
    }

    // ===================== Save logic (maps back to your DoctorProfile) =====================
    private void saveDoctorProfileFromScreenshotForm() {
        if (profile == null) profile = DoctorProfile.defaults();

        // Name/email are read-only, so we keep existing values

        // Map ORGANIZATION -> specialty
        profile.setOrgnization(safe(organizationField));

        // Name/email are read-only; keep the current display text
        headerName.setText(headerName.getText());
        headerEmail.setText(headerEmail.getText());

        // Update TopBar
        window.getTopBar().updateDoctorInfo(headerName.getText(), Session.getDoctorRole());

        // Save selected role to meta and persist
        String selectedRole = Objects.toString(roleCombo.getSelectedItem(), "");
        meta.role = selectedRole;
        metaService.saveRole(selectedRole);

        Session.setDoctorRole(selectedRole);
        profileService.save(profile);

        // Update originals & button
        originalFullName = safe(fullNameField);
        originalOrg = safe(organizationField);
        originalRole = Objects.toString(roleCombo.getSelectedItem(), "");
        refreshSaveEnabled();
        AppEvents.fireProfileChanged();

        JOptionPane.showMessageDialog(this, LanguageManager.t("account.profileSaved"));
    }

    private void refreshSaveEnabled() {
        boolean dirty =
                !Objects.equals(safe(fullNameField), originalFullName) ||
                        !Objects.equals(safe(organizationField), originalOrg) ||
                        !Objects.equals(Objects.toString(roleCombo.getSelectedItem(), ""), originalRole);

        saveBtn.setEnabled(dirty);
        // optional: gray-out style could be added here if your RoundedButton supports it
    }

    // ===================== Avatar actions =====================
    private void onChangePhoto() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(LanguageManager.t("account.chooseAvatar"));
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter(
                LanguageManager.t("settings.imageFilter"), "png", "jpg", "jpeg"
        ));
        int res = chooser.showOpenDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) return;

        File chosen = chooser.getSelectedFile();
        try {
            // save to ~/.patientmonitor/avatar.png
            Path dir = metaService.getDirPath();
            Files.createDirectories(dir);
            Path out = dir.resolve("avatar.png");

            // copy (we keep extension .png; if user selects jpg it still works for ImageIO reading often,
            // but safer would be re-encode; keeping minimal: just copy)
            Files.copy(chosen.toPath(), out, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            meta.avatarPath = out.toString();
            metaService.saveAvatarPath(meta.avatarPath);

            avatarCircle.setImagePath(meta.avatarPath);
            avatarCircle.repaint();
            AppEvents.fireProfileChanged();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, LanguageManager.t("account.avatarFailed") + ex.getMessage(),
                    LanguageManager.t("account.error"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onRemovePhoto() {
        try {
            if (meta.avatarPath != null) {
                Files.deleteIfExists(Path.of(meta.avatarPath));
            }
        } catch (Exception ignored) {}

        meta.avatarPath = null;
        metaService.saveAvatarPath(null);

        avatarCircle.setImagePath(null);
        avatarCircle.repaint();
        AppEvents.fireProfileChanged();
    }

    // ===================== Reload when page shown =====================
    public void reloadProfile() {
        DoctorProfile p = loadProfileFromDbOrFallback();
        this.profile = p;

        if (fullNameField != null) {
            String displayName = Session.getDoctorFullName();
            if (displayName == null || displayName.isBlank() || "demo".equalsIgnoreCase(displayName.trim())) {
                displayName = (p == null ? "" : p.getFullName());
            }
            fullNameField.setText(displayName);
        }
        if (organizationField != null) organizationField.setText(p.getOrgnization());

        // Keep email consistent everywhere: prefer Session email when available.
        String em = Session.getDoctorEmail();
        if (em == null || em.isBlank() || "demo".equalsIgnoreCase(em.trim())) {
            em = p.getEmail();
        } else {
            em = em.trim();
        }
        if (emailField != null) emailField.setText(em == null ? "" : em);

        if (headerName != null) {
            String displayName = Session.getDoctorFullName();
            if (displayName == null || displayName.isBlank() || "demo".equalsIgnoreCase(displayName.trim())) {
                displayName = (p == null ? "" : p.getFullName());
            }
            headerName.setText(displayName);
        }
        if (headerEmail != null) {
            headerEmail.setText(em == null ? "" : em);
        }

        // update user id card if you want
        if (userIdCard != null) {
            userIdCard.setTitleText(LanguageManager.t("account.userId"));
            String userId = p.getIdNumber() == null || p.getIdNumber().isBlank() ? "DOC-UNKNOWN" : p.getIdNumber();
            userIdCard.setValueText(userId);
        }

        // refresh meta
        meta = metaService.loadOrInit();
        if (lastLoginCard != null) {
            lastLoginCard.setTitleText(LanguageManager.t("account.lastLogin"));
            lastLoginCard.setValueText(humanizeAgo(meta.lastLogin));
            lastLoginCard.setSubText(formatDateTime(meta.lastLogin));
        }
        if (createdCard != null) {
            createdCard.setTitleText(LanguageManager.t("account.created"));
            createdCard.setValueText(formatDate(meta.createdAt));
            createdCard.setSubText(daysAgo(meta.createdAt) + " " + LanguageManager.t("account.daysAgo"));
        }

        // re-apply saved role to combo
        if (roleCombo != null) {
            String initialRole = (meta != null && meta.role != null && !meta.role.isBlank()) ? meta.role : Objects.toString(roleCombo.getSelectedItem(), "Clinician");
            roleCombo.setSelectedItem(initialRole);

            // Sync role to Session so TopBar can display it
            Session.setDoctorRole(initialRole);
            window.getTopBar().updateDoctorInfo(headerName == null ? (p == null ? "" : p.getFullName()) : headerName.getText(), Session.getDoctorRole());
        }

        // originals reset
        originalFullName = safe(fullNameField);
        originalOrg = safe(organizationField);
        originalRole = Objects.toString(roleCombo.getSelectedItem(), "");
        refreshSaveEnabled();
        if (fullNameField != null) {
            fullNameField.setEditable(false);
            fullNameField.setOpaque(false);
        }
        if (emailField != null) {
            emailField.setEditable(false);
            emailField.setOpaque(false);
        }
    }

    @Override
    public void onShown() {
        reloadProfile();
    }

    @Override
    public void disposePage() {
        AppEvents.removeProfileChangedListener(profileChangedListener);
        AppEvents.removeSettingsChangedListener(settingsChangedListener);
        if (recordsPanel != null) {
            recordsPanel.dispose();
        }
    }

    // ===================== Field builders =====================
    private void addLabeledInput(JPanel parent, String labelText, JComponent field,
                                 GridBagConstraints gbc, int col, int row) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Arial", Font.BOLD, 12));
        label.setForeground(new Color(107, 114, 128));

        gbc.gridx = col;
        gbc.gridy = row * 2;
        parent.add(label, gbc);

        BaseTile tile = new BaseTile(520, 80, 38, false);
        tile.setLayout(new BorderLayout());
        tile.setBackground(Color.WHITE);

        field.setFont(new Font("Arial", Font.PLAIN, 16));
        field.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        field.setOpaque(false);

        gbc.gridy = row * 2 + 1;
        parent.add(tile, gbc);
        tile.add(field, BorderLayout.CENTER);
    }

    private void addLabeledCombo(JPanel parent, String labelText, JComboBox<String> combo,
                                 GridBagConstraints gbc, int col, int row) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Arial", Font.BOLD, 12));
        label.setForeground(new Color(107, 114, 128));

        gbc.gridx = col;
        gbc.gridy = row * 2;
        parent.add(label, gbc);

        // Use a simple bordered panel instead of BaseTile to avoid clipping the combo UI
        JPanel box = new JPanel(new BorderLayout());
        box.setBackground(Color.WHITE);
        box.setOpaque(false);
        box.setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 12));

        combo.setFont(new Font("Arial", Font.PLAIN, 16));
        styleComboBox(combo, new SettingManager().isDarkMode());
        combo.setPreferredSize(new Dimension(520, 60));

        box.add(combo, BorderLayout.CENTER);

        gbc.gridy = row * 2 + 1;
        parent.add(box, gbc);
    }

    // ===================== Theme helpers =====================
    private void fixInputColors(Container root, boolean dark) {
        Color inputFg = dark ? new Color(235, 235, 235) : Color.BLACK;
        Color tileBg = dark ? new Color(45, 48, 56) : Color.WHITE;

        for (Component c : root.getComponents()) {
            if (c instanceof BaseTile bt) bt.setBackground(tileBg);
            if (c instanceof JTextField tf) {
                tf.setForeground(inputFg);
                tf.setCaretColor(inputFg);
            }
            if (c instanceof JPasswordField pf) {
                pf.setForeground(inputFg);
                pf.setCaretColor(inputFg);
            }
            if (c instanceof Container child) fixInputColors(child, dark);
        }
    }

    public void applyThemeToInputs() {
        boolean dark = new SettingManager().isDarkMode();
        if (formRoot != null) fixInputColors(formRoot, dark);
    }

    private void fixAccountTheme(Container root, boolean dark) {
        Color bg = dark ? DARK_BG : themeBg;
        Color cardBg = dark ? DARK_CARD : Color.WHITE;
        Color fieldBg = dark ? DARK_FIELD : Color.WHITE;
        Color fg = dark ? DARK_TEXT : Color.BLACK;
        Color muted = dark ? DARK_MUTED : new Color(107, 114, 128);

        root.setBackground(bg);
        for (Component c : root.getComponents()) {
            if (c instanceof BaseTile bt) {
                bt.setBackground(cardBg);
            } else if (c instanceof JPanel p) {
                if (p.isOpaque()) p.setBackground(bg);
            } else if (c instanceof JScrollPane sp) {
                sp.getViewport().setBackground(bg);
            } else if (c instanceof JTextField tf) {
                tf.setForeground(fg);
                tf.setCaretColor(fg);
                tf.setBackground(fieldBg);
            } else if (c instanceof JComboBox<?> combo) {
                SwingStyle.styleComboBox(combo, dark);
            } else if (c instanceof JLabel label) {
                label.setForeground(label.getFont() != null && label.getFont().getSize() <= 14 ? muted : fg);
            }
            if (c instanceof Container child) fixAccountTheme(child, dark);
        }
    }

    private void styleComboBox(JComboBox<String> combo, boolean dark) {
        SwingStyle.styleComboBox(combo, dark);
    }

    // ===================== small UI bits =====================
    private JButton smallOutlineButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Arial", Font.BOLD, 13));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setBackground(new Color(243, 244, 246));
        b.setForeground(new Color(37, 99, 235));
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(229, 231, 235)),
                BorderFactory.createEmptyBorder(8, 14, 8, 14)
        ));
        return b;
    }

    private String safe(JTextField tf) {
        return tf == null ? "" : tf.getText().trim();
    }

    // ===================== formatting helpers =====================
    private String humanizeAgo(LocalDateTime t) {
        if (t == null) return "";
        Duration d = Duration.between(t, LocalDateTime.now());
        long minutes = Math.max(0, d.toMinutes());
        if (minutes < 60) return minutes + " " + LanguageManager.t("account.minutesAgo");
        long hours = minutes / 60;
        if (hours < 24) return hours + " " + LanguageManager.t("account.hoursAgo");
        long days = hours / 24;
        return days + " " + LanguageManager.t("account.daysAgo");
    }

    private String formatDateTime(LocalDateTime t) {
        if (t == null) return "";
        if (isChinese()) return t.format(DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.CHINA));
        return t.format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm", currentLocale()));
    }

    private String formatDate(LocalDateTime t) {
        if (t == null) return "";
        if (isChinese()) return t.format(DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA));
        return t.format(DateTimeFormatter.ofPattern("MMM d, yyyy", currentLocale()));
    }

    private long daysAgo(LocalDateTime t) {
        if (t == null) return 0;
        Duration d = Duration.between(t, LocalDateTime.now());
        return Math.max(0, d.toDays());
    }

    private Locale currentLocale() {
        return isChinese() ? Locale.CHINA : Locale.ENGLISH;
    }

    private boolean isChinese() {
        return "zh".equalsIgnoreCase(LanguageManager.getLanguage());
    }

    // ===================== inner components (no extra files needed) =====================
    private static class StatusBadge extends JPanel {
        private final boolean dark = new SettingManager().isDarkMode();

        public StatusBadge(String text) {
            setOpaque(false);
            setLayout(new FlowLayout(FlowLayout.LEFT, 8, 6));
            setPreferredSize(new Dimension(120, 20));
            setMinimumSize(new Dimension(120, 20));
            JLabel dot = new JLabel("●");
            dot.setForeground(dark ? new Color(74, 222, 128) : new Color(34, 197, 94));
            JLabel label = new JLabel(text);
            label.setForeground(dark ? new Color(187, 247, 208) : new Color(22, 101, 52));
            label.setFont(new Font("Arial", Font.BOLD, 13));
            add(dot);
            add(label);
            setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 30));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(dark ? new Color(22, 72, 50) : new Color(220, 252, 231));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(d.width + 14, d.height + 6);
        }
    }

    private static class MetadataCard extends BaseTile {
        private final JLabel title = new JLabel();
        private final JLabel value = new JLabel();
        private final JLabel sub = new JLabel();

        public MetadataCard(String title, String valueText, String subText) {
            super(1, 1, 26, false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

            this.title.setText(title);
            this.title.setFont(new Font("Arial", Font.BOLD, 12));
            this.title.setForeground(new SettingManager().isDarkMode() ? DARK_MUTED : new Color(107, 114, 128));

            value.setText(valueText);
            value.setFont(new Font("Arial", Font.BOLD, 16));
            value.setForeground(new SettingManager().isDarkMode() ? DARK_TEXT : new Color(17, 24, 39));

            sub.setText(subText);
            sub.setFont(new Font("Arial", Font.PLAIN, 12));
            sub.setForeground(new SettingManager().isDarkMode() ? DARK_MUTED : new Color(107, 114, 128));

            add(this.title);
            add(Box.createVerticalStrut(12));
            add(value);
            add(Box.createVerticalStrut(6));
            add(sub);
        }

        public void setValueText(String v) { value.setText(v); }
        public void setSubText(String s) { sub.setText(s); }
        public void setTitleText(String s) { title.setText(s); }
    }

    private static class AvatarCircle extends JComponent {
        private final int size;
        private String imagePath;

        public AvatarCircle(int size) {
            this.size = size;
            setPreferredSize(new Dimension(size, size));
            setMinimumSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
        }

        public void setImagePath(String path) {
            this.imagePath = (path == null || path.isBlank()) ? null : path;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // ring
            g2.setColor(new Color(229, 231, 235));
            g2.fillOval(0, 0, w, h);

            int pad = 3;
            Shape clip = new Ellipse2D.Double(pad, pad, w - pad * 2, h - pad * 2);
            g2.setClip(clip);

            boolean drawn = false;
            if (imagePath != null) {
                try {
                    Image img = ImageIO.read(new File(imagePath));
                    if (img != null) {
                        Image scaled = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
                        g2.drawImage(scaled, 0, 0, null);
                        drawn = true;
                    }
                } catch (Exception ignored) {}
            }

            if (!drawn) {
                // fallback initials
                g2.setColor(new Color(243, 244, 246));
                g2.fillOval(0, 0, w, h);
                g2.setColor(new Color(107, 114, 128));
                g2.setFont(new Font("Arial", Font.BOLD, 18));
                String s = "DR";
                FontMetrics fm = g2.getFontMetrics();
                int x = (w - fm.stringWidth(s)) / 2;
                int y = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(s, x, y);
            }

            g2.setClip(null);
            g2.dispose();
        }
    }
}
