package UI.Components;

import UI.MainWindow;
import Utilities.ImageLoader;
import Utilities.LanguageManager;

import javax.swing.*;
import java.awt.*;

public class TopBar extends JPanel {

    private static final Color BAR_COLOR = new Color(63, 90, 227);

    private JLabel nameLabel;
    private JLabel roleLabel;
    private JLabel avatarLabel;
    private JLabel title;

    private final MainWindow window;

    // Creates the top navigation bar containing the app title and doctor account information.
    public TopBar(MainWindow window) {
        this.window = window;

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(0, 90));
        setBackground(BAR_COLOR);

        add(buildLeftSection(), BorderLayout.WEST);
        add(buildRightSection(), BorderLayout.EAST);
    }

    // Ensures all text within the top bar remains white when the component is added to the UI.
    @Override
    public void addNotify() {
        super.addNotify();
        forceWhiteText(this);
    }

    // Recursively forces all JLabel components within the container to render white text.
    private void forceWhiteText(Container c) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JLabel lbl) {
                lbl.setForeground(Color.WHITE);
            }
            if (comp instanceof Container child) {
                forceWhiteText(child);
            }
        }
    }

    // Builds the left section containing the application logo and title
    private JPanel buildLeftSection() {
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
        left.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JLabel logo = new JLabel(
                ImageLoader.loadImageScaled("icon_logo2", "Icons", 80)
        );

        title = new JLabel(LanguageManager.t("app.title"));
        title.setFont(new Font("Arial", Font.BOLD, 35));
        title.setForeground(Color.WHITE);

        left.add(logo);
        left.add(title);

        return left;
    }

    // Builds the right section containing notifications, doctor information, and avatar.
    private JPanel buildRightSection() {
        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));
        right.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 30));

        // Notification button
        JLabel notificationBtn = new JLabel(
                ImageLoader.loadImageScaled("icon_notification", "Icons", 24)
        );
        notificationBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        notificationBtn.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 18));

        notificationBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                JOptionPane.showMessageDialog(
                        TopBar.this,
                        "Active and recent alerts are shown in the alert panel on Home.",
                        "Notifications",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        });

        JPanel textBlock = new JPanel();
        textBlock.setOpaque(false);
        textBlock.setLayout(new BoxLayout(textBlock, BoxLayout.Y_AXIS));

        nameLabel = new JLabel("Dr. Sarah Johnson");
        nameLabel.setFont(new Font("Arial", Font.BOLD, 16));
        nameLabel.setForeground(Color.WHITE);

        roleLabel = new JLabel("");
        roleLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        roleLabel.setForeground(new Color(220, 225, 255));

        textBlock.add(nameLabel);
        textBlock.add(Box.createVerticalStrut(4));
        textBlock.add(roleLabel);

        // ---- Avatar ----
        avatarLabel = new JLabel("SJ", SwingConstants.CENTER);
        avatarLabel.setPreferredSize(new Dimension(80, 80));
        avatarLabel.setMaximumSize(new Dimension(80, 80));
        avatarLabel.setFont(new Font("Arial", Font.BOLD, 18));
        avatarLabel.setForeground(Color.WHITE);
        avatarLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        avatarLabel.setOpaque(false);

        avatarLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                window.showPage(MainWindow.PAGE_ACCOUNT);
            }
        });

        JPanel avatarCircle = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(33, 150, 243));
                g.fillOval(0, 0, getWidth(), getHeight());
            }
        };
        avatarCircle.setPreferredSize(new Dimension(42, 42));
        avatarCircle.setMaximumSize(new Dimension(42, 42));
        avatarCircle.setOpaque(false);
        avatarCircle.add(avatarLabel);

        right.add(notificationBtn);
        right.add(textBlock);
        right.add(Box.createHorizontalStrut(16));
        right.add(avatarCircle);

        return right;
    }

    // Updates the displayed doctor name, role, and avatar initials.
    public void updateDoctorInfo(String fullName, String role) {
        nameLabel.setText(fullName == null ? "" : fullName);
        roleLabel.setText(role == null ? "" : role);

        String initials = extractInitials(fullName == null ? "" : fullName);
        avatarLabel.setText(initials);
    }

    public void applyLanguage() {
        title.setText(LanguageManager.t("app.title"));
        revalidate();
        repaint();
    }

    // Extracts uppercase initials from a full name string for avatar display.
    private String extractInitials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 0) return "";
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
