package UI.Pages;
import NetWork.ApiClient;
import NetWork.Session;

import UI.MainWindow;
import UI.Components.RoundedButton;
import UI.Components.RoundedPanel;
import Utilities.LanguageManager;
import Utilities.SettingManager;
import Utilities.SwingStyle;
import Utilities.ThemeManager;

import Models.DoctorProfile;
import Services.AccountMetaService;
import Services.AppEvents;
import Services.DoctorProfileService;
import UI.PageLifecycle;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Settings page (clean version) + Avatar upload:
 * - Default avatar: drawn initials circle
 * - Click avatar to upload image (png/jpg/jpeg)
 * - Image is copied into user home folder (.patient-monitor/avatars)
 * - Path persisted in SettingManager (avatarPath)
 */
public class SettingsPage extends JPanel implements PageLifecycle {

    // Dark palette
    private static final Color DARK_BG = new Color(24, 26, 30);
    private static final Color DARK_CARD = new Color(32, 35, 41);
    private static final Color DARK_TEXT = new Color(235, 235, 235);
    private static final Color DARK_MUTED = new Color(170, 170, 170);

    private final MainWindow window;
    private final SettingManager settings = new SettingManager();

    private JCheckBox darkModeToggle;
    private JComboBox<String> languageDropdown;
    private JPasswordField deletePasswordField;
    private RoundedButton deleteAccountButton;

    // Keep SettingsPage identity/avatar in sync with AccountPage
    private final DoctorProfileService profileService = new DoctorProfileService();
    private final AccountMetaService metaService = new AccountMetaService();
    private AccountMetaService.Meta meta;

    // Resolved display values (from Session first, then local profile fallback)
    private String displayName = "Doctor";
    private String displayEmail = "";

    // Avatar label (clickable)
    private JLabel avatarLabel;

    // Prevent refresh recursion when page becomes visible
    private boolean refreshing = false;
    private final Runnable profileChangedListener = this::refreshPageLater;

    // Layout constants
    private static final int COLUMN_W = 540;
    public SettingsPage(MainWindow window) {
        this.window = window;

        // Load account meta (avatar path) and resolve display identity
        this.meta = metaService.loadOrInit();
        resolveDisplayIdentity();

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // Load persisted settings
        LanguageManager.setLanguage(settings.getLanguage());

        add(wrapScrollable(buildMainCard()), BorderLayout.CENTER);

        // Apply theme on load
        ThemeManager.apply(window, settings.isDarkMode());
        applyLocalTheme();
        AppEvents.addProfileChangedListener(profileChangedListener);
    }

    // Main UI builder
    private JComponent buildMainCard() {
        resolveDisplayIdentity();
        boolean dark = settings.isDarkMode();
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setOpaque(true);
        outer.setBackground(dark ? DARK_BG : Color.WHITE);
        outer.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        RoundedPanel card = new RoundedPanel(new BorderLayout());
        card.setFillColor(dark ? DARK_CARD : Color.WHITE);
        card.setBorder(BorderFactory.createEmptyBorder(28, 28, 28, 28));
        card.setPreferredSize(new Dimension(760, 760));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // Column controls overall width & alignment
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setAlignmentX(Component.CENTER_ALIGNMENT);
        column.setMaximumSize(new Dimension(COLUMN_W, Integer.MAX_VALUE));

        JLabel title = new JLabel(LanguageManager.t("settings.title"));
        title.setFont(new Font("Dialog", Font.BOLD, 32));
        title.setForeground(dark ? DARK_TEXT : Color.BLACK);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        column.add(title);
        column.add(Box.createVerticalStrut(22));

        // Profile header
        JComponent profile = buildProfileHeader();
        profile.setAlignmentX(Component.CENTER_ALIGNMENT);
        column.add(profile);
        column.add(Box.createVerticalStrut(22));

        // Dark mode + Language
        initControls(); // IMPORTANT: init before adding rows

        column.add(buildGroupCard(
                buildToggleRow(LanguageManager.t("settings.darkMode"), darkModeToggle),
                buildDropdownRow(LanguageManager.t("settings.language"), languageDropdown)
        ));
        column.add(Box.createVerticalStrut(22));

        // Danger zone: permanent delete account
        column.add(buildDeleteAccountCard());
        column.add(Box.createVerticalStrut(22));

        // Buttons: Reset + Log out
        column.add(buildButtonsRow());
        column.add(Box.createVerticalGlue());

        content.add(column);

        card.add(content, BorderLayout.CENTER);
        outer.add(card);

        // Theme apply on this built card
        // applyLocalTheme(card, title);

        return outer;
    }

    private void initControls() {
        // Dark mode toggle
        darkModeToggle = new JCheckBox();
        darkModeToggle.setOpaque(false);
        darkModeToggle.setSelected(settings.isDarkMode());
        darkModeToggle.addActionListener(e -> {
            settings.setDarkMode(darkModeToggle.isSelected());
            ThemeManager.apply(window, settings.isDarkMode());

            // keep your previous call if exists
            try {
                window.getAccountPage().applyThemeToInputs();
            } catch (Exception ignored) {}

            AppEvents.fireSettingsChanged();
            refreshPageLater();
        });

        // Language dropdown
        languageDropdown = new JComboBox<>(new String[]{"en", "zh"});
        languageDropdown.setSelectedItem(settings.getLanguage());
        languageDropdown.setFont(new Font("Dialog", Font.PLAIN, 16));
        languageDropdown.setPreferredSize(new Dimension(160, 34));

        languageDropdown.addActionListener(e -> {
            String lang = (String) languageDropdown.getSelectedItem();
            settings.setLanguage(lang);
            LanguageManager.setLanguage(lang);
            AppEvents.fireSettingsChanged();
            window.rebuildPagesForLanguage(MainWindow.PAGE_SETTINGS);
        });

        styleLanguageDropdown();
    }

    private void styleLanguageDropdown() {
        SwingStyle.styleComboBox(languageDropdown, settings.isDarkMode());
    }

    private void stylePasswordField(JPasswordField field) {
        SwingStyle.styleTextField(field, settings.isDarkMode());
    }

    private JComponent buildButtonsRow() {
        RoundedButton resetBtn = new RoundedButton(" " + LanguageManager.t("settings.reset") + " ");
        resetBtn.setFont(new Font("Dialog", Font.BOLD, 16));
        resetBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    LanguageManager.t("settings.resetConfirm"),
                    LanguageManager.t("settings.resetConfirmTitle"),
                    JOptionPane.YES_NO_OPTION
            );
            if (confirm != JOptionPane.YES_OPTION) return;
            settings.resetToDefaults();
            LanguageManager.setLanguage(settings.getLanguage());
            ThemeManager.apply(window, settings.isDarkMode());
            AppEvents.fireSettingsChanged();
            AppEvents.fireProfileChanged();
            refreshPage();
        });

        RoundedButton logoutBtn = new RoundedButton(" " + LanguageManager.t("settings.logout") + " ");
        logoutBtn.setFont(new Font("Dialog", Font.BOLD, 16));
        logoutBtn.addActionListener(e -> window.logout());

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        btnRow.add(resetBtn);
        btnRow.add(logoutBtn);

        // Wrap in a rounded panel to match grouped UI
        RoundedPanel actionsCard = new RoundedPanel(new BorderLayout());
        actionsCard.setFillColor(settings.isDarkMode() ? new Color(40, 43, 50) : new Color(245, 245, 245));
        actionsCard.setBorder(new EmptyBorder(14, 14, 14, 14));
        actionsCard.setAlignmentX(Component.CENTER_ALIGNMENT);
        actionsCard.setMaximumSize(new Dimension(COLUMN_W, Integer.MAX_VALUE));
        actionsCard.add(btnRow, BorderLayout.CENTER);

        return actionsCard;
    }

    private JComponent buildProfileHeader() {
        resolveDisplayIdentity();
        boolean dark = settings.isDarkMode();
        JPanel header = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        header.setOpaque(false);
        header.setMaximumSize(new Dimension(COLUMN_W, Integer.MAX_VALUE));
        header.setBorder(new EmptyBorder(6, 6, 6, 6));

        // Avatar
        JComponent avatar = buildAvatarPicker(92);

        // Right text (Doctor + email)
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel nameLabel = new JLabel(displayName);
        nameLabel.setFont(new Font("Dialog", Font.BOLD, 34));
        nameLabel.setForeground(dark ? DARK_TEXT : Color.BLACK);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel emailLabel = new JLabel(displayEmail);
        emailLabel.setFont(new Font("Dialog", Font.PLAIN, 18));
        emailLabel.setForeground(dark ? DARK_MUTED : Color.GRAY);
        emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        text.add(nameLabel);
        text.add(Box.createVerticalStrut(6));
        if (displayEmail != null && !displayEmail.isBlank()) text.add(emailLabel);

        header.add(avatar);
        header.add(text);

        return header;
    }

    private void resolveDisplayIdentity() {
        DoctorProfile p = profileService.load();
        if (p == null) p = DoctorProfile.defaults();

        // Name
        String name = Session.getDoctorFullName();
        if (name == null || name.isBlank() || "demo".equalsIgnoreCase(name.trim())) {
            name = p.getFullName();
        }
        if (name == null || name.isBlank()) name = "Doctor";
        displayName = name;

        // Email
        String email = Session.getDoctorEmail();
        if (email == null || email.isBlank() || "demo".equalsIgnoreCase(email.trim())) {
            email = p.getEmail();
        }
        displayEmail = (email == null ? "" : email.trim());
    }

    private String currentAvatarPath() {
        if (meta == null) meta = metaService.loadOrInit();
        return (meta == null || meta.avatarPath == null) ? "" : meta.avatarPath;
    }


    // Avatar Upload
    private JComponent buildAvatarPicker(int size) {
        avatarLabel = new JLabel();
        avatarLabel.setPreferredSize(new Dimension(size, size));
        avatarLabel.setMinimumSize(new Dimension(size, size));
        avatarLabel.setMaximumSize(new Dimension(size, size));
        avatarLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        avatarLabel.setToolTipText(LanguageManager.t("settings.avatarTooltip"));

        updateAvatarIcon(size);

        avatarLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    chooseAndSaveAvatar(size);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    JPopupMenu menu = new JPopupMenu();
                    JMenuItem remove = new JMenuItem(LanguageManager.t("settings.removePhoto"));
                    remove.addActionListener(ev -> {
                        meta.avatarPath = null;
                        metaService.saveAvatarPath(null);
                        updateAvatarIcon(size);
                        AppEvents.fireProfileChanged();
                        refreshPageLater();
                    });
                    menu.add(remove);
                    menu.show(avatarLabel, e.getX(), e.getY());
                }
            }
        });

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(avatarLabel, BorderLayout.CENTER);
        return wrap;
    }

    private void chooseAndSaveAvatar(int size) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(LanguageManager.t("settings.choosePhoto"));
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter(
                LanguageManager.t("settings.imageFilter"), "png", "jpg", "jpeg"
        ));

        int result = chooser.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File src = chooser.getSelectedFile();
        if (src == null || !src.exists()) return;

        try {
            String ext = getFileExtension(src.getName());
            if (ext.isBlank()) ext = "png";

            // Copy to the same account meta directory used by AccountPage
            Path dir = metaService.getDirPath();
            Files.createDirectories(dir);

            String filename = "avatar_" + System.currentTimeMillis() + "." + ext;
            Path dst = dir.resolve(filename);

            Files.copy(src.toPath(), dst, StandardCopyOption.REPLACE_EXISTING);

            meta.avatarPath = dst.toString();
            metaService.saveAvatarPath(meta.avatarPath);

            updateAvatarIcon(size);
            AppEvents.fireProfileChanged();
            refreshPageLater();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    LanguageManager.t("settings.profilePhotoFailed") + "\n" + ex.getMessage(),
                    LanguageManager.t("settings.error"),
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateAvatarIcon(int size) {
        String path = currentAvatarPath();
        File f = (path == null || path.isBlank()) ? null : new File(path);

        try {
            if (f != null && f.exists()) {
                BufferedImage img = ImageIO.read(f);
                if (img != null) {
                    BufferedImage circle = makeCircleAvatar(img, size);
                    avatarLabel.setIcon(new ImageIcon(circle));
                    avatarLabel.setText(null);
                    return;
                }
            }
        } catch (Exception ignored) {}

        // fallback: default initials
        BufferedImage fallback = drawInitialsAvatar(size, displayName);
        avatarLabel.setIcon(new ImageIcon(fallback));
        avatarLabel.setText(null);
    }

    private static String getFileExtension(String name) {
        int i = name.lastIndexOf('.');
        if (i < 0 || i == name.length() - 1) return "";
        return name.substring(i + 1).toLowerCase();
    }

    private static BufferedImage makeCircleAvatar(BufferedImage src, int size) {
        int w = src.getWidth();
        int h = src.getHeight();
        double scale = Math.max((double) size / w, (double) size / h);
        int nw = (int) Math.round(w * scale);
        int nh = (int) Math.round(h * scale);

        BufferedImage scaled = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, nw, nh, null);
        g.dispose();

        int x = (nw - size) / 2;
        int y = (nh - size) / 2;
        BufferedImage cropped = scaled.getSubimage(Math.max(0, x), Math.max(0, y), size, size);

        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setClip(new Ellipse2D.Double(0, 0, size, size));
        g2.drawImage(cropped, 0, 0, null);
        g2.dispose();

        return out;
    }

    private static BufferedImage drawInitialsAvatar(int size, String name) {
        String initials = InitialsAvatar.computeInitials(name);
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color top = new Color(173, 132, 255);
        Color bottom = new Color(255, 205, 102);
        GradientPaint gp = new GradientPaint(0, 0, top, 0, size, bottom);
        g2.setPaint(gp);
        g2.fillOval(0, 0, size, size);

        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Dialog", Font.BOLD, (int) (size * 0.30)));
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth(initials);
        int h = fm.getAscent();
        int x = (size - w) / 2;
        int y = (size + h) / 2 - 4;
        g2.drawString(initials, x, y);

        g2.dispose();
        return out;
    }

    // Group + rows (fixed width to keep alignment)
    private JComponent buildGroupCard(JComponent... rows) {
        RoundedPanel group = new RoundedPanel(new BorderLayout());
        group.setFillColor(settings.isDarkMode() ? new Color(40, 43, 50) : new Color(245, 245, 245));
        group.setBorder(new EmptyBorder(6, 10, 6, 10));

        group.setAlignmentX(Component.CENTER_ALIGNMENT);
        group.setMaximumSize(new Dimension(COLUMN_W, Integer.MAX_VALUE));

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setAlignmentX(Component.CENTER_ALIGNMENT);

        for (int i = 0; i < rows.length; i++) {
            list.add(rows[i]);
            if (i != rows.length - 1) {
                JSeparator sep = new JSeparator();
                sep.setOpaque(false);
                list.add(sep);
            }
        }

        group.add(list, BorderLayout.CENTER);
        return group;
    }

    private JComponent buildToggleRow(String title, JCheckBox toggle) {
        JPanel row = baseRow(title);
        toggle.setPreferredSize(new Dimension(54, 28));
        row.add(toggle, BorderLayout.EAST);
        return row;
    }

    private JComponent buildDropdownRow(String title, JComboBox<String> dropdown) {
        JPanel row = baseRow(title);
        dropdown.setPreferredSize(new Dimension(160, 34));
        row.add(dropdown, BorderLayout.EAST);
        return row;
    }

    private JPanel baseRow(String title) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(14, 10, 14, 10));


        row.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel t = new JLabel(title);
        t.setFont(new Font("Dialog", Font.PLAIN, 20));
        t.setForeground(settings.isDarkMode() ? DARK_TEXT : Color.BLACK);
        row.add(t, BorderLayout.WEST);

        return row;
    }

    // Theme
    private void applyLocalTheme() {
        setBackground(settings.isDarkMode() ? DARK_BG : Color.WHITE);
    }

    private void applyLocalTheme(RoundedPanel card, JLabel title) {
        boolean dark = settings.isDarkMode();

        setBackground(dark ? DARK_BG : Color.WHITE);

        try {
            card.setFillColor(dark ? DARK_CARD : Color.WHITE);
        } catch (Exception ignored) {}

        title.setForeground(dark ? DARK_TEXT : Color.BLACK);

        if (darkModeToggle != null) {
            darkModeToggle.setForeground(dark ? DARK_TEXT : Color.BLACK);
        }

        if (languageDropdown != null) {
            styleLanguageDropdown();
        }

        if (deletePasswordField != null) {
            stylePasswordField(deletePasswordField);
        }

        updateAllLabels(this, dark ? DARK_TEXT : Color.BLACK, dark ? DARK_MUTED : Color.GRAY);
    }

    private void updateAllLabels(Container root, Color text, Color muted) {
        for (Component c : root.getComponents()) {
            if (c instanceof JLabel lbl) {
                if (lbl.getFont() != null && lbl.getFont().getSize() <= 18) lbl.setForeground(muted);
                else lbl.setForeground(text);
            } else if (c instanceof Container child) {
                updateAllLabels(child, text, muted);
            }
        }
    }

    private void refreshPage() {
        // Re-resolve identity/avatar every refresh so this page stays in sync with AccountPage
        meta = metaService.loadOrInit();
        resolveDisplayIdentity();
        removeAll();
        add(wrapScrollable(buildMainCard()), BorderLayout.CENTER);
        revalidate();
        repaint();
        applyLocalTheme();
    }

    private JScrollPane wrapScrollable(JComponent content) {
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(settings.isDarkMode() ? DARK_BG : Color.WHITE);
        return scroll;
    }

    private void refreshPageLater() {
        SwingUtilities.invokeLater(this::refreshPage);
    }

    @Override
    public void onShown() {
        refreshPageLater();
    }

    @Override
    public void disposePage() {
        AppEvents.removeProfileChangedListener(profileChangedListener);
    }

    @Override
    public void setVisible(boolean aFlag) {
        super.setVisible(aFlag);
        if (!aFlag) return;

        // When this page is shown, re-sync identity/avatar with current Session
        if (refreshing) return;
        refreshing = true;
        SwingUtilities.invokeLater(() -> {
            try {
                refreshPage();
            } finally {
                refreshing = false;
            }
        });
    }

    // Delete Account Card
    private JComponent buildDeleteAccountCard() {
        // Password field for re-auth
        deletePasswordField = new JPasswordField();
        deletePasswordField.setFont(new Font("Dialog", Font.PLAIN, 16));
        deletePasswordField.setPreferredSize(new Dimension(260, 34));
        deletePasswordField.setMaximumSize(new Dimension(260, 34));
        stylePasswordField(deletePasswordField);

        JLabel title = new JLabel(LanguageManager.t("settings.dangerTitle"));
        title.setFont(new Font("Dialog", Font.BOLD, 18));
        title.setForeground(settings.isDarkMode() ? DARK_TEXT : Color.BLACK);

        JLabel hint = new JLabel(LanguageManager.t("settings.dangerHint"));
        hint.setFont(new Font("Dialog", Font.PLAIN, 14));
        hint.setForeground(settings.isDarkMode() ? DARK_MUTED : Color.GRAY);

        JPanel passwordRow = new JPanel(new BorderLayout(12, 0));
        passwordRow.setOpaque(false);
        JLabel pwLabel = new JLabel(LanguageManager.t("settings.password"));
        pwLabel.setFont(new Font("Dialog", Font.PLAIN, 16));
        pwLabel.setForeground(settings.isDarkMode() ? DARK_TEXT : Color.BLACK);
        passwordRow.add(pwLabel, BorderLayout.WEST);
        passwordRow.add(deletePasswordField, BorderLayout.EAST);

        deleteAccountButton = new RoundedButton(" " + LanguageManager.t("settings.delete") + " ");
        deleteAccountButton.setFont(new Font("Dialog", Font.BOLD, 16));
        deleteAccountButton.setColors(
                new Color(190, 55, 55),
                new Color(220, 70, 70),
                new Color(150, 40, 40),
                Color.WHITE
        );

        deleteAccountButton.addActionListener(e -> {
            String email = Session.getDoctorEmail();
            String pw = new String(deletePasswordField.getPassword());

            if (email == null || email.isBlank() || "demo".equalsIgnoreCase(email.trim())) {
                JOptionPane.showMessageDialog(this,
                        LanguageManager.t("settings.noAccount"),
                        LanguageManager.t("settings.delete"),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (pw == null || pw.isBlank()) {
                JOptionPane.showMessageDialog(this,
                        LanguageManager.t("settings.enterPassword"),
                        LanguageManager.t("settings.delete"),
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Strong confirmation: user must type DELETE
            String typed = JOptionPane.showInputDialog(this,
                    LanguageManager.t("settings.confirmDeletePrompt"),
                    LanguageManager.t("settings.confirmDeleteTitle"),
                    JOptionPane.WARNING_MESSAGE);

            if (typed == null) return; // cancelled
            if (!"DELETE".equals(typed.trim())) {
                JOptionPane.showMessageDialog(this,
                        LanguageManager.t("settings.confirmMismatch"),
                        LanguageManager.t("settings.delete"),
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            deleteAccountAsync(email, pw);
        });

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        passwordRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        deleteAccountButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        center.add(title);
        center.add(Box.createVerticalStrut(6));
        center.add(hint);
        center.add(Box.createVerticalStrut(12));
        center.add(passwordRow);
        center.add(Box.createVerticalStrut(14));
        center.add(deleteAccountButton);

        RoundedPanel dangerCard = new RoundedPanel(new BorderLayout());
        dangerCard.setFillColor(settings.isDarkMode() ? new Color(52, 38, 40) : new Color(255, 245, 245));
        dangerCard.setBorder(new EmptyBorder(14, 14, 14, 14));
        dangerCard.setAlignmentX(Component.CENTER_ALIGNMENT);
        dangerCard.setMaximumSize(new Dimension(COLUMN_W, Integer.MAX_VALUE));
        dangerCard.add(center, BorderLayout.CENTER);

        return dangerCard;
    }

    private void deleteAccountAsync(String email, String password) {
        deleteAccountButton.setEnabled(false);
        deleteAccountButton.setText(" " + LanguageManager.t("settings.deleting") + " ");

        SwingWorker<ApiClient.ApiResult<ApiClient.SimpleResponse>, Void> worker = new SwingWorker<>() {
            @Override
            protected ApiClient.ApiResult<ApiClient.SimpleResponse> doInBackground() {
                return ApiClient.deleteAccountResult(email, password);
            }

            @Override
            protected void done() {
                deleteAccountButton.setEnabled(true);
                deleteAccountButton.setText(" " + LanguageManager.t("settings.delete") + " ");

                try {
                    ApiClient.ApiResult<ApiClient.SimpleResponse> result = get();
                    if (result == null) {
                        JOptionPane.showMessageDialog(SettingsPage.this,
                                LanguageManager.t("settings.noResponse"),
                                LanguageManager.t("settings.delete"),
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    ApiClient.SimpleResponse res = result.data;
                    if (!result.isSuccess()) {
                        String msg = (result.message == null || result.message.isBlank())
                                ? LanguageManager.t("settings.deleteFailed")
                                : result.message;
                        JOptionPane.showMessageDialog(SettingsPage.this,
                                msg,
                                LanguageManager.t("settings.delete"),
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    if (res.status != null && res.status.equalsIgnoreCase("ok")) {
                        Session.clear();
                        JOptionPane.showMessageDialog(SettingsPage.this,
                                LanguageManager.t("settings.accountDeleted"),
                                LanguageManager.t("settings.delete"),
                                JOptionPane.INFORMATION_MESSAGE);
                        window.logout();
                    } else {
                        String msg = (res.message == null || res.message.isBlank()) ? LanguageManager.t("settings.deleteFailed") : res.message;
                        JOptionPane.showMessageDialog(SettingsPage.this,
                                msg,
                                LanguageManager.t("settings.delete"),
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(SettingsPage.this,
                            LanguageManager.t("settings.deleteFailedPrefix") + ex.getMessage(),
                            LanguageManager.t("settings.delete"),
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    // Avatar (initials) fallback component (kept for compatibility)
    private static class InitialsAvatar extends JPanel {
        private final int size;
        private final String initials;

        InitialsAvatar(int size, String name) {
            this.size = size;
            this.initials = computeInitials(name);
            setOpaque(false);
            setPreferredSize(new Dimension(size, size));
            setMinimumSize(new Dimension(size, size));
            setMaximumSize(new Dimension(size, size));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color top = new Color(173, 132, 255);
            Color bottom = new Color(255, 205, 102);
            GradientPaint gp = new GradientPaint(0, 0, top, 0, size, bottom);
            g2.setPaint(gp);
            g2.fillOval(0, 0, size, size);

            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Dialog", Font.BOLD, (int) (size * 0.30)));
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(initials);
            int h = fm.getAscent();
            int x = (size - w) / 2;
            int y = (size + h) / 2 - 4;
            g2.drawString(initials, x, y);

            g2.dispose();
        }

        // changed to public so SettingsPage can reuse it for fallback image rendering
        public static String computeInitials(String name) {
            if (name == null || name.isBlank()) return "D";
            String[] parts = name.trim().split("\\s+");
            if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
            String a = parts[0].substring(0, 1).toUpperCase();
            String b = parts[parts.length - 1].substring(0, 1).toUpperCase();
            return a + b;
        }
    }
}
