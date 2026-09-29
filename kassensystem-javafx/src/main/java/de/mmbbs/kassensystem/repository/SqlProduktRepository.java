package de.mmbbs.kassensystem.repository;

import de.mmbbs.kassensystem.db.DatabaseInitializer;
import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.model.Steuersatz;
import de.mmbbs.kassensystem.model.Verkaufseinheit;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite-Implementierung der Produktdatenhaltung fuer die JavaFX-Anwendung.
 *
 * <p>Dieses Repository nutzt dieselbe Datenbankdatei wie das Spring-Backend.
 * Dadurch wirken Aenderungen aus der Admin-Weboberflaeche auch in der Kasse.</p>
 */
public class SqlProduktRepository implements ProduktRepository {
    private static final String DB_URL = "jdbc:sqlite:" + DatabaseInitializer.getDbPath();

    public SqlProduktRepository() {
        DatabaseInitializer.initialize();
    }

    @Override
    public List<Produkt> findeAlle() {
        List<Produkt> produkte = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name, preis, lagerbestand, bildPfad, einheit, steuerSatz FROM produkte")) {

            while (rs.next()) {
                Produkt p = new Produkt(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getDouble("preis"),
                    rs.getDouble("lagerbestand"),
                    rs.getString("bildPfad"),
                    Verkaufseinheit.fromLabel(rs.getString("einheit")),
                    normalisiereSteuersatz(rs.getDouble("steuerSatz"))
                );
                produkte.add(p);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Produkte konnten nicht geladen werden.", e);
        }

        return produkte;
    }

    @Override
    public Optional<Produkt> findeNachId(int id) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT id, name, preis, lagerbestand, bildPfad, einheit, steuerSatz FROM produkte WHERE id = ?")) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Produkt(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("preis"),
                        rs.getDouble("lagerbestand"),
                        rs.getString("bildPfad"),
                        Verkaufseinheit.fromLabel(rs.getString("einheit")),
                        normalisiereSteuersatz(rs.getDouble("steuerSatz"))
                    ));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Produkt konnte nicht geladen werden.", e);
        }

        return Optional.empty();
    }

    @Override
    public Produkt speichern(Produkt produkt) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            int produktId = produkt.getId();
            if (produktId == 0) {
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO produkte (name, preis, lagerbestand, bildPfad, einheit, steuerSatz) VALUES (?, ?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    fuelleProduktwerte(pstmt, produkt, 1);
                    pstmt.executeUpdate();
                    try (ResultSet keys = pstmt.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Produkt-ID fehlt.");
                        }
                        produktId = keys.getInt(1);
                    }
                }
            } else {
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "UPDATE produkte SET name = ?, preis = ?, lagerbestand = ?, bildPfad = ?, einheit = ?, steuerSatz = ? WHERE id = ?")) {
                    fuelleProduktwerte(pstmt, produkt, 1);
                    pstmt.setInt(7, produktId);
                    if (pstmt.executeUpdate() != 1) {
                        throw new IllegalArgumentException("Produkt nicht gefunden.");
                    }
                }
            }

            return new Produkt(produktId, produkt.getName(), produkt.getPreis(), produkt.getLagerbestand(),
                    produkt.getBildPfad(), produkt.getEinheit(), produkt.getSteuerSatz());
        } catch (SQLException e) {
            throw new IllegalStateException("Produkt konnte nicht gespeichert werden.", e);
        }

    }

    @Override
    public void loeschen(int id) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM produkte WHERE id = ?")) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Produkt konnte nicht geloescht werden.", e);
        }
    }

    private void fuelleProduktwerte(PreparedStatement pstmt, Produkt produkt, int start) throws SQLException {
        pstmt.setString(start, produkt.getName());
        pstmt.setDouble(start + 1, produkt.getPreis());
        pstmt.setDouble(start + 2, produkt.getLagerbestand());
        pstmt.setString(start + 3, produkt.getBildPfad());
        pstmt.setString(start + 4, produkt.getEinheit().name());
        pstmt.setDouble(start + 5, produkt.getSteuerSatz());
    }

    private double normalisiereSteuersatz(double steuerSatz) {
        return steuerSatz == 0 ? Steuersatz.REGELSTEUERSATZ.getProzent() : steuerSatz;
    }
}
