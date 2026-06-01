package de.mmbbs.kassensystem.repository;

import de.mmbbs.kassensystem.db.DatabaseInitializer;
import de.mmbbs.kassensystem.model.Produkt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqlProduktRepository implements ProduktRepository {
    private static final String DB_URL = "jdbc:sqlite:" + DatabaseInitializer.getDbPath();

    public SqlProduktRepository() {
        DatabaseInitializer.initialize();
    }

    @Override
    public void speichern(List<Produkt> produkte) {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            conn.setAutoCommit(false);

            try (Statement stmt = conn.createStatement()) {
                stmt.execute("DELETE FROM produkte");
            }

            try (PreparedStatement pstmt = conn.prepareStatement(
                    "INSERT INTO produkte (id, name, preis, lagerbestand, bildPfad) VALUES (?, ?, ?, ?, ?)")) {
                for (Produkt p : produkte) {
                    pstmt.setInt(1, p.getId());
                    pstmt.setString(2, p.getName());
                    pstmt.setDouble(3, p.getPreis());
                    pstmt.setInt(4, p.getLagerbestand());
                    pstmt.setString(5, p.getBildPfad());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }

            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Produkt> laden() {
        List<Produkt> produkte = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name, preis, lagerbestand, bildPfad FROM produkte")) {

            while (rs.next()) {
                Produkt p = new Produkt(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getDouble("preis"),
                    rs.getInt("lagerbestand"),
                    rs.getString("bildPfad")
                );
                produkte.add(p);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return produkte;
    }

    @Override
    public Produkt findeNachId(int id) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT id, name, preis, lagerbestand, bildPfad FROM produkte WHERE id = ?")) {

            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Produkt(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getDouble("preis"),
                        rs.getInt("lagerbestand"),
                        rs.getString("bildPfad")
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }
}
