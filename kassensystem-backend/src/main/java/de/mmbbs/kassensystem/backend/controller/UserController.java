package de.mmbbs.kassensystem.backend.controller;

import de.mmbbs.kassensystem.backend.security.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class UserController {
    private final AccountService accounts;

    public UserController(AccountService accounts) { this.accounts = accounts; }

    @GetMapping("/permissions")
    public Map<String, Object> permissions() {
        return Map.of("roles", AccountService.ROLES, "permissions", AccountService.PERMISSIONS,
                "roleDefaults", accounts.roleDefaults());
    }

    @GetMapping("/users")
    public List<Map<String, Object>> list() { return accounts.list(); }

    @GetMapping("/users/{id}")
    public Map<String, Object> get(@PathVariable("id") long id) {
        var account = accounts.findById(id);
        if (account == null) throw new IllegalArgumentException("Benutzer nicht gefunden.");
        return accounts.publicView(account);
    }

    @PostMapping("/users")
    public ResponseEntity<Map<String, Object>> create(@RequestBody UserInput body, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accounts.create(authentication.getName(), body.username(),
                body.displayName(), body.password(), body.active(), body.roles(), body.overrides()));
    }

    @PatchMapping("/users/{id}")
    public Map<String, Object> update(@PathVariable("id") long id, @RequestBody UserInput body, Authentication authentication) {
        return accounts.update(authentication.getName(), id, body.displayName(), body.active(), body.roles(), body.overrides());
    }

    @PostMapping("/users/{id}/password-reset")
    public ResponseEntity<Void> reset(@PathVariable("id") long id, @RequestBody PasswordReset body, Authentication authentication) {
        accounts.resetPassword(authentication.getName(), id, body.password());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badInput(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }

    public record UserInput(String username, String displayName, String password, Boolean active,
                            List<String> roles, Map<String, Boolean> overrides) {}
    public record PasswordReset(String password) {}
}
