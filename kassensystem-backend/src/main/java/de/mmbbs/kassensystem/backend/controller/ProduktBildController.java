package de.mmbbs.kassensystem.backend.controller;

import de.mmbbs.kassensystem.backend.config.DatabasePathResolver;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

/** Speichert Produktbilder neben der SQLite-Datei, damit Docker und Desktop dieselben Bilder sehen. */
@RestController
@RequestMapping("/api/bilder")
public class ProduktBildController {
    private static final long MAX_BYTES = 5_000_000;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> hochladen(@RequestParam("datei") MultipartFile datei) throws IOException {
        if (datei.isEmpty() || datei.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("Bitte ein Bild bis 5 MB auswählen.");
        }
        if (datei.getContentType() == null
                || !Set.of("image/png", "image/jpeg", "image/gif").contains(datei.getContentType())) {
            throw new IllegalArgumentException("Bitte eine gültige PNG-, JPEG- oder GIF-Datei auswählen.");
        }
        BufferedImage bild;
        try (InputStream eingabe = datei.getInputStream();
             ImageInputStream bildStream = ImageIO.createImageInputStream(eingabe)) {
            if (bildStream == null) throw new IllegalArgumentException("Bilddatei konnte nicht gelesen werden.");
            Iterator<ImageReader> leser = ImageIO.getImageReaders(bildStream);
            if (!leser.hasNext()) throw new IllegalArgumentException("Bildformat wird nicht unterstützt.");
            ImageReader reader = leser.next();
            try {
                reader.setInput(bildStream);
                if (reader.getWidth(0) > 5000 || reader.getHeight(0) > 5000) {
                    throw new IllegalArgumentException("Das Bild ist zu groß. Maximal 5000 × 5000 Pixel.");
                }
                bild = reader.read(0);
            } finally {
                reader.dispose();
            }
        }
        double faktor = Math.min(1.0, 1200.0 / Math.max(bild.getWidth(), bild.getHeight()));
        int breite = Math.max(1, (int) Math.round(bild.getWidth() * faktor));
        int hoehe = Math.max(1, (int) Math.round(bild.getHeight() * faktor));
        BufferedImage optimiert = new BufferedImage(breite, hoehe, BufferedImage.TYPE_INT_ARGB);
        Graphics2D grafik = optimiert.createGraphics();
        try {
            grafik.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            grafik.drawImage(bild, 0, 0, breite, hoehe, null);
        } finally {
            grafik.dispose();
        }
        Path ordner = DatabasePathResolver.resolve().getParent().resolve("images");
        Files.createDirectories(ordner);
        String dateiname = UUID.randomUUID() + ".png";
        Path ziel = ordner.resolve(dateiname);
        if (!ImageIO.write(optimiert, "png", ziel.toFile())) {
            throw new IOException("Bild konnte nicht gespeichert werden.");
        }
        return Map.of("url", "api/bilder/" + dateiname);
    }

    @GetMapping(value = "/{dateiname}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> laden(@PathVariable("dateiname") String dateiname) throws IOException {
        if (!dateiname.matches("[0-9a-f-]{36}\\.png")) return ResponseEntity.notFound().build();
        Path bild = DatabasePathResolver.resolve().getParent().resolve("images").resolve(dateiname);
        if (!Files.isRegularFile(bild)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(Files.readAllBytes(bild));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> ungueltig(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}
