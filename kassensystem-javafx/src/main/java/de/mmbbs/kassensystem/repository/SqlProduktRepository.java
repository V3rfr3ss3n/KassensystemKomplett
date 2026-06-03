package de.mmbbs.kassensystem.repository;

import de.mmbbs.kassensystem.db.DatabaseInitializer;
import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.model.Steuersatz;
import de.mmbbs.kassensystem.model.Verkaufseinheit;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
            e.printStackTrace();
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
            e.printStackTrace();
        }

        return Optional.empty();
    }

    @Override
    public Produkt speichern(Produkt produkt) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            int produktId = produkt.getId();

            if (produktId == 0) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT MAX(id) as maxId FROM produkte")) {
                    produktId = rs.next() ? rs.getInt("maxId") + 1 : 1;
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT OR REPLACE INTO produkte (id, name, preis, lagerbestand, bildPfad, einheit, steuerSatz) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                pstmt.setInt(1, produktId);
                pstmt.setString(2, produkt.getName());
                pstmt.setDouble(3, produkt.getPreis());
                pstmt.setDouble(4, produkt.getLagerbestand());
                pstmt.setString(5, produkt.getBildPfad());
                pstmt.setString(6, produkt.getEinheit().name());
                pstmt.setDouble(7, produkt.getSteuerSatz());
                pstmt.executeUpdate();
            }

            return new Produkt(produktId, produkt.getName(), produkt.getPreis(), produkt.getLagerbestand(),
                    produkt.getBildPfad(), produkt.getEinheit(), produkt.getSteuerSatz());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return produkt;
    }

    @Override
    public void loeschen(int id) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("DELETE FROM produkte WHERE id = ?")) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private double normalisiereSteuersatz(double steuerSatz) {
        return steuerSatz == 0 ? Steuersatz.REGELSTEUERSATZ.getProzent() : steuerSatz;
    }
}
