package UI.Pages;

import NetWork.ApiClient;
import UI.Components.ImagePanel;
import UI.Components.PlaceHolders.PlaceholderPasswordField;
import UI.Components.PlaceHolders.PlaceholderTextField;
import UI.Components.Tiles.BaseTile;
import UI.MainWindow;
import Utilities.ImageLoader;
import Utilities.LanguageManager;

import javax.swing.*;
import java.awt.*;

public class RegisterPage extends JPanel {

    public RegisterPage(MainWindow mainWindow) {

        setLayout(new GridLayout(1, 2));

        // left blue
        // load image
        Image bgImage = ImageLoader.loadImage("bg_left", "UI", 1000).getImage();

        // use ImagePanel instead of plain JPanel
        ImagePanel leftPanel = new ImagePanel(bgImage);
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

        JLabel titleLeft = new JLabel(LanguageManager.t("register.leftTitle"), SwingConstants.CENTER);
        titleLeft.setFont(new Font("Arial", Font.BOLD, 50));
        titleLeft.setForeground(Color.WHITE);
        titleLeft.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLeft = new JLabel(LanguageManager.t("register.haveAccount"));
        subtitleLeft.setFont(new Font("Arial", Font.PLAIN, 16));
        subtitleLeft.setForeground(Color.WHITE);
        subtitleLeft.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitleLeft.setBorder(BorderFactory.createEmptyBorder(15, 0, 10, 0));

        BaseTile loginTile = new BaseTile(160, 65, 40, true);
        loginTile.setMaximumSize(new Dimension(200, 60));
        loginTile.setBackground(Color.WHITE);
        loginTile.setLayout(new BorderLayout());

        JButton loginBtn = new JButton(LanguageManager.t("register.login"));
        loginBtn.setFont(new Font("Arial", Font.BOLD, 16));
        loginBtn.setForeground(new Color(65, 88, 208));
        loginBtn.setContentAreaFilled(false);
        loginBtn.setBorderPainted(false);
        loginBtn.setFocusPainted(false);
        loginTile.add(loginBtn, BorderLayout.CENTER);

        loginBtn.addActionListener(e -> mainWindow.showLoginPage());

        leftPanel.add(Box.createVerticalGlue());
        leftPanel.add(titleLeft);
        leftPanel.add(subtitleLeft);
        leftPanel.add(Box.createVerticalStrut(20));
        leftPanel.add(loginTile);
        leftPanel.add(Box.createVerticalGlue());

        // right
        JPanel rightPanel = new JPanel();
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(120, 120, 100, 120));

        JLabel title = new JLabel(LanguageManager.t("register.title"));
        title.setFont(new Font("Arial", Font.BOLD, 28));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.setBorder(BorderFactory.createEmptyBorder(0, 150, 0, 150));

        // email
        JLabel emailLabel = label(LanguageManager.t("register.email"));
        emailLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        BaseTile emailTile = inputTile();
        PlaceholderTextField emailField = new PlaceholderTextField(LanguageManager.t("register.emailPlaceholder"));
        styleTextField(emailField);
        emailTile.add(emailField);

        // password
        JLabel passwordLabel = label(LanguageManager.t("register.password"));
        passwordLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        BaseTile passwordTile = inputTile();
        passwordTile.setLayout(new BorderLayout());
        // password field
        PlaceholderPasswordField passwordField = new PlaceholderPasswordField(LanguageManager.t("register.passwordPlaceholder"));
        styleTextField(passwordField);
        passwordTile.add(passwordField, BorderLayout.CENTER);
        // visibility button
        JButton toggleBtn = new JButton();
        toggleBtn.setPreferredSize(new Dimension(50, 50));
        toggleBtn.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 30));
        toggleBtn.setFocusPainted(false);
        toggleBtn.setContentAreaFilled(false);
        toggleBtn.setBorderPainted(false);
        // load icons
        ImageIcon showIcon = ImageLoader.loadImage("icon_visible", "Icons", 24);
        ImageIcon hideIcon = ImageLoader.loadImage("icon_invisible", "Icons", 24);
        // initial icon is "hidden"
        toggleBtn.setIcon(hideIcon);
        // toggle logic
        toggleBtn.addActionListener(ev -> {
            if (passwordField.getEchoChar() == 0) {
                // currently visible, hide it
                passwordField.setEchoChar('*');
                toggleBtn.setIcon(hideIcon);
            } else {
                // currently hidden, show it
                passwordField.setEchoChar((char) 0);
                toggleBtn.setIcon(showIcon);
            }
        });
        passwordTile.add(toggleBtn, BorderLayout.EAST);


        // given name
        JLabel givenLabel = label(LanguageManager.t("register.givenName"));
        givenLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        BaseTile givenTile = inputTile();
        PlaceholderTextField givenField = new PlaceholderTextField(LanguageManager.t("register.givenPlaceholder"));
        styleTextField(givenField);
        givenTile.add(givenField);

        // family name
        JLabel familyLabel = label(LanguageManager.t("register.familyName"));
        familyLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        BaseTile familyTile = inputTile();
        PlaceholderTextField familyField = new PlaceholderTextField(LanguageManager.t("register.familyPlaceholder"));
        styleTextField(familyField);
        familyTile.add(familyField);

        // check box
        JCheckBox termsCheck = new JCheckBox(LanguageManager.t("register.terms"));
        termsCheck.setFont(new Font("Arial", Font.PLAIN, 14));
        termsCheck.setBackground(Color.WHITE);
        termsCheck.setAlignmentX(Component.LEFT_ALIGNMENT);

        // sign up button
        BaseTile signUpTile = new BaseTile(650, 60, 50, true);
        signUpTile.setMaximumSize(new Dimension(650, 60));
        signUpTile.setBackground(new Color(68, 104, 140));
        signUpTile.setLayout(new BorderLayout());
        JButton signUpBtn = new JButton(LanguageManager.t("register.signUp"));
        signUpBtn.setFont(new Font("Arial", Font.BOLD, 14));
        signUpBtn.setForeground(Color.WHITE);
        signUpBtn.setContentAreaFilled(false);
        signUpBtn.setBorderPainted(false);
        signUpBtn.setFocusPainted(false);
        signUpTile.add(signUpBtn, BorderLayout.CENTER);
        signUpTile.setAlignmentX(Component.LEFT_ALIGNMENT);

        // sign up
        signUpBtn.addActionListener(e -> {

            if (!termsCheck.isSelected()) {
                JOptionPane.showMessageDialog(this,
                        LanguageManager.t("register.acceptTerms"),
                        LanguageManager.t("register.warning"),
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            String email = emailField.getText().trim();
            String givenName = givenField.getText().trim();
            String familyName = familyField.getText().trim();

            if (!isValidEmail(email)) {
                JOptionPane.showMessageDialog(this,
                        LanguageManager.t("register.invalidEmail"),
                        LanguageManager.t("register.invalidEmailTitle"),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (givenName.isBlank() || familyName.isBlank()) {
                JOptionPane.showMessageDialog(this,
                        LanguageManager.t("register.missingName"),
                        LanguageManager.t("register.missingNameTitle"),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }


            String password = new String(passwordField.getPassword()).trim();
            // password length check
            if (password.length() < 10) {
                JOptionPane.showMessageDialog(this,
                        LanguageManager.t("register.invalidPassword"),
                        LanguageManager.t("register.invalidPasswordTitle"),
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            signUpBtn.setEnabled(false);
            SwingWorker<ApiClient.ApiResult<ApiClient.SimpleResponse>, Void> worker = new SwingWorker<>() {
                @Override
                protected ApiClient.ApiResult<ApiClient.SimpleResponse> doInBackground() {
                    return ApiClient.registerResult(
                            email,
                            password,
                            givenName,
                            familyName
                    );
                }

                @Override
                protected void done() {
                    signUpBtn.setEnabled(true);
                    try {
                        ApiClient.ApiResult<ApiClient.SimpleResponse> result = get();
                        if (!result.isSuccess()) {
                            JOptionPane.showMessageDialog(RegisterPage.this,
                                    result.message,
                                    LanguageManager.t("register.failedTitle"),
                                    JOptionPane.ERROR_MESSAGE);
                            return;
                        }

                        JOptionPane.showMessageDialog(RegisterPage.this,
                                LanguageManager.t("register.successMessage"),
                                LanguageManager.t("register.successTitle"),
                                JOptionPane.INFORMATION_MESSAGE);

                        mainWindow.showLoginPage();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(RegisterPage.this,
                                LanguageManager.t("register.failedPrefix") + ex.getMessage(),
                                LanguageManager.t("register.failedTitle"),
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            worker.execute();
        });


        // add to right panel
        rightPanel.add(title);
        rightPanel.add(Box.createVerticalStrut(20));

        rightPanel.add(emailLabel);
        rightPanel.add(emailTile);
        rightPanel.add(Box.createVerticalStrut(15));

        rightPanel.add(passwordLabel);
        rightPanel.add(passwordTile);
        rightPanel.add(Box.createVerticalStrut(15));

        rightPanel.add(givenLabel);
        rightPanel.add(givenTile);
        rightPanel.add(Box.createVerticalStrut(15));

        rightPanel.add(familyLabel);
        rightPanel.add(familyTile);
        rightPanel.add(Box.createVerticalStrut(20));

        rightPanel.add(termsCheck);
        rightPanel.add(Box.createVerticalStrut(20));

        rightPanel.add(signUpTile);

        add(leftPanel);
        add(rightPanel);
    }

    // check if email is valid
    private boolean isValidEmail(String email) {
        if (email == null) return false;

        email = email.trim();

        // @ + email with at least 5 characters
        String regex = "^[A-Za-z0-9+_.-]+@.{5,}$";

        return email.matches(regex);
    }


    // create label
    private JLabel label(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Arial", Font.PLAIN, 16));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    // input fields
    private BaseTile inputTile() {
        BaseTile tile = new BaseTile(650, 60, 50, false);
        tile.setMaximumSize(new Dimension(650, 60));
        tile.setLayout(new BorderLayout());
        tile.setAlignmentX(Component.LEFT_ALIGNMENT);
        return tile;
    }

    // text field
    private void styleTextField(JTextField field) {
        field.setFont(new Font("Arial", Font.PLAIN, 16));
        field.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 15));
        field.setOpaque(false);
    }
}
