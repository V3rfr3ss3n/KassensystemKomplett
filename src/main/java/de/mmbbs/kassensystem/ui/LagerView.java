package de.mmbbs.kassensystem.ui;

import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.service.ProduktService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.Locale;

public class LagerView extends VBox {
    private final ProduktService produktService;
    private final TableView<Produkt> produktListe = new TableView<>();
    private final TextField mengeField = new TextField();
    private final Label statusLabel = new Label();
    private final Label summaryLabel = new Label("Produkte: 0");

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

        summaryLabel.setText("Produkte: " + produktService.alleProdukte().size());
        summaryLabel.setStyle("-fx-padding: 4 0 0 0;");

        Label selectedLabel = new Label("Ausgewählt: nichts");
        selectedLabel.setStyle("-fx-text-fill: #334155;");

        produktListe.setPrefHeight(220);
        TableColumn<Produkt, Number> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(data -> data.getValue().getIdProperty());
        TableColumn<Produkt, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(data -> data.getValue().nameProperty());
        TableColumn<Produkt, Number> preisColumn = new TableColumn<>("Preis");
        preisColumn.setCellValueFactory(data -> data.getValue().preisProperty());
        TableColumn<Produkt, Number> lagerColumn = new TableColumn<>("Lager");
        lagerColumn.setCellValueFactory(data -> data.getValue().lagerbestandProperty());
        TableColumn<Produkt, String> actionColumn = new TableColumn<>("✎");
        actionColumn.setPrefWidth(60);
        actionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button editCellButton = new Button("✎");

            {
                editCellButton.setOnAction(event -> {
                    Produkt produkt = getTableView().getItems().get(getIndex());
                    bearbeiteProdukt(produkt);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : editCellButton);
            }
        });

        produktListe.getColumns().setAll(idColumn, nameColumn, preisColumn, lagerColumn, actionColumn);
        produktListe.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Produkt produkt = produktListe.getSelectionModel().getSelectedItem();
                if (produkt != null) {
                    bearbeiteProdukt(produkt);
                }
            }
        });
        aktualisiereTabelle();

        Button zugangButton = new Button("Warenzugang buchen");
        zugangButton.setOnAction(event -> bucheWarenzugang());

        Button editButton = new Button("Produkt bearbeiten");
        editButton.setOnAction(event -> bearbeiteProdukt());

        Button deleteButton = new Button("Produkt löschen");
        deleteButton.setOnAction(event -> loescheProdukt());

        HBox controls = new HBox(10, new Label("Menge:"), mengeField, zugangButton, editButton, deleteButton);

        produktListe.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected == null) {
                selectedLabel.setText("Ausgewählt: nichts");
            } else {
                selectedLabel.setText("Ausgewählt: " + selected.getName() + " · Lager: " + selected.getLagerbestand());
            }
        });

        getChildren().addAll(title, hint, overview, summaryLabel, selectedLabel, produktListe, controls, statusLabel);
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

    private void bearbeiteProdukt() {
        Produkt produkt = produktListe.getSelectionModel().getSelectedItem();
        bearbeiteProdukt(produkt);
    }

    private void bearbeiteProdukt(Produkt produkt) {
        if (produkt == null) {
            statusLabel.setText("Bitte Produkt auswählen.");
            return;
        }

        TextField nameField = new TextField(produkt.getName());
        TextField preisField = new TextField(String.format(Locale.GERMAN, "%.2f", produkt.getPreis()));
        TextField bestandField = new TextField(String.valueOf(produkt.getLagerbestand()));

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Preis in €:"), 0, 1);
        grid.add(preisField, 1, 1);
        grid.add(new Label("Lagerbestand:"), 0, 2);
        grid.add(bestandField, 1, 2);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Produkt bearbeiten");
        dialog.setHeaderText("Ändern Sie Name, Preis und Lagerbestand.");
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    String name = nameField.getText().trim();
                    double preis = Double.parseDouble(preisField.getText().replace(',', '.'));
                    int bestand = Integer.parseInt(bestandField.getText().trim());

                    produktService.produktAktualisieren(produkt.getId(), name, preis, bestand);
                    aktualisiereTabelle();
                    statusLabel.setText("Produkt wurde aktualisiert.");
                } catch (NumberFormatException ex) {
                    statusLabel.setText("Bitte gültige Zahlen für Preis und Bestand eingeben.");
                } catch (IllegalArgumentException ex) {
                    statusLabel.setText(ex.getMessage());
                }
            }
        });
    }

    private void loescheProdukt() {
        Produkt produkt = produktListe.getSelectionModel().getSelectedItem();
        if (produkt == null) {
            statusLabel.setText("Bitte Produkt auswählen.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Produkt löschen");
        confirmation.setHeaderText("Produkt wirklich löschen?");
        confirmation.setContentText("Das Produkt '" + produkt.getName() + "' wird aus dem Lager entfernt.");

        confirmation.showAndWait().ifPresent(result -> {
            if (result == javafx.scene.control.ButtonType.OK) {
                produktService.produktLoeschen(produkt.getId());
                aktualisiereTabelle();
                summaryLabel.setText("Produkte: " + produktService.alleProdukte().size());
                statusLabel.setText("Produkt wurde entfernt.");
            }
        });
    }

    private void aktualisiereTabelle() {
        produktListe.setItems(FXCollections.observableArrayList(produktService.alleProdukte()));
        summaryLabel.setText("Produkte: " + produktService.alleProdukte().size());
    }
}
