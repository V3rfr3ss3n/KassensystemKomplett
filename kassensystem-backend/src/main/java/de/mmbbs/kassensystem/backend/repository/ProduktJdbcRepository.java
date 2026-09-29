package de.mmbbs.kassensystem.backend.repository;

import de.mmbbs.kassensystem.backend.model.ProduktDto;
import de.mmbbs.kassensystem.backend.model.ProduktRequest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * JDBC-Repository fuer Produkte im Spring-Backend.
 *
 * <p>Die Klasse arbeitet direkt auf der SQLite-Tabelle `produkte`, damit das
 * Backend exakt dieselbe Datenstruktur nutzt wie die JavaFX-Kasse.</p>
 */
@Repository
public class ProduktJdbcRepository {
    private static final List<String> ERLAUBTE_EINHEITEN = List.of("STUECK", "KILOGRAMM", "LITER", "PACKUNG");
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<ProduktDto> produktMapper = (rs, rowNum) -> {
        String einheit = normalisiereEinheit(rs.getString("einheit"));
        return new ProduktDto(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getDouble("preis"),
                rs.getDouble("lagerbestand"),
                rs.getString("bildPfad"),
                einheit,
                einheitLabel(einheit),
                normalisiereSteuersatz(rs.getDouble("steuerSatz"))
        );
    };

    public ProduktJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Laedt alle Produkte fuer API und Adminoberflaeche.
     */
    public List<ProduktDto> findeAlle() {
        return jdbcTemplate.query("""
                SELECT id, name, preis, lagerbestand, bildPfad, einheit, steuerSatz
                FROM produkte
                ORDER BY name COLLATE NOCASE
                """, produktMapper);
    }

    public Optional<ProduktDto> findeNachId(int id) {
        try {
            ProduktDto produkt = jdbcTemplate.queryForObject("""
                    SELECT id, name, preis, lagerbestand, bildPfad, einheit, steuerSatz
                    FROM produkte
                    WHERE id = ?
                    """, produktMapper, id);
            return Optional.ofNullable(produkt);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    /**
     * Validiert und speichert ein neues Produkt.
     */
    public ProduktDto erstellen(ProduktRequest request) {
        ProduktRequest daten = validiere(request, true);
        Integer id = jdbcTemplate.execute((ConnectionCallback<Integer>) connection -> {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO produkte (name, preis, lagerbestand, bildPfad, einheit, steuerSatz)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """)) {
                fuelleStatement(statement, daten, 1);
                statement.executeUpdate();
            }

            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT last_insert_rowid()")) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            throw new IllegalStateException("Produkt-ID konnte nicht ermittelt werden.");
        });

        if (id == null) {
            throw new IllegalStateException("Produkt-ID konnte nicht ermittelt werden.");
        }
        return findeNachId(id).orElseThrow();
    }

    public ProduktDto aktualisieren(int id, ProduktRequest request) {
        findeNachId(id).orElseThrow(() -> new NoSuchElementException("Produkt nicht gefunden."));
        ProduktRequest daten = validiere(request, true);
        jdbcTemplate.update("""
                UPDATE produkte
                SET name = ?, preis = ?, lagerbestand = ?, bildPfad = ?, einheit = ?, steuerSatz = ?
                WHERE id = ?
                """, daten.name().trim(), daten.preis(), daten.lagerbestand(), blankToNull(daten.bildPfad()),
                normalisiereEinheit(daten.einheit()), normalisiereSteuersatz(daten.steuerSatz()), id);
        return findeNachId(id).orElseThrow();
    }

    /**
     * Erhoeht den Lagerbestand eines Produkts.
     */
    public ProduktDto warenzugangBuchen(int id, double menge) {
        if (!Double.isFinite(menge) || menge <= 0) {
            throw new IllegalArgumentException("Menge muss größer als 0 sein.");
        }
        ProduktDto produkt = findeNachId(id).orElseThrow(() -> new NoSuchElementException("Produkt nicht gefunden."));
        if (!Double.isFinite(produkt.lagerbestand() + menge)) {
            throw new IllegalArgumentException("Der resultierende Lagerbestand ist zu gross.");
        }
        jdbcTemplate.update("UPDATE produkte SET lagerbestand = lagerbestand + ? WHERE id = ?", menge, id);
        return findeNachId(produkt.id()).orElseThrow();
    }

    public void loeschen(int id) {
        int geaendert = jdbcTemplate.update("DELETE FROM produkte WHERE id = ?", id);
        if (geaendert == 0) {
            throw new NoSuchElementException("Produkt nicht gefunden.");
        }
    }

    private ProduktRequest validiere(ProduktRequest request, boolean lagerbestandPflicht) {
        if (request == null) {
            throw new IllegalArgumentException("Produktdaten fehlen.");
        }
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Name fehlt.");
        }
        if (request.preis() == null || !Double.isFinite(request.preis()) || request.preis() <= 0) {
            throw new IllegalArgumentException("Preis muss größer als 0 sein.");
        }
        if (lagerbestandPflicht && request.lagerbestand() == null) {
            throw new IllegalArgumentException("Lagerbestand fehlt.");
        }
        double lagerbestand = request.lagerbestand() == null ? 0.0 : request.lagerbestand();
        if (!Double.isFinite(lagerbestand) || lagerbestand < 0) {
            throw new IllegalArgumentException("Lagerbestand darf nicht negativ sein.");
        }

        return new ProduktRequest(
                request.name().trim(),
                request.preis(),
                lagerbestand,
                blankToNull(request.bildPfad()),
                normalisiereEinheit(request.einheit()),
                normalisiereSteuersatz(request.steuerSatz())
        );
    }

    private void fuelleStatement(PreparedStatement statement, ProduktRequest daten, int offset) throws java.sql.SQLException {
        statement.setString(offset, daten.name().trim());
        statement.setDouble(offset + 1, daten.preis());
        statement.setDouble(offset + 2, daten.lagerbestand());
        statement.setString(offset + 3, blankToNull(daten.bildPfad()));
        statement.setString(offset + 4, normalisiereEinheit(daten.einheit()));
        statement.setDouble(offset + 5, normalisiereSteuersatz(daten.steuerSatz()));
    }

    private static String normalisiereEinheit(String einheit) {
        if (einheit == null || einheit.isBlank()) {
            return "STUECK";
        }
        String normalisiert = einheit.trim().toUpperCase()
                .replace("STUCK", "STUECK")
                .replace("STUEK", "STUECK");
        if ("KG".equals(normalisiert)) {
            normalisiert = "KILOGRAMM";
        }
        if ("L".equals(normalisiert)) {
            normalisiert = "LITER";
        }
        if (!ERLAUBTE_EINHEITEN.contains(normalisiert)) {
            return "STUECK";
        }
        return normalisiert;
    }

    private static String einheitLabel(String einheit) {
        return switch (normalisiereEinheit(einheit)) {
            case "KILOGRAMM" -> "kg";
            case "LITER" -> "l";
            case "PACKUNG" -> "Packung";
            default -> "Stück";
        };
    }

    private static double normalisiereSteuersatz(Double steuerSatz) {
        if (steuerSatz == null) {
            return 19.0;
        }
        return normalisiereSteuersatz(steuerSatz.doubleValue());
    }

    private static double normalisiereSteuersatz(double steuerSatz) {
        return Math.abs(steuerSatz - 7.0) < 0.001 ? 7.0 : 19.0;
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }
}
