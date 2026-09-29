package de.mmbbs.kassensystem.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/** HTTP-Zugriff auf das Backend; Zugangsdaten bleiben nur im laufenden Prozess. */
public class ApiClient {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String basis;
    private final String authorization;

    public ApiClient(String benutzer, String passwort) {
        String configured = System.getProperty("kassensystem.api.url", System.getenv().getOrDefault(
                "KASSENSYSTEM_API_URL", "http://localhost:8080/kassensystem"));
        this.basis = configured.replaceAll("/+$", "");
        this.authorization = "Basic " + Base64.getEncoder().encodeToString(
                (benutzer + ":" + passwort).getBytes(StandardCharsets.UTF_8));
    }

    public JsonNode get(String path) { return sende("GET", path, null); }
    public JsonNode post(String path, Object body) { return sende("POST", path, body); }
    public JsonNode put(String path, Object body) { return sende("PUT", path, body); }
    public void delete(String path) { sende("DELETE", path, null); }

    private JsonNode sende(String methode, String path, Object body) {
        try {
            HttpRequest.BodyPublisher daten = body == null ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body));
            HttpRequest request = HttpRequest.newBuilder(URI.create(basis + path))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", authorization)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .method(methode, daten).build();
            HttpResponse<String> antwort = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (antwort.statusCode() >= 400) {
                String meldung = antwort.body();
                try { meldung = mapper.readTree(meldung).path("message").asText(meldung); }
                catch (Exception ignored) { }
                throw new IllegalStateException("Backend (HTTP " + antwort.statusCode() + "): " + meldung);
            }
            return antwort.body().isBlank() ? mapper.getNodeFactory().nullNode() : mapper.readTree(antwort.body());
        } catch (IOException e) {
            throw new IllegalStateException("Backend nicht erreichbar: " + basis, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Anfrage wurde unterbrochen.", e);
        }
    }
}
