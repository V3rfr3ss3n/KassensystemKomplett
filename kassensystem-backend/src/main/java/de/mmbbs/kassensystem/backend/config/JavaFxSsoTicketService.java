package de.mmbbs.kassensystem.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@Service
public class JavaFxSsoTicketService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final String secret;

    public JavaFxSsoTicketService(@Value("${kassensystem.sso.secret:kassensystem-dev-secret-2026}") String secret) {
        this.secret = secret;
    }

    public Optional<Ticket> validiere(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            return Optional.empty();
        }

        String[] teile = ticket.split("\\.", 2);
        if (teile.length != 2) {
            return Optional.empty();
        }

        String payload = dekodiere(teile[0]);
        if (payload == null || !signaturPasst(payload, teile[1])) {
            return Optional.empty();
        }

        String[] payloadTeile = payload.split(":", 3);
        if (payloadTeile.length != 3) {
            return Optional.empty();
        }

        long expiresAt;
        try {
            expiresAt = Long.parseLong(payloadTeile[2]);
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }

        if (Instant.now().getEpochSecond() > expiresAt) {
            return Optional.empty();
        }

        return Optional.of(new Ticket(payloadTeile[0], payloadTeile[1]));
    }

    private boolean signaturPasst(String payload, String signatur) {
        byte[] erwartet = signiere(payload).getBytes(StandardCharsets.UTF_8);
        byte[] erhalten = signatur.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(erwartet, erhalten);
    }

    private String signiere(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SSO-Ticket konnte nicht geprueft werden.", ex);
        }
    }

    private String dekodiere(String payload) {
        try {
            return new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public record Ticket(String benutzername, String rolle) {
    }
}
