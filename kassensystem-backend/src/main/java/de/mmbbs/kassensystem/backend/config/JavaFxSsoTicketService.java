package de.mmbbs.kassensystem.backend.config;

import de.mmbbs.kassensystem.backend.security.AccountPrincipal;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Kurzlebige, einmalig verwendbare Browser-Tickets. */
@Service
public class JavaFxSsoTicketService {
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

    public String issue(AccountPrincipal principal) {
        tickets.entrySet().removeIf(entry -> entry.getValue().expiresAt() < Instant.now().getEpochSecond());
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tickets.put(token, new Ticket(principal.getUsername(), principal.version(), Instant.now().plusSeconds(60).getEpochSecond()));
        return token;
    }

    public Optional<Ticket> consume(String token) {
        if (token == null) return Optional.empty();
        Ticket ticket = tickets.remove(token);
        return ticket != null && ticket.expiresAt() >= Instant.now().getEpochSecond()
                ? Optional.of(ticket) : Optional.empty();
    }

    public record Ticket(String username, long version, long expiresAt) {}
}
