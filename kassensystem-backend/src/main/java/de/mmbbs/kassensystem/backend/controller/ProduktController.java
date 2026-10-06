package de.mmbbs.kassensystem.backend.controller;

import de.mmbbs.kassensystem.backend.model.ProduktRequest;
import de.mmbbs.kassensystem.backend.model.EtikettenRequest;
import de.mmbbs.kassensystem.backend.model.WarenzugangRequest;
import de.mmbbs.kassensystem.backend.repository.ProduktJdbcRepository;
import de.mmbbs.kassensystem.backend.service.EtikettenPdfService;
import de.mmbbs.kassensystem.backend.service.ScanBildService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.List;
import java.util.LinkedHashSet;
import java.io.IOException;

/**
 * REST-Endpunkte fuer Produkt- und Lagerverwaltung.
 *
 * <p>Die Browser-Adminoberflaeche nutzt diesen Controller, um Produkte aus der
 * gemeinsamen SQLite-Datenbank zu lesen und zu bearbeiten.</p>
 */
@RestController
@RequestMapping("/api/produkte")
public class ProduktController {
    private final ProduktJdbcRepository repository;
    private final EtikettenPdfService etiketten;
    private final ScanBildService scanBild;

    public ProduktController(ProduktJdbcRepository repository, EtikettenPdfService etiketten, ScanBildService scanBild) {
        this.repository = repository;
        this.etiketten = etiketten;
        this.scanBild = scanBild;
    }

    @GetMapping
    /**
     * Gibt alle Produkte fuer die Adminliste zurueck.
     */
    public ResponseEntity<?> getAllProdukte() {
        return ResponseEntity.ok(repository.findeAlle());
    }

    @PostMapping(value = "/scan-bild", consumes = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    public ResponseEntity<?> scanBild(HttpServletRequest request) throws IOException {
        byte[] bild = request.getInputStream().readNBytes(1_048_577);
        if (bild.length > 1_048_576) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(Map.of("message", "Bild darf höchstens 1 MB groß sein."));
        }
        return scanBild.leseCode(bild)
                .<ResponseEntity<?>>map(code -> ResponseEntity.ok(Map.of("scanCode", code)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping(value = "/etiketten", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> etiketten(@RequestBody(required = false) EtikettenRequest request) {
        if (request == null || request.produktIds() == null || request.produktIds().isEmpty()) {
            throw new IllegalArgumentException("Bitte mindestens ein Produkt auswählen.");
        }
        if (request.produktIds().size() > 1000 || request.produktIds().contains(null)) {
            throw new IllegalArgumentException("Ungültige Produktauswahl.");
        }
        List<de.mmbbs.kassensystem.backend.model.ProduktDto> produkte = new LinkedHashSet<>(request.produktIds())
                .stream().map(id -> repository.findeNachId(id)
                        .orElseThrow(() -> new NoSuchElementException("Produkt " + id + " nicht gefunden."))).toList();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=qr-etiketten.pdf")
                .body(etiketten.erstelle(produkte));
    }

    @PostMapping
    /**
     * Legt ein neues Produkt an.
     */
    public ResponseEntity<?> createProdukt(@RequestBody ProduktRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.erstellen(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProdukt(@PathVariable("id") int id) {
        return repository.findeNachId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProdukt(@PathVariable("id") int id, @RequestBody ProduktRequest request) {
        return ResponseEntity.ok(repository.aktualisieren(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProdukt(@PathVariable("id") int id) {
        repository.loeschen(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/warenzugang")
    /**
     * Bucht einen Warenzugang fuer ein Produkt.
     */
    public ResponseEntity<?> buchWarenzugang(@PathVariable("id") int id,
                                             @RequestBody(required = false) WarenzugangRequest request,
                                             @RequestParam(name = "menge", required = false) Double menge) {
        double buchungsmenge = request != null && request.menge() != null ? request.menge() : (menge == null ? 0.0 : menge);
        return ResponseEntity.ok(repository.warenzugangBuchen(id, buchungsmenge));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<?> handleNotFound(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<?> handleDatabaseError(DataAccessException ex) {
        Throwable ursache = ex.getMostSpecificCause();
        if (ex.getCause() instanceof IllegalArgumentException argument) {
            return ResponseEntity.badRequest().body(Map.of("message", argument.getMessage()));
        }
        if (ursache instanceof java.sql.SQLException sql && sql.getErrorCode() == 19
                && sql.getMessage() != null && sql.getMessage().contains("produkte.scan_code")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Scan-Code ist bereits vergeben."));
        }
        throw ex;
    }
}
