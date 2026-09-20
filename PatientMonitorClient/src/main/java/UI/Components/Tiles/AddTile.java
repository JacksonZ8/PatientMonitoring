package UI.Components.Tiles;

import UI.MainWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


public class AddTile extends BaseTile {

    // Creates a clickable tile that navigates to the add-patient page.
    public AddTile(MainWindow window) {
        super(370, 320, 30, true);

        JLabel plus = new JLabel("+");
        plus.setFont(new Font("Arial", Font.BOLD, 50));
        plus.setAlignmentX(Component.CENTER_ALIGNMENT);

        add(Box.createVerticalGlue());
        add(plus);
        add(Box.createVerticalGlue());

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                window.showAddPatientPage();
            }
        });

    }
}
