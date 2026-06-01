package de.mmbbs.kassensystem.util;

import javafx.scene.image.Image;

import java.io.File;

public class ImageUtil {
    private static final String FALLBACK_IMAGE_PATH = "/empty-foto.png";

    public static Image loadProductImage(String imagePath) {
        if (imagePath != null && !imagePath.isBlank()) {
            try {
                File file = new File(imagePath);
                if (file.exists() && file.isFile()) {
                    return new Image(file.toURI().toString(), true);
                }
            } catch (Exception e) {
                // Fall through to fallback
            }
        }
        // Fallback: empty-foto.png aus Resources
        try {
            return new Image(ImageUtil.class.getResource(FALLBACK_IMAGE_PATH).toExternalForm(), true);
        } catch (Exception e) {
            // Kein Fallback vorhanden, null zurückgeben
            return null;
        }
    }
}
