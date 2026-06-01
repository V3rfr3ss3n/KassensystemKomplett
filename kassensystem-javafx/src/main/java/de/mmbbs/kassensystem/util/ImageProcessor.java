package de.mmbbs.kassensystem.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ImageProcessor {
    private static final int TARGET_WIDTH = 120;
    private static final int TARGET_HEIGHT = 120;
    private static final String IMAGE_DIRECTORY = "product-images";

    static {
        try {
            Files.createDirectories(Paths.get(IMAGE_DIRECTORY));
        } catch (IOException e) {
            System.err.println("Fehler beim Erstellen des Produktbilder-Ordners: " + e.getMessage());
        }
    }

    public static String processAndSaveImage(String sourcePath) {
        if (sourcePath == null || sourcePath.isBlank()) {
            return null;
        }

        try {
            File sourceFile = new File(sourcePath);
            if (!sourceFile.exists()) {
                return null;
            }

            BufferedImage originalImage = ImageIO.read(sourceFile);
            if (originalImage == null) {
                return null;
            }

            BufferedImage resizedImage = resizeImage(originalImage);

            String fileName = System.currentTimeMillis() + ".png";
            String destPath = IMAGE_DIRECTORY + File.separator + fileName;
            File destFile = new File(destPath);

            ImageIO.write(resizedImage, "PNG", destFile);

            return destFile.getAbsolutePath();
        } catch (IOException e) {
            System.err.println("Fehler beim Verarbeiten des Bildes: " + e.getMessage());
            return null;
        }
    }

    private static BufferedImage resizeImage(BufferedImage originalImage) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        BufferedImage resizedImage = new BufferedImage(TARGET_WIDTH, TARGET_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();

        // Hintergrund mit Grau füllen (als Padding für nicht-quadratische Bilder)
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, TARGET_WIDTH, TARGET_HEIGHT);

        // Bild zentriert einfügen (mit Aspect-Ratio-Erhaltung)
        double scale = Math.min((double) TARGET_WIDTH / originalWidth, (double) TARGET_HEIGHT / originalHeight);
        int scaledWidth = (int) (originalWidth * scale);
        int scaledHeight = (int) (originalHeight * scale);

        int x = (TARGET_WIDTH - scaledWidth) / 2;
        int y = (TARGET_HEIGHT - scaledHeight) / 2;

        g2d.drawImage(originalImage, x, y, scaledWidth, scaledHeight, null);
        g2d.dispose();

        return resizedImage;
    }
}
