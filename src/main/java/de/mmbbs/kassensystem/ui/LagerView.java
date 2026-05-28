package de.mmbbs.kassensystem.ui;

import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.service.ProduktService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class LagerView extends VBox {
    private final ProduktService produktService;
    private final ListView<Produkt> produktListe = new ListView<>();
    private final TextField mengeField = new TextField();
    private final Label statusLabel = new Label();

    public LagerView(ProduktService produktService) {
        this.produktService = produktService;

        setSpacing(10);
        setPadding(new Insets(16));

        Label title = new Label("Lagerbestand");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        produktListe.getItems().setAll(produktService.alleProdukte());
        produktListe.setPrefHeight(180);

        Button refreshButton = new Button("Aktualisieren");
        refreshButton.setOnAction(event -> produktListe.getItems().setAll(produktService.alleProdukte()));

        Button zugangButton = new Button("Warenzugang buchen");
        zugangButton.setOnAction(event -> bucheWarenzugang());

        HBox controls = new HBox(10, new Label("Menge:"), mengeField, zugangButton, refreshButton);

        getChildren().addAll(title, produktListe, controls, statusLabel);
    }

    private void bucheWarenzugang() {
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
            produktService.warenzugangErfassen(produkt.getId(), menge);
            produktListe.getItems().setAll(produktService.alleProdukte());
            statusLabel.setText("Warenzugang wurde gespeichert.");
            mengeField.clear();
        } catch (IllegalArgumentException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }
}
