package Utilities;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class ImageLoader {

    // load image and force it into a square (for square icon)
    public static ImageIcon loadImage(String imageName, String folderName, int size) {
        try {
            URL url = ImageLoader.class.getResource("/" + folderName +"/" + imageName + ".png");
            if (url == null) {
                System.out.println("icon not found: /" + folderName + "/" + imageName + ".png");
                return null;
            }

            Image img = new ImageIcon(url).getImage();
            Image scaled = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // load image and resize it but keep the aspect ratio
    public static ImageIcon loadImageScaled(String imageName, String folderName, int scaledWidth) {
        try {
            URL url = ImageLoader.class.getResource("/" + folderName + "/" + imageName + ".png");
            if (url == null) {
                System.out.println("icon not found: /" + folderName + "/" + imageName + ".png");
                return null;
            }

            Image img = new ImageIcon(url).getImage();
            int originalW = img.getWidth(null);
            int originalH = img.getHeight(null);

            if (originalW <= 0 || originalH <= 0) {
                System.out.println("Invalid image dimensions for " + imageName);
                return null;
            }

            // compute proportional height
            double scale = (double) scaledWidth / originalW;
            int newH = (int) (originalH * scale);

            Image scaled = img.getScaledInstance(scaledWidth, newH, Image.SCALE_SMOOTH);
            return new ImageIcon(scaled);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
