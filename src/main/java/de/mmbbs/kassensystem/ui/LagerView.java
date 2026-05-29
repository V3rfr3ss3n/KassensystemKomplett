package de.mmbbs.kassensystem.ui;

import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.service.ProduktService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class LagerView extends VBox {
    private final ProduktService produktService;
    private final TableView<Produkt> produktListe = new TableView<>();
    private final TextField mengeField = new TextField();
    private final Label statusLabel = new Label();

    public LagerView(ProduktService produktService) {
        this.produktService = produktService;

        setSpacing(10);
        setPadding(new Insets(16));

        Label title = new Label("Lagerbestand");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label overview = new Label("Übersicht der aktuellen Lagerstände");
        overview.setStyle("-fx-font-weight: bold;");

        Label hint = new Label("Wählen Sie ein Produkt aus der Tabelle aus und buchen Sie einen Warenzugang direkt hier.");
        hint.setWrapText(true);

        Label summary = new Label("Produkte: " + produktService.alleProdukte().size());
        summary.setStyle("-fx-padding: 4 0 0 0;");

        produktListe.setPrefHeight(220);
        TableColumn<Produkt, Number> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(data -> data.getValue().getIdProperty());
        TableColumn<Produkt, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(data -> data.getValue().nameProperty());
        TableColumn<Produkt, Number> preisColumn = new TableColumn<>("Preis");
        preisColumn.setCellValueFactory(data -> data.getValue().preisProperty());
        TableColumn<Produkt, Number> lagerColumn = new TableColumn<>("Lager");
        lagerColumn.setCellValueFactory(data -> data.getValue().lagerbestandProperty());
        produktListe.getColumns().setAll(idColumn, nameColumn, preisColumn, lagerColumn);
        aktualisiereTabelle();

        Button refreshButton = new Button("Aktualisieren");
        refreshButton.setOnAction(event -> {
            aktualisiereTabelle();
            summary.setText("Produkte: " + produktService.alleProdukte().size());
        });

        Button zugangButton = new Button("Warenzugang buchen");
        zugangButton.setOnAction(event -> bucheWarenzugang());

        HBox controls = new HBox(10, new Label("Menge:"), mengeField, zugangButton, refreshButton);

        getChildren().addAll(title, hint, overview, summary, produktListe, controls, statusLabel);
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
            aktualisiereTabelle();
            statusLabel.setText("Warenzugang wurde gespeichert.");
            mengeField.clear();
        } catch (IllegalArgumentException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }

    private void aktualisiereTabelle() {
        produktListe.setItems(FXCollections.observableArrayList(produktService.alleProdukte()));
    }
}
