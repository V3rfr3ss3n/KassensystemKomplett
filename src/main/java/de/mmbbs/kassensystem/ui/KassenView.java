package de.mmbbs.kassensystem.ui;

import de.mmbbs.kassensystem.model.Bon;
import de.mmbbs.kassensystem.model.BonPosition;
import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.service.KassenService;
import de.mmbbs.kassensystem.service.ProduktService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class KassenView extends VBox {
    private final ProduktService produktService;
    private final KassenService kassenService;
    private final ListView<Produkt> produktListe = new ListView<>();
    private final ListView<BonPosition> warenkorbListe = new ListView<>();
    private final Label gesamtPreisLabel = new Label("Gesamtpreis: 0,00 €");
    private final TextField mengeField = new TextField();
    private final Label statusLabel = new Label();

    public KassenView(ProduktService produktService, KassenService kassenService) {
        this.produktService = produktService;
        this.kassenService = kassenService;

        setSpacing(10);
        setPadding(new Insets(16));

        Label title = new Label("Kasse");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        produktListe.getItems().setAll(produktService.alleProdukte());
        produktListe.setPrefHeight(160);

        warenkorbListe.setPrefHeight(160);

        Button addButton = new Button("Zum Warenkorb hinzufügen");
        addButton.setOnAction(event -> hinzufuegen());

        Button checkoutButton = new Button("Kauf abschließen");
        checkoutButton.setOnAction(event -> abschliessen());

        Button clearButton = new Button("Warenkorb leeren");
        clearButton.setOnAction(event -> {
            kassenService.warenkorbLeeren();
            aktualisiereWarenkorb();
            statusLabel.setText("Warenkorb wurde geleert.");
        });

        HBox controls = new HBox(10, new Label("Menge:"), mengeField, addButton, checkoutButton, clearButton);
        controls.setPadding(new Insets(0, 0, 6, 0));

        getChildren().addAll(title, new Label("Produkte:"), produktListe, controls, new Label("Warenkorb:"), warenkorbListe, gesamtPreisLabel, statusLabel);
        aktualisiereWarenkorb();
    }

    private void hinzufuegen() {
        Produkt produkt = produktListe.getSelectionModel().getSelectedItem();
        if (produkt == null) {
            statusLabel.setText("Bitte Produkt auswählen.");
            return;
        }

        int menge;
        try {
            menge = Integer.parseInt(mengeField.getText().trim());
        } catch (NumberFormatException ex) {
            statusLabel.setText("Menge muss eine ganze Zahl größer als 0 sein.");
            return;
        }

        try {
            kassenService.positionHinzufuegen(produkt.getId(), menge);
            aktualisiereWarenkorb();
            statusLabel.setText("Produkt zum Warenkorb hinzugefügt.");
            mengeField.clear();
        } catch (IllegalArgumentException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }

    private void abschliessen() {
        try {
            Bon bon = kassenService.kassenvorgangAbschliessen();
            aktualisiereWarenkorb();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Bon erstellt");
            alert.setHeaderText("Kauf erfolgreich abgeschlossen");
            alert.setContentText("Bon Nr. " + bon.getBonnummer() + "\nGesamtpreis: " + String.format("%.2f €", bon.getGesamtpreis()));
            alert.showAndWait();
            statusLabel.setText("Kauf abgeschlossen. Lagerbestand wurde aktualisiert.");
            produktListe.getItems().setAll(produktService.alleProdukte());
        } catch (IllegalArgumentException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }

    private void aktualisiereWarenkorb() {
        warenkorbListe.getItems().setAll(kassenService.getWarenkorb());
        gesamtPreisLabel.setText("Gesamtpreis: " + String.format("%.2f €", kassenService.berechneGesamtpreis()));
    }
}
