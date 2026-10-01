package de.mmbbs.kassensystem.auth;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * Erzeugt kurzlebige, signierte Login-Tickets fuer den Spring-Adminbereich.
 */
public class SsoTicketService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Duration TICKET_GUELTIGKEIT = Duration.ofMinutes(2);
    private static final String DEFAULT_SECRET = "kassensystem-dev-secret-2026";

    private final String secret;

    public SsoTicketService() {
        this(System.getProperty("kassensystem.sso.secret", DEFAULT_SECRET));
    }

    public SsoTicketService(String secret) {
        this.secret = secret;
    }

    public String erstelleTicket(Benutzer benutzer) {
        if (!benutzer.darfWebVerwaltungNutzen()) {
            throw new IllegalArgumentException("Benutzer darf die Webverwaltung nicht nutzen.");
        }

        long expiresAt = Instant.now().plus(TICKET_GUELTIGKEIT).getEpochSecond();
        String payload = benutzer.benutzername() + ":" + benutzer.rolle().name() + ":" + expiresAt;
        return base64Url(payload) + "." + signiere(payload);
    }

    private String signiere(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SSO-Ticket konnte nicht signiert werden.", ex);
        }
    }

    private String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
