package de.mmbbs.kassensystem.backend.controller;

import de.mmbbs.kassensystem.backend.model.BonDto;
import de.mmbbs.kassensystem.backend.model.KaufRequest;
import de.mmbbs.kassensystem.backend.repository.KaufJdbcRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
public class KasseController {
    private final KaufJdbcRepository repository;

    public KasseController(KaufJdbcRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/api/kasse/abschluss")
    @ResponseStatus(HttpStatus.CREATED)
    public BonDto abschliessen(@RequestBody KaufRequest request) {
        return repository.abschliessen(request);
    }

    @GetMapping("/api/bons")
    public List<BonDto> historie() {
        return repository.findeAlle();
    }

    @GetMapping("/api/bons/{id}")
    public BonDto bon(@PathVariable("id") int id) {
        return repository.findeNachId(id).orElseThrow(() -> new NoSuchElementException("Bon nicht gefunden."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> badRequest(IllegalArgumentException e) {
        return Map.of("message", e.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> notFound(NoSuchElementException e) {
        return Map.of("message", e.getMessage());
    }
}
