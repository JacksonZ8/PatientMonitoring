package UI.Components;

import UI.Components.Tiles.BaseTile;
import Utilities.ImageLoader;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.function.Consumer;

public class SearchBar extends BaseTile {
    private JTextField searchField;

    // Creates a search bar tile with an icon and text input field.
    public SearchBar() {
        super(400, 65, 40, true);
        setLayout(new BorderLayout());

        // search field
        searchField = new JTextField();
        searchField.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        searchField.setOpaque(false);

        // icon
        JLabel searchLabel = new JLabel(ImageLoader.loadImage("icon_search", "Icons", 25));
        add(searchLabel, BorderLayout.WEST);

        add(searchField, BorderLayout.CENTER);
        setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 30)); // make sure text inside teh border
    }

    // Registers a listener that receives live search text updates as the user types.
    public void addSearchListener(Consumer<String> listener) {
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                listener.accept(searchField.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                listener.accept(searchField.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                listener.accept(searchField.getText());
            }
        });
    }
}