package de.mmbbs.kassensystem.ui;

import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.model.Steuersatz;
import de.mmbbs.kassensystem.model.Verkaufseinheit;
import de.mmbbs.kassensystem.service.ProduktService;
import de.mmbbs.kassensystem.util.GeldFormatter;
import de.mmbbs.kassensystem.util.ImageProcessor;
import de.mmbbs.kassensystem.util.ImageUtil;
import de.mmbbs.kassensystem.util.MengenFormatter;
import de.mmbbs.kassensystem.util.ValidationUtil;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class LagerView extends VBox {
    private final ProduktService produktService;
    private final TableView<Produkt> produktListe = new TableView<>();
    private final TextField mengeField = new TextField();
    private final Label statusLabel = new Label();
    private final Label summaryLabel = new Label("Produkte: 0");
    private final ProduktTableHelper.FilterFields produktFilter;
    private final Runnable produktHinzufuegenAction;

    public LagerView(ProduktService produktService) {
        this(produktService, null);
    }

    public LagerView(ProduktService produktService, Runnable produktHinzufuegenAction) {
        this.produktService = produktService;
        this.produktHinzufuegenAction = produktHinzufuegenAction;
        this.produktFilter = new ProduktTableHelper.FilterFields(this::aktualisiereTabelle);

        // Listener registrieren für automatische Aktualisierung
        produktService.addListener(() -> aktualisiereTabelle());

        setSpacing(0);
        setPadding(new Insets(40));
        setAlignment(Pos.TOP_CENTER);

        VBox mainContainer = new VBox();
        mainContainer.getStyleClass().add("main-container");
        mainContainer.setSpacing(20);
        mainContainer.setMaxWidth(900);

        Label title = new Label("Lagerbestand");
        title.getStyleClass().add("title-label");

        Label hint = new Label("Wählen Sie ein Produkt aus der Tabelle aus und buchen Sie einen Warenzugang direkt hier.");
        hint.getStyleClass().add("subtitle-label");
        hint.setWrapText(true);

        Label overview = new Label("Übersicht der aktuellen Lagerstände");
        overview.getStyleClass().add("section-label");

        summaryLabel.setText("Produkte: " + produktService.alleProdukte().size());
        summaryLabel.getStyleClass().add("subtitle-label");

        Label selectedLabel = new Label("Ausgewählt: nichts");
        selectedLabel.getStyleClass().add("subtitle-label");

        produktListe.setPrefHeight(300);
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

        produktListe.getColumns().setAll(
                ProduktTableHelper.bildColumn(),
                ProduktTableHelper.idColumn(produktFilter),
                ProduktTableHelper.nameColumn(produktFilter),
                ProduktTableHelper.preisColumn(produktFilter),
                ProduktTableHelper.lagerColumn(produktFilter),
                actionColumn
        );
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
        zugangButton.getStyleClass().add("primary-button");
        zugangButton.setOnAction(event -> bucheWarenzugang());

        Button editButton = new Button("Produkt bearbeiten");
        editButton.getStyleClass().add("secondary-button");
        editButton.setOnAction(event -> bearbeiteProdukt());

        Button deleteButton = new Button("Produkt löschen");
        deleteButton.getStyleClass().add("secondary-button");
        deleteButton.setOnAction(event -> loescheProdukt());

        Button addProductButton = new Button("Produkt hinzufügen");
        addProductButton.getStyleClass().add("secondary-button");
        addProductButton.setOnAction(event -> {
            if (produktHinzufuegenAction != null) {
                produktHinzufuegenAction.run();
            }
        });

        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER_LEFT);
        Label mengeLabel = new Label("Menge:");
        mengeLabel.getStyleClass().add("subtitle-label");
        controls.getChildren().addAll(mengeLabel, mengeField, zugangButton, addProductButton, editButton, deleteButton);

        produktListe.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, selected) -> {
            if (selected == null) {
                selectedLabel.setText("Ausgewählt: nichts");
            } else {
                selectedLabel.setText("Ausgewählt: " + selected.getName()
                        + " · Lager: " + MengenFormatter.formatiereMenge(selected.getLagerbestand(), selected.getEinheitLabel()));
            }
        });

        mainContainer.getChildren().addAll(title, hint, overview, summaryLabel, selectedLabel, produktListe, controls, statusLabel);
        getChildren().add(mainContainer);
    }

    private void bucheWarenzugang() {
        Produkt produkt = produktListe.getSelectionModel().getSelectedItem();
        if (produkt == null) {
            statusLabel.setText("❌ Bitte Produkt auswählen.");
            return;
        }

        String mengeStr = mengeField.getText().trim();
        if (!ValidationUtil.isValidQuantity(mengeStr)) {
            statusLabel.setText("❌ " + ValidationUtil.getQuantityError());
            return;
        }

        try {
            double menge = MengenFormatter.parseMenge(mengeStr);
            produktService.warenzugangErfassen(produkt.getId(), menge);
            aktualisiereTabelle();
            statusLabel.setText("✓ Warenzugang gespeichert!");
            mengeField.clear();
        } catch (IllegalArgumentException ex) {
            statusLabel.setText("❌ " + ex.getMessage());
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
        TextField preisField = new TextField(GeldFormatter.formatiereZahl(produkt.getPreis()));
        TextField bestandField = new TextField(MengenFormatter.formatiereMenge(produkt.getLagerbestand()));
        TextField bildPfadField = new TextField(produkt.getBildPfad() != null ? produkt.getBildPfad() : "");
        bildPfadField.setEditable(false);
        ComboBox<Verkaufseinheit> einheitBox = new ComboBox<>();
        einheitBox.getItems().setAll(Verkaufseinheit.values());
        einheitBox.getSelectionModel().select(produkt.getEinheit());
        ComboBox<Steuersatz> steuerBox = new ComboBox<>();
        steuerBox.getItems().setAll(Steuersatz.values());
        steuerBox.getSelectionModel().select(Steuersatz.fromProzent(produkt.getSteuerSatz()));

        javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView();
        imageView.setFitWidth(48);
        imageView.setFitHeight(48);
        imageView.setPreserveRatio(true);
        aktualisiereBildVorschau(produkt.getBildPfad(), imageView);

        Button bildButton = new Button("Bild auswählen");
        bildButton.setOnAction(e -> {
            javafx.stage.FileChooser chooser = new javafx.stage.FileChooser();
            chooser.setTitle("Produktbild auswählen");
            chooser.getExtensionFilters().addAll(
                    new javafx.stage.FileChooser.ExtensionFilter("Bilder", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
            );
            java.io.File datei = chooser.showOpenDialog(getScene().getWindow());
            if (datei != null) {
                ImageCropDialog.show(getScene().getWindow(), datei).ifPresent(cropArea -> {
                    String processedPath = ImageProcessor.processAndSaveImage(datei.getAbsolutePath(), cropArea);
                    if (processedPath != null) {
                        bildPfadField.setText(processedPath);
                        aktualisiereBildVorschau(processedPath, imageView);
                        statusLabel.setText("");
                    } else {
                        statusLabel.setText("Fehler beim Verarbeiten des Bildes. Bitte versuchen Sie es erneut.");
                    }
                });
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Preis in €:"), 0, 1);
        grid.add(preisField, 1, 1);
        grid.add(new Label("Lagerbestand:"), 0, 2);
        grid.add(bestandField, 1, 2);
        grid.add(new Label("Einheit:"), 0, 3);
        grid.add(einheitBox, 1, 3);
        grid.add(new Label("Umsatzsteuer:"), 0, 4);
        grid.add(steuerBox, 1, 4);
        grid.add(new Label("Bildpfad:"), 0, 5);
        grid.add(bildPfadField, 1, 5);
        grid.add(bildButton, 2, 5);
        grid.add(imageView, 3, 5);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Produkt bearbeiten");
        dialog.setHeaderText("Ändern Sie Name, Preis, Lagerbestand und Bild.");
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    String name = nameField.getText().trim();
                    double preis = Double.parseDouble(preisField.getText().replace(',', '.'));
                    double bestand = MengenFormatter.parseMenge(bestandField.getText());
                    String bildPfad = bildPfadField.getText().isBlank() ? null : bildPfadField.getText();
                    Verkaufseinheit einheit = einheitBox.getSelectionModel().getSelectedItem();
                    Steuersatz steuer = steuerBox.getSelectionModel().getSelectedItem();

                    produktService.produktAktualisieren(produkt.getId(), name, preis, bestand, bildPfad,
                            einheit, steuer.getProzent());
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

    private void aktualisiereBildVorschau(String bildPfad, javafx.scene.image.ImageView imageView) {
        imageView.setImage(ImageUtil.loadProductImage(bildPfad));
    }

    private void loescheProdukt() {
        Produkt produkt = produktListe.getSelectionModel().getSelectedItem();
        if (produkt == null) {
            statusLabel.setText("Bitte Produkt auswählen.");
            return;
        }

        boolean bestaetigt = AlertUtil.showConfirmation("Produkt löschen",
                "Das Produkt '" + produkt.getName() + "' wird aus dem Lager entfernt.");
        if (bestaetigt) {
            produktService.produktLoeschen(produkt.getId());
            aktualisiereTabelle();
            summaryLabel.setText("Produkte: " + produktService.alleProdukte().size());
            statusLabel.setText("Produkt wurde entfernt.");
        }
    }

    private void aktualisiereTabelle() {
        produktListe.setItems(FXCollections.observableArrayList(produktService.alleProdukte().stream()
                .filter(produktFilter::matches)
                .toList()));
        summaryLabel.setText("Produkte: " + produktService.alleProdukte().size());
    }
}
