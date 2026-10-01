package de.mmbbs.kassensystem.repository;

import de.mmbbs.kassensystem.db.DatabaseInitializer;
import de.mmbbs.kassensystem.model.Bon;
import de.mmbbs.kassensystem.model.BonPosition;
import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.model.Verkaufseinheit;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SqlBonHistorieRepository implements BonHistorieRepository, TransaktionalerKaufRepository {
    private static final String DB_URL = "jdbc:sqlite:" + DatabaseInitializer.getDbPath();

    public SqlBonHistorieRepository(ProduktRepository produktRepository) {
        DatabaseInitializer.initialize();
    }

    @Override
    public List<Bon> ladeBonHistorie() {
        List<Bon> bonHistorie = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT bonnummer, datumUhrzeit, gesamtpreis FROM bons ORDER BY bonnummer DESC")) {

            while (rs.next()) {
                int bonnummer = rs.getInt("bonnummer");
                LocalDateTime datumUhrzeit = LocalDateTime.parse(rs.getString("datumUhrzeit"));
                double gesamtpreis = rs.getDouble("gesamtpreis");

                List<BonPosition> positionen = ladeBonPositionen(conn, bonnummer);
                Bon bon = new Bon(bonnummer, datumUhrzeit, positionen, gesamtpreis);
                bonHistorie.add(bon);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Bon-Historie konnte nicht geladen werden.", e);
        }

        return bonHistorie;
    }

    @Override
    public void speichereBonHistorie(List<Bon> bonHistorie) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);
            try {
                for (Bon bon : bonHistorie) {
                    speichereSingleBon(conn, bon, true);
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Bon-Historie konnte nicht gespeichert werden.", e);
        }
    }

    /** Speichert Lagerabbuchung und Bon in derselben SQLite-Transaktion. */
    @Override
    public void speichereKauf(Bon bon, Map<Integer, Double> mengen) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);
            try {
                for (Map.Entry<Integer, Double> eintrag : mengen.entrySet()) {
                    try (PreparedStatement update = conn.prepareStatement(
                            "UPDATE produkte SET lagerbestand = lagerbestand - ? WHERE id = ? AND lagerbestand >= ?")) {
                        update.setDouble(1, eintrag.getValue());
                        update.setInt(2, eintrag.getKey());
                        update.setDouble(3, eintrag.getValue());
                        if (update.executeUpdate() != 1) {
                            throw new IllegalArgumentException("Nicht genuegend Produkte auf Lager.");
                        }
                    }
                }
                speichereSingleBon(conn, bon, false);
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Kauf konnte nicht gespeichert werden.", e);
        }
    }

    private void speichereSingleBon(Connection conn, Bon bon, boolean ersetzen) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
                (ersetzen ? "INSERT OR REPLACE" : "INSERT")
                        + " INTO bons (bonnummer, datumUhrzeit, gesamtpreis) VALUES (?, ?, ?)")) {
            pstmt.setInt(1, bon.getBonnummer());
            pstmt.setString(2, bon.getDatumUhrzeit().toString());
            pstmt.setDouble(3, bon.getGesamtpreis());
            pstmt.executeUpdate();

            try (PreparedStatement deletePstmt = conn.prepareStatement(
                    "DELETE FROM bon_positionen WHERE bonnummer = ?")) {
                deletePstmt.setInt(1, bon.getBonnummer());
                deletePstmt.executeUpdate();
            }

            try (PreparedStatement posPstmt = conn.prepareStatement(
                    "INSERT INTO bon_positionen (bonnummer, produkt_id, produkt_name, einheit, menge, einzelpreis, steuerSatz, gesamtpreis) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                for (BonPosition pos : bon.getPositionen()) {
                    posPstmt.setInt(1, bon.getBonnummer());
                    posPstmt.setInt(2, pos.getProdukt().getId());
                    posPstmt.setString(3, pos.getProdukt().getName());
                    posPstmt.setString(4, pos.getProdukt().getEinheit().name());
                    posPstmt.setDouble(5, pos.getMenge());
                    posPstmt.setDouble(6, pos.getEinzelpreis());
                    posPstmt.setDouble(7, pos.getSteuerSatz());
                    posPstmt.setDouble(8, pos.getGesamtpreis());
                    posPstmt.addBatch();
                }
                posPstmt.executeBatch();
            }
        }
    }

    private List<BonPosition> ladeBonPositionen(Connection conn, int bonnummer) throws SQLException {
        List<BonPosition> positionen = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT produkt_id, produkt_name, einheit, menge, einzelpreis, steuerSatz, gesamtpreis FROM bon_positionen WHERE bonnummer = ? ORDER BY id")) {
            pstmt.setInt(1, bonnummer);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int produktId = rs.getInt("produkt_id");
                    double menge = rs.getDouble("menge");
                    double einzelpreis = rs.getDouble("einzelpreis");
                    double steuerSatz = rs.getDouble("steuerSatz");
                    double gesamtpreis = rs.getDouble("gesamtpreis");

                    String name = rs.getString("produkt_name");
                    if (name == null || name.isBlank()) {
                        name = "Produkt #" + produktId;
                    }
                    Produkt snapshot = new Produkt(produktId, name, einzelpreis, 0, null,
                            Verkaufseinheit.fromLabel(rs.getString("einheit")), steuerSatz);
                    positionen.add(new BonPosition(snapshot, menge, einzelpreis, gesamtpreis, steuerSatz));
                }
            }
        }

        return positionen;
    }
}
