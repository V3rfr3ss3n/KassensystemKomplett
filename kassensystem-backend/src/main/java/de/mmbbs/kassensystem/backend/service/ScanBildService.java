package de.mmbbs.kassensystem.backend.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Liest einen einzelnen Kamera-Schnappschuss; Bilder werden nicht gespeichert. */
@Service
public class ScanBildService {
    private static final int MAX_PIXEL = 4_000_000;
    private static final Map<DecodeHintType, Object> HINWEISE = Map.of(
            DecodeHintType.POSSIBLE_FORMATS,
            List.of(BarcodeFormat.QR_CODE, BarcodeFormat.EAN_13, BarcodeFormat.EAN_8, BarcodeFormat.CODE_128),
            DecodeHintType.TRY_HARDER, Boolean.TRUE);

    public Optional<String> leseCode(byte[] bildDaten) {
        try (ImageInputStream eingabe = new MemoryCacheImageInputStream(new ByteArrayInputStream(bildDaten))) {
            var leser = ImageIO.getImageReaders(eingabe);
            if (!leser.hasNext()) throw new IllegalArgumentException("Ungültiges JPEG- oder PNG-Bild.");
            ImageReader reader = leser.next();
            try {
                reader.setInput(eingabe, true, true);
                int breite = reader.getWidth(0);
                int hoehe = reader.getHeight(0);
                if (breite <= 0 || hoehe <= 0 || (long) breite * hoehe > MAX_PIXEL) {
                    throw new IllegalArgumentException("Bildauflösung ist zu groß.");
                }
                BufferedImage bild = reader.read(0);
                int[] pixel = bild.getRGB(0, 0, breite, hoehe, null, 0, breite);
                BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(breite, hoehe, pixel)));
                try {
                    return Optional.of(new MultiFormatReader().decode(bitmap, HINWEISE).getText());
                } catch (NotFoundException ex) {
                    return Optional.empty();
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Bild konnte nicht gelesen werden.", ex);
        }
    }
}
