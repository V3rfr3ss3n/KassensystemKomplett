package de.mmbbs.kassensystem.backend.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/session")
public class SessionController {
    @GetMapping
    public Map<String, Object> aktuelleSession(Authentication authentication) {
        List<String> rollen = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.replaceFirst("^ROLE_", ""))
                .toList();

        boolean admin = rollen.contains("ADMIN");
        boolean lagerist = rollen.contains("LAGERIST");

        return Map.of(
                "username", authentication.getName(),
                "roles", rollen,
                "permissions", Map.of(
                        "manageProducts", admin,
                        "bookStock", admin || lagerist
                )
        );
    }
}
