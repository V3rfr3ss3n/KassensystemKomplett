package de.mmbbs.kassensystem.auth;

import java.util.List;
import java.util.Set;

/** Serverseitig bestätigte Identität und wirksame Rechte. */
public record Benutzer(String benutzername, String anzeigename, List<BenutzerRolle> rollen,
                       Set<String> rechte, boolean passwortwechselNoetig) {
    public Benutzer(String benutzername, BenutzerRolle rolle) {
        this(benutzername, benutzername, List.of(rolle), switch (rolle) {
            case ADMIN -> Set.of("products.read", "products.manage", "stock.book", "sales.create", "receipts.read", "users.manage");
            case KASSIERER -> Set.of("products.read", "sales.create", "receipts.read");
            case LAGERIST -> Set.of("products.read", "stock.book");
        }, false);
    }

    public BenutzerRolle rolle() { return rollen.getFirst(); }
    public boolean istAdmin() { return rechte.contains("users.manage"); }
    public boolean istLagerist() { return darfWarenzugangBuchen() && !darfProduktverwaltung(); }
    public boolean darfKassieren() { return rechte.contains("sales.create"); }
    public boolean darfProduktverwaltung() { return rechte.contains("products.manage"); }
    public boolean darfWarenzugangBuchen() { return rechte.contains("stock.book"); }
    public boolean darfWebVerwaltungNutzen() {
        return darfProduktverwaltung() || darfWarenzugangBuchen() || rechte.contains("users.manage");
    }
    public String rollenText() { return String.join(", ", rollen.stream().map(BenutzerRolle::getAnzeigename).toList()); }
}
