package de.mmbbs.kassensystem.ui;

import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.service.ProduktService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class MainView extends VBox {
    private final ProduktService produktService;
    private final Label valueLabel = new Label("Gesamtwert im Lager: 0,00 €");

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

        Label hint = new Label("Die Übersicht zeigt sofort an, wie viele Produkte aktuell verfügbar sind.");
        hint.setWrapText(true);

        Label summaryLabel = new Label("Produkte insgesamt: 0");
        Label stockLabel = new Label("Gesamtbestand: 0");

        TableView<Produkt> produktListe = new TableView<>();
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
        aktualisiereTable(produktListe, summaryLabel, stockLabel, produktService);

        Button refreshButton = new Button("Produkte aktualisieren");
        refreshButton.setOnAction(event -> aktualisiereTable(produktListe, summaryLabel, stockLabel, produktService));

        Button demoButton = new Button("MVP-Status anzeigen");
        demoButton.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("MVP-Status");
            alert.setHeaderText("Grundbau und Hauptfunktionen sind eingebunden");
            alert.setContentText("Die aktuelle Version enthält Modell-, Repository-, Service- und UI-Bausteine für Kasse, Lager und Produktanlegen.");
            alert.showAndWait();
        });

        HBox summaryBox = new HBox(16, summaryLabel, stockLabel, valueLabel);
        summaryBox.setStyle("-fx-padding: 4 0 0 0;");

        HBox actions = new HBox(10, refreshButton, demoButton);

        getChildren().addAll(title, info, hint, overview, summaryBox, produktListe, actions);
    }

    private void aktualisiereTable(TableView<Produkt> table, Label summaryLabel, Label stockLabel, ProduktService produktService) {
        table.setItems(FXCollections.observableArrayList(produktService.alleProdukte()));
        summaryLabel.setText("Produkte insgesamt: " + produktService.alleProdukte().size());
        int gesamtbestand = produktService.alleProdukte().stream().mapToInt(Produkt::getLagerbestand).sum();
        double gesamtwert = produktService.alleProdukte().stream()
                .mapToDouble(produkt -> produkt.getPreis() * produkt.getLagerbestand())
                .sum();
        stockLabel.setText("Gesamtbestand: " + gesamtbestand);
        valueLabel.setText("Gesamtwert im Lager: " + String.format("%.2f €", gesamtwert));
    }
}
