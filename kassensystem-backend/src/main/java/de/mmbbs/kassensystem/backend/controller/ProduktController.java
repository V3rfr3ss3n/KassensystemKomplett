package de.mmbbs.kassensystem.backend.controller;

import de.mmbbs.kassensystem.backend.model.ProduktRequest;
import de.mmbbs.kassensystem.backend.model.WarenzugangRequest;
import de.mmbbs.kassensystem.backend.repository.ProduktJdbcRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    public ProduktController(ProduktJdbcRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    /**
     * Gibt alle Produkte fuer die Adminliste zurueck.
     */
    public ResponseEntity<?> getAllProdukte() {
        return ResponseEntity.ok(repository.findeAlle());
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
}
