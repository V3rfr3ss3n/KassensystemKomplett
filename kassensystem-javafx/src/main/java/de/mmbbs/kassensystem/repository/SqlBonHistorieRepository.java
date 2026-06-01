package de.mmbbs.kassensystem.repository;

import de.mmbbs.kassensystem.db.DatabaseInitializer;
import de.mmbbs.kassensystem.model.Bon;
import de.mmbbs.kassensystem.model.BonPosition;
import de.mmbbs.kassensystem.model.Produkt;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SqlBonHistorieRepository implements BonHistorieRepository {
    private static final String DB_URL = "jdbc:sqlite:" + DatabaseInitializer.getDbPath();
    private final ProduktRepository produktRepository;

    public SqlBonHistorieRepository(ProduktRepository produktRepository) {
        this.produktRepository = produktRepository;
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
            e.printStackTrace();
        }

        return bonHistorie;
    }

    @Override
    public void speichereBonHistorie(List<Bon> bonHistorie) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);

            for (Bon bon : bonHistorie) {
                speichereSingleBon(conn, bon);
            }

            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void speichereSingleBon(Connection conn, Bon bon) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO bons (bonnummer, datumUhrzeit, gesamtpreis) VALUES (?, ?, ?)")) {
            pstmt.setInt(1, bon.getBonnummer());
            pstmt.setString(2, bon.getDatumUhrzeit().toString());
            pstmt.setDouble(3, bon.getGesamtpreis());
            pstmt.executeUpdate();

            try (PreparedStatement posPstmt = conn.prepareStatement(
                    "INSERT INTO bon_positionen (bonnummer, produkt_id, menge, einzelpreis, gesamtpreis) VALUES (?, ?, ?, ?, ?)")) {
                for (BonPosition pos : bon.getPositionen()) {
                    posPstmt.setInt(1, bon.getBonnummer());
                    posPstmt.setInt(2, pos.getProdukt().getId());
                    posPstmt.setInt(3, pos.getMenge());
                    posPstmt.setDouble(4, pos.getEinzelpreis());
                    posPstmt.setDouble(5, pos.getGesamtpreis());
                    posPstmt.addBatch();
                }
                posPstmt.executeBatch();
            }
        }
    }

    private List<BonPosition> ladeBonPositionen(Connection conn, int bonnummer) throws SQLException {
        List<BonPosition> positionen = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(
                "SELECT produkt_id, menge FROM bon_positionen WHERE bonnummer = ?")) {
            pstmt.setInt(1, bonnummer);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int produktId = rs.getInt("produkt_id");
                    int menge = rs.getInt("menge");

                    Optional<Produkt> produktOpt = produktRepository.findeNachId(produktId);
                    if (produktOpt.isPresent()) {
                        BonPosition pos = new BonPosition(produktOpt.get(), menge);
                        positionen.add(pos);
                    }
                }
            }
        }

        return positionen;
    }
}
