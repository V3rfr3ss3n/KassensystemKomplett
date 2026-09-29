package de.mmbbs.kassensystem.backend.repository;

import de.mmbbs.kassensystem.backend.model.BonDto;
import de.mmbbs.kassensystem.backend.model.KaufRequest;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/** Der Server bestimmt Preise und schreibt einen Kauf samt Lagerabbuchung atomar. */
@Repository
public class KaufJdbcRepository {
    private final DataSource dataSource;

    public KaufJdbcRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public BonDto abschliessen(KaufRequest request) {
        if (request == null || request.positionen() == null || request.positionen().isEmpty()) {
            throw new IllegalArgumentException("Der Warenkorb ist leer.");
        }
        Map<Integer, Double> mengen = new LinkedHashMap<>();
        for (KaufRequest.Position position : request.positionen()) {
            if (position == null || position.produktId() <= 0 || !Double.isFinite(position.menge()) || position.menge() <= 0) {
                throw new IllegalArgumentException("Produkt und Menge müssen gültig sein.");
            }
            mengen.merge(position.produktId(), position.menge(), Double::sum);
            if (!Double.isFinite(mengen.get(position.produktId()))) {
                throw new IllegalArgumentException("Menge ist zu gross.");
            }
        }
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                List<BonDto.Position> positionen = new ArrayList<>();
                double gesamt = 0;
                for (var entry : mengen.entrySet()) {
                    ProduktStand produkt = produkt(connection, entry.getKey());
                    double menge = entry.getValue();
                    if (produkt.bestand < menge) {
                        throw new IllegalArgumentException("Nicht genügend Bestand für " + produkt.name + ".");
                    }
                    double betrag = Math.round(produkt.preis * menge * 100.0) / 100.0;
                    if (!Double.isFinite(betrag) || betrag <= 0) {
                        throw new IllegalArgumentException("Positionsbetrag ist ungültig.");
                    }
                    gesamt += betrag;
                    positionen.add(new BonDto.Position(entry.getKey(), produkt.name, produkt.einheit,
                            menge, produkt.preis, produkt.steuerSatz, betrag));
                }
                if (!Double.isFinite(gesamt)) {
                    throw new IllegalArgumentException("Gesamtbetrag ist zu gross.");
                }
                for (BonDto.Position position : positionen) {
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE produkte SET lagerbestand = lagerbestand - ? WHERE id = ? AND lagerbestand >= ?")) {
                        update.setDouble(1, position.menge());
                        update.setInt(2, position.produktId());
                        update.setDouble(3, position.menge());
                        if (update.executeUpdate() != 1) {
                            throw new IllegalArgumentException("Nicht genügend Bestand vorhanden.");
                        }
                    }
                }
                LocalDateTime datum = LocalDateTime.now();
                int nummer;
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO bons (datumUhrzeit, gesamtpreis) VALUES (?, ?)")) {
                    insert.setString(1, datum.toString());
                    insert.setDouble(2, gesamt);
                    insert.executeUpdate();
                    try (Statement keyQuery = connection.createStatement();
                         ResultSet keys = keyQuery.executeQuery("SELECT last_insert_rowid()")) {
                        if (!keys.next()) throw new SQLException("Bonnummer fehlt.");
                        nummer = keys.getInt(1);
                    }
                }
                for (BonDto.Position position : positionen) {
                    try (PreparedStatement insert = connection.prepareStatement("""
                            INSERT INTO bon_positionen (bonnummer, produkt_id, produkt_name, einheit, menge,
                                einzelpreis, steuerSatz, gesamtpreis) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                            """)) {
                        insert.setInt(1, nummer);
                        insert.setInt(2, position.produktId());
                        insert.setString(3, position.produktName());
                        insert.setString(4, position.einheit());
                        insert.setDouble(5, position.menge());
                        insert.setDouble(6, position.einzelpreis());
                        insert.setDouble(7, position.steuerSatz());
                        insert.setDouble(8, position.gesamtpreis());
                        insert.executeUpdate();
                    }
                }
                connection.commit();
                return new BonDto(nummer, datum, gesamt, positionen);
            } catch (Exception e) {
                connection.rollback();
                if (e instanceof RuntimeException runtime) throw runtime;
                throw new IllegalStateException("Kauf konnte nicht gespeichert werden.", e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Datenbank nicht erreichbar.", e);
        }
    }

    public List<BonDto> findeAlle() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT bonnummer, datumUhrzeit, gesamtpreis FROM bons ORDER BY bonnummer DESC");
             ResultSet rs = statement.executeQuery()) {
            List<BonDto> bons = new ArrayList<>();
            while (rs.next()) bons.add(leseBon(connection, rs));
            return bons;
        } catch (SQLException e) {
            throw new IllegalStateException("Bon-Historie konnte nicht geladen werden.", e);
        }
    }

    public Optional<BonDto> findeNachId(int id) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT bonnummer, datumUhrzeit, gesamtpreis FROM bons WHERE bonnummer = ?")) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? Optional.of(leseBon(connection, rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Bon konnte nicht geladen werden.", e);
        }
    }

    private BonDto leseBon(Connection connection, ResultSet bon) throws SQLException {
        int nummer = bon.getInt("bonnummer");
        List<BonDto.Position> positionen = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT bp.produkt_id, COALESCE(bp.produkt_name, p.name, 'Produkt #' || bp.produkt_id) AS produkt_name,
                       COALESCE(bp.einheit, p.einheit, 'STUECK') AS einheit, bp.menge, bp.einzelpreis,
                       bp.steuerSatz, bp.gesamtpreis
                FROM bon_positionen bp LEFT JOIN produkte p ON p.id = bp.produkt_id
                WHERE bp.bonnummer = ? ORDER BY bp.id
                """)) {
            statement.setInt(1, nummer);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) positionen.add(new BonDto.Position(rs.getInt(1), rs.getString(2), rs.getString(3),
                        rs.getDouble(4), rs.getDouble(5), rs.getDouble(6), rs.getDouble(7)));
            }
        }
        return new BonDto(nummer, LocalDateTime.parse(bon.getString("datumUhrzeit").replace(' ', 'T')),
                bon.getDouble("gesamtpreis"), positionen);
    }

    private ProduktStand produkt(Connection connection, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT name, preis, lagerbestand, einheit, steuerSatz FROM produkte WHERE id = ?")) {
            statement.setInt(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) throw new NoSuchElementException("Produkt nicht gefunden: " + id);
                return new ProduktStand(rs.getString(1), rs.getDouble(2), rs.getDouble(3), rs.getString(4), rs.getDouble(5));
            }
        }
    }

    private record ProduktStand(String name, double preis, double bestand, String einheit, double steuerSatz) {}
}
