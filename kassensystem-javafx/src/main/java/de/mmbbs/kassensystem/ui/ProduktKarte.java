package de.mmbbs.kassensystem.ui;

import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.util.GeldFormatter;
import de.mmbbs.kassensystem.util.ImageUtil;
import de.mmbbs.kassensystem.util.MengenFormatter;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/** Eine Produktkarte der visuellen Produktauswahl. */
final class ProduktKarte extends VBox {
    ProduktKarte(Produkt produkt, boolean favorit, Consumer<Produkt> auswahl, Runnable favoritUmschalten) {
        super(8);
        getStyleClass().add("product-card");
        setAlignment(Pos.TOP_LEFT);
        setPrefWidth(188);
        setMinWidth(188);
        setMaxWidth(188);
        boolean hatBild = produkt.getBildPfad() != null && !produkt.getBildPfad().isBlank();
        ImageView bild = new ImageView(hatBild ? ImageUtil.loadProductImage(produkt.getBildPfad()) : null);
        bild.setFitWidth(158);
        bild.setFitHeight(126);
        bild.setPreserveRatio(true);
        StackPane rahmen = new StackPane(hatBild ? bild : new Label("Kein Bild"));
        rahmen.getStyleClass().add("product-picker-image");
        rahmen.setMinHeight(135);
        rahmen.setMaxHeight(135);
        Label name = new Label(produkt.getName());
        name.getStyleClass().add("product-card-name");
        name.setWrapText(true);
        name.setMaxWidth(175);
        Label preis = new Label(GeldFormatter.formatiereBetrag(produkt.getPreis()) + "/" + produkt.getEinheitLabel());
        preis.getStyleClass().add("product-card-price");
        Label kategorie = new Label(produkt.getKategorie());
        kategorie.getStyleClass().add("product-card-details");
        Label bestand = new Label("Lagerbestand: " + MengenFormatter.formatiereMenge(produkt.getLagerbestand(), produkt.getEinheitLabel()));
        bestand.getStyleClass().add("product-card-details");
        Button waehlen = new Button("Auswählen");
        waehlen.getStyleClass().add("primary-button");
        waehlen.setMaxWidth(Double.MAX_VALUE);
        waehlen.setDisable(produkt.getLagerbestand() <= 0);
        waehlen.setOnAction(event -> auswahl.accept(produkt));
        Button stern = new Button(favorit ? "★ Favorit" : "☆ Merken");
        stern.setOnAction(event -> favoritUmschalten.run());
        getChildren().addAll(rahmen, name, kategorie, preis, bestand, waehlen, stern);
        if (produkt.getLagerbestand() <= 0) getStyleClass().add("product-card-unavailable");
    }
}
