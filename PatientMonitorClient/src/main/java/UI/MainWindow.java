package UI;

import NetWork.Session;
import Services.LocalAppStateService;
import Utilities.LanguageManager;
import Utilities.SettingManager;
import Utilities.ThemeManager;

import Models.Patients.Patient;
import UI.Components.SideBar;
import UI.Components.TopBar;
import UI.Pages.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;

public class MainWindow extends JFrame {
    private static final Dimension MINIMUM_WINDOW_SIZE = new Dimension(980, 680);
    private final LocalAppStateService localStateService = new LocalAppStateService();

    private CardLayout cardLayout;
    private JPanel pageContainer;
    private TopBar topBar;
    private SideBar sidebar;

    public static final String PAGE_HOME = "home";
    public static final String PAGE_LOGIN = "login";
    public static final String PAGE_REGISTER = "register";
    public static final String PAGE_ADD = "add";
    public static final String PAGE_DIGITALTWIN = "digitalTwin";
    public static final String PAGE_ACCOUNT = "account";
    public static final String PAGE_SETTINGS = "settings";
    public static final String PAGE_LIVE = "live";

    private HomePage homePage;
    private LoginPage loginPage;
    private RegisterPage registerPage;
    private DigitalTwinPage digitalTwinPage;
    private SettingsPage settingsPage;
    private AccountPage accountPage;
    private AddPatientPage addPatientPage;
    private Component liveMonitoringPage;
    private String currentPageName = PAGE_LOGIN;

    // Creates the main application window and initialises all pages, navigation, and theme settings.
    public MainWindow() {
        setTitle(LanguageManager.t("app.title"));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        LocalAppStateService.State localState = localStateService.load();
        Dimension restoredSize = new Dimension(
                Math.max(MINIMUM_WINDOW_SIZE.width, localState.window.width),
                Math.max(MINIMUM_WINDOW_SIZE.height, localState.window.height)
        );
        setPreferredSize(restoredSize);
        setMinimumSize(MINIMUM_WINDOW_SIZE);
        setLayout(new BorderLayout());

        // Top bar & sidebar
        topBar = new TopBar(this);
        sidebar = new SideBar(this);
        add(topBar, BorderLayout.NORTH);
        add(sidebar, BorderLayout.WEST);
        topBar.setVisible(true);
        sidebar.setVisible(true);

        // Card layout
        cardLayout = new CardLayout();
        pageContainer = new JPanel(cardLayout);

        buildPages();

        add(pageContainer, BorderLayout.CENTER);
        showPage(PAGE_LOGIN);
        //cardLayout.show(pageContainer, PAGE_HOME);

        // Theme
        SettingManager settings = new SettingManager();
        ThemeManager.apply(this, settings.isDarkMode());

        refreshTopBarDoctorInfo();

        pack();
        setLocationRelativeTo(null);
        if (localState.window.maximized) {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveWindowState();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                saveWindowState();
            }
        });
        setVisible(true);
    }

    // Navigates the UI to the login page.
    public void showLoginPage() {
        showPage(PAGE_LOGIN);
    }

    // Navigates the UI to the register page
    public void showRegisterPage() {
        showPage(PAGE_REGISTER);
    }

    // Navigates to the home page and enables alert behaviour for live monitoring
    public void showHomePage() {
        Services.AlertManager.getInstance().enableAlerts();
        showPage(PAGE_HOME);
    }

    public void showDigitalTwin(Patient patient) {
        sidebar.clearSelection();
        digitalTwinPage.setPatient(patient);
        showPage(PAGE_DIGITALTWIN);
    }

    // Navigates to the live monitoring page and swaps in a page instance for the chosen patient.
    public void showLiveMonitoring(Patient patient) {
        sidebar.clearSelection();

        pageContainer.remove(liveMonitoringPage);
        liveMonitoringPage = new LiveMonitoringPage(patient, this);
        pageContainer.add(liveMonitoringPage, PAGE_LIVE);

        showPage(PAGE_LIVE);
    }

    // Navigates to the add patient page.
    public void showAddPatientPage() {
        sidebar.setSelectedPage(PAGE_ADD);
        showPage(PAGE_ADD);
    }

    // Shows a named page and toggles top-level UI components based on authentication state.
    public void showPage(String pageName) {
        String previousPageName = currentPageName;
        boolean pageChanged = !Objects.equals(previousPageName, pageName);
        if (pageChanged) {
            notifyHidden(componentForPage(previousPageName));
        }

        currentPageName = pageName;
        boolean isAuthPage =
                pageName.equals(PAGE_LOGIN) || pageName.equals(PAGE_REGISTER);

        sidebar.setVisible(!isAuthPage);
        topBar.setVisible(!isAuthPage);

        // Keep sidebar highlight in sync even when navigation comes from TopBar or code
        if (!isAuthPage && sidebar != null) {
            switch (pageName) {
                case PAGE_HOME, PAGE_ADD, PAGE_ACCOUNT, PAGE_SETTINGS -> sidebar.setSelectedPage(pageName);
                case PAGE_DIGITALTWIN, PAGE_LIVE -> sidebar.clearSelection();
                default -> {}
            }
        }

        if (pageName.equals(PAGE_LOGIN)) {
            loginPage.clearFields();
        }

        cardLayout.show(pageContainer, pageName);
        notifyShown(componentForPage(pageName));
        revalidate();
        repaint();
    }

    // Logs out the current user by returning to the login page.
    public void logout() {
        Services.AlertManager.getInstance().disableAlerts();
        Models.Vitals.LiveVitals.clearAllShared();
        Session.clear();
        showPage(PAGE_LOGIN);
    }

    // Updates the UI after a successful login, including top bar doctor identity display.
    public void onDoctorLoggedIn() {
        // Ensure UI switches out of auth pages
        showHomePage();

        // Refresh top bar doctor info using the logged-in identity
        refreshTopBarDoctorInfo();

        if (sidebar != null) {
            sidebar.setVisible(true);
        }
        if (topBar != null) {
            topBar.setVisible(true);
        }
    }

    // Refreshes the top bar doctor details using the current session information
    private void refreshTopBarDoctorInfo() {
        if (topBar == null) return;

        String email = Session.getDoctorEmail();
        if (email != null && !email.isBlank() && !"demo".equalsIgnoreCase(email.trim())) {
            topBar.updateDoctorInfo(Session.getDoctorFullName(), Session.getDoctorRole());
        } else {
            topBar.updateDoctorInfo("Demo", "");
        }
    }
    // Returns the account page instance for external components that need to refresh profile state
    public AccountPage getAccountPage() {
        return accountPage;
    }
    // Returns the top bar component for pages that need to update doctor display information
    public TopBar getTopBar() {return topBar;}

    public void applyLanguage() {
        if (topBar != null) topBar.applyLanguage();
        if (sidebar != null) sidebar.applyLanguage();
        setTitle(LanguageManager.t("app.title"));
        revalidate();
        repaint();
    }

    public void rebuildPagesForLanguage(String pageName) {
        currentPageName = pageName == null ? currentPageName : pageName;
        buildPages();
        applyLanguage();
        showPage(currentPageName);
    }

    private void buildPages() {
        if (pageContainer == null) return;
        disposePages();
        pageContainer.removeAll();

        loginPage = new LoginPage(this);
        registerPage = new RegisterPage(this);
        homePage = new HomePage(this);
        digitalTwinPage = new DigitalTwinPage(this);
        settingsPage = new SettingsPage(this);
        accountPage = new AccountPage(this);
        addPatientPage = new AddPatientPage(this);
        liveMonitoringPage = new JPanel(new BorderLayout());

        pageContainer.add(loginPage, PAGE_LOGIN);
        pageContainer.add(registerPage, PAGE_REGISTER);
        pageContainer.add(homePage, PAGE_HOME);
        pageContainer.add(digitalTwinPage, PAGE_DIGITALTWIN);
        pageContainer.add(settingsPage, PAGE_SETTINGS);
        pageContainer.add(accountPage, PAGE_ACCOUNT);
        pageContainer.add(addPatientPage, PAGE_ADD);
        pageContainer.add(liveMonitoringPage, PAGE_LIVE);
    }

    private Component componentForPage(String pageName) {
        if (pageName == null) return null;
        return switch (pageName) {
            case PAGE_HOME -> homePage;
            case PAGE_LOGIN -> loginPage;
            case PAGE_REGISTER -> registerPage;
            case PAGE_ADD -> addPatientPage;
            case PAGE_DIGITALTWIN -> digitalTwinPage;
            case PAGE_ACCOUNT -> accountPage;
            case PAGE_SETTINGS -> settingsPage;
            case PAGE_LIVE -> liveMonitoringPage;
            default -> null;
        };
    }

    private void notifyShown(Component component) {
        if (component instanceof PageLifecycle lifecycle) {
            lifecycle.onShown();
        }
    }

    private void notifyHidden(Component component) {
        if (component instanceof PageLifecycle lifecycle) {
            lifecycle.onHidden();
        }
    }

    private void saveWindowState() {
        disposePages();
        LocalAppStateService.State state = localStateService.load();
        state.window.maximized = (getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH;
        if (!state.window.maximized) {
            Dimension size = getSize();
            state.window.width = Math.max(MINIMUM_WINDOW_SIZE.width, size.width);
            state.window.height = Math.max(MINIMUM_WINDOW_SIZE.height, size.height);
        }
        localStateService.save(state);
    }

    private void disposePages() {
        if (pageContainer == null) return;
        for (Component component : pageContainer.getComponents()) {
            if (component instanceof PageLifecycle lifecycle) {
                lifecycle.disposePage();
            }
        }
    }
}
