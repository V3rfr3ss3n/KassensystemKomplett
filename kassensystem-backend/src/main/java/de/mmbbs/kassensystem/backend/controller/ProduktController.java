package de.mmbbs.kassensystem.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/produkte")
public class ProduktController {

    @GetMapping
    public ResponseEntity<?> getAllProdukte() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "REST API Struktur vorbereitet");
        response.put("status", "Implementation folgt in Phase 4");
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<?> createProdukt(@RequestBody Map<String, Object> body) {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Produkt-Erstellung folgt");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProdukt(@PathVariable int id) {
        Map<String, String> response = new HashMap<>();
        response.put("id", String.valueOf(id));
        response.put("message", "Produkt-Details folgen");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProdukt(@PathVariable int id, @RequestBody Map<String, Object> body) {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Produkt-Update folgt");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProdukt(@PathVariable int id) {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Produkt-Löschung folgt");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/warenzugang")
    public ResponseEntity<?> buchWarenzugang(@PathVariable int id, @RequestParam int menge) {
        Map<String, String> response = new HashMap<>();
        response.put("message", "Warenzugang-Buchung folgt");
        return ResponseEntity.ok(response);
    }
}
