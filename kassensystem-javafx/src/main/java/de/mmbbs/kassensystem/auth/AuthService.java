package de.mmbbs.kassensystem.auth;

import com.fasterxml.jackson.databind.JsonNode;
import de.mmbbs.kassensystem.repository.ApiClient;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Meldet Desktop-Benutzer beim Backend an. */
public class AuthService {
    private static final String INCOMPATIBLE_BACKEND = "Das Backend ist veraltet oder passt nicht zur App. "
            + "Bitte das Backend derselben Release-Version starten.";

    public Optional<Benutzer> anmelden(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) return Optional.empty();
        try {
            JsonNode session = new ApiClient(username.trim(), password).get("/api/session");
            pruefeSession(session);
            List<BenutzerRolle> roles = new ArrayList<>();
            session.path("roles").forEach(role -> roles.add(BenutzerRolle.valueOf(role.asText())));
            if (roles.isEmpty()) throw new IllegalStateException("Das Konto hat keine Rolle.");
            Set<String> permissions = new HashSet<>();
            session.path("permissions").properties().forEach(entry -> {
                if (entry.getValue().asBoolean()) permissions.add(entry.getKey());
            });
            return Optional.of(new Benutzer(session.path("username").asText(), session.path("displayName").asText(),
                    List.copyOf(roles), Set.copyOf(permissions), session.path("mustChangePassword").asBoolean()));
        } catch (IllegalStateException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("HTTP 401")) return Optional.empty();
            throw ex;
        }
    }

    private static void pruefeSession(JsonNode session) {
        if (session == null || !session.isObject()
                || !session.path("username").isTextual()
                || !session.path("displayName").isTextual()
                || session.path("displayName").asText().isBlank()
                || !session.path("roles").isArray()
                || !session.path("mustChangePassword").isBoolean()) {
            throw new IllegalStateException(INCOMPATIBLE_BACKEND);
        }
        JsonNode permissions = session.path("permissions");
        for (String permission : List.of("products.read", "products.manage", "stock.book",
                "sales.create", "receipts.read", "users.manage")) {
            if (!permissions.path(permission).isBoolean()) {
                throw new IllegalStateException(INCOMPATIBLE_BACKEND);
            }
        }
    }

    public void passwortAendern(String username, String oldPassword, String newPassword) {
        new ApiClient(username, oldPassword).post("/api/account/password",
                java.util.Map.of("oldPassword", oldPassword, "newPassword", newPassword));
    }
}
