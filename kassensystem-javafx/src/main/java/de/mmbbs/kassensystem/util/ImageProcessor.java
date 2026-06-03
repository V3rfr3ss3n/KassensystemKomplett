package de.mmbbs.kassensystem.util;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ImageProcessor {
    public static final int TARGET_WIDTH = 120;
    public static final int TARGET_HEIGHT = 120;
    private static final String IMAGE_DIRECTORY = "product-images";

    public record CropArea(double x, double y, double size) {
    }

    static {
        try {
            Files.createDirectories(Paths.get(IMAGE_DIRECTORY));
        } catch (IOException e) {
            System.err.println("Fehler beim Erstellen des Produktbilder-Ordners: " + e.getMessage());
        }
    }

    public static String processAndSaveImage(String sourcePath) {
        return processAndSaveImage(sourcePath, null);
    }

    public static String processAndSaveImage(String sourcePath, CropArea cropArea) {
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

            BufferedImage resizedImage = cropAndResizeImage(originalImage, cropArea);

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

    private static BufferedImage cropAndResizeImage(BufferedImage originalImage, CropArea cropArea) {
        CropArea normalizedCropArea = normalizeCropArea(originalImage, cropArea);
        BufferedImage resizedImage = new BufferedImage(TARGET_WIDTH, TARGET_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resizedImage.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double scale = (double) TARGET_WIDTH / normalizedCropArea.size();
        AffineTransform transform = new AffineTransform();
        transform.translate(-normalizedCropArea.x() * scale, -normalizedCropArea.y() * scale);
        transform.scale(scale, scale);

        g2d.drawImage(originalImage, transform, null);
        g2d.dispose();

        return resizedImage;
    }

    private static CropArea normalizeCropArea(BufferedImage originalImage, CropArea cropArea) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        double maxSize = Math.min(originalWidth, originalHeight);

        if (cropArea == null) {
            double x = (originalWidth - maxSize) / 2.0;
            double y = (originalHeight - maxSize) / 2.0;
            return new CropArea(x, y, maxSize);
        }

        double size = clamp(cropArea.size(), 1, maxSize);
        double x = clamp(cropArea.x(), 0, originalWidth - size);
        double y = clamp(cropArea.y(), 0, originalHeight - size);

        return new CropArea(x, y, size);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
