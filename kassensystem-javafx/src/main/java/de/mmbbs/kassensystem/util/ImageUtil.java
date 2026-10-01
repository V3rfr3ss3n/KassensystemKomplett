package de.mmbbs.kassensystem.util;

import de.mmbbs.kassensystem.repository.ApiClient;
import javafx.scene.image.Image;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class ImageUtil {
    private static final String FALLBACK_IMAGE_PATH = "/empty-foto.png";
    private static final Map<String, Image> CACHE = new LinkedHashMap<>(128, .75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
            return size() > 120;
        }
    };

    public static synchronized Image loadProductImage(String imagePath) {
        if (imagePath != null && !imagePath.isBlank()) {
            try {
                String url = null;
                if (imagePath.startsWith("api/bilder/")) {
                    url = ApiClient.basisUrl() + "/" + imagePath;
                } else if (imagePath.startsWith("https://") || imagePath.startsWith("http://")) {
                    url = imagePath;
                }
                if (url != null) {
                    String bildUrl = url;
                    return CACHE.computeIfAbsent(bildUrl, pfad -> new Image(pfad, 320, 320, true, true, true));
                }
                File file = new File(imagePath);
                if (file.exists() && file.isFile()) {
                    String bildUrl = file.toURI().toString();
                    return CACHE.computeIfAbsent(bildUrl, pfad -> new Image(pfad, 320, 320, true, true, true));
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
