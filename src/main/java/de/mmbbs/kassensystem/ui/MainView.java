package de.mmbbs.kassensystem.ui;

import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.service.ProduktService;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class MainView extends VBox {
    private final ProduktService produktService;

    public MainView(ProduktService produktService) {
        this.produktService = produktService;

        setSpacing(12);
        setPadding(new Insets(16));
        getStyleClass().add("root");

        Label title = new Label("Kassensystem MVP");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label info = new Label("Startseite mit Produktübersicht, Kassenfunktion und Lagerverwaltung.");
        info.setWrapText(true);

        Label overview = new Label("Aktueller Produktbestand");
        overview.setStyle("-fx-font-weight: bold;");

        ListView<Produkt> produktListe = new ListView<>();
        produktListe.getItems().setAll(produktService.alleProdukte());
        produktListe.setPrefHeight(180);

        Button refreshButton = new Button("Produkte aktualisieren");
        refreshButton.setOnAction(event -> produktListe.getItems().setAll(produktService.alleProdukte()));

        Button demoButton = new Button("MVP-Status anzeigen");
        demoButton.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("MVP-Status");
            alert.setHeaderText("Grundbau und Hauptfunktionen sind eingebunden");
            alert.setContentText("Die aktuelle Version enthält Modell-, Repository-, Service- und UI-Bausteine für Kasse, Lager und Produktanlegen.");
            alert.showAndWait();
        });

        HBox actions = new HBox(10, refreshButton, demoButton);

        getChildren().addAll(title, info, overview, produktListe, actions);
    }
}
