package UI.Components;

import Models.Patients.Patient;
import UI.Pages.HomePage;
import Utilities.ImageLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// Button that allows a patient tile to be marked as sticky (pinned to the top).
public class StickyButton extends JButton {

    private boolean selected = false;
    private final ImageIcon normalImage;
    private final ImageIcon selectedImage;

    // Creates a sticky toggle button linked to a patient and refreshes the home page on change.
    public StickyButton(Patient patient, HomePage homePage) {
        // set image
        normalImage = ImageLoader.loadImage("stickybutton", "UI", 35);
        selectedImage = ImageLoader.loadImage("stickybutton_highlight", "UI", 35);
        selected = patient.isSticky();

        // appearance cleanup
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // set initial icon after configuring button
        setIcon(selected ? selectedImage : normalImage);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selected = !selected;
                patient.setSticky(selected);
                setIcon(selected ? selectedImage : normalImage);

                homePage.refresh();
            }
        });
    }
    // Returns whether the sticky button is currently selected.
    public boolean isSelected() {
        return selected;
    }

}
