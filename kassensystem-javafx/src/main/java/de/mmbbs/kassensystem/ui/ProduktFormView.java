package de.mmbbs.kassensystem.ui;

import de.mmbbs.kassensystem.service.ProduktService;
import de.mmbbs.kassensystem.util.ImageUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;

public class ProduktFormView extends VBox {
    private final ProduktService produktService;
    private final TextField nameField = new TextField();
    private final TextField preisField = new TextField();
    private final TextField bestandField = new TextField();
    private final TextField bildPfadField = new TextField();
    private final ImageView bildVorschau = new ImageView();
    private final Label statusLabel = new Label();
    private String gewaehlterBildPfad;

    public ProduktFormView(ProduktService produktService) {
        this.produktService = produktService;

        setSpacing(0);
        setPadding(new Insets(40));
        setAlignment(Pos.TOP_CENTER);

        VBox mainContainer = new VBox();
        mainContainer.getStyleClass().add("main-container");
        mainContainer.setSpacing(20);
        mainContainer.setMaxWidth(900);

        Label title = new Label("Produkt hinzufügen");
        title.getStyleClass().add("title-label");

        Label hint = new Label("Geben Sie Name, Preis, Startbestand und optional ein Produktbild ein. Ohne Bild fällt das Produkt auf einen Fallback zurück.");
        hint.getStyleClass().add("subtitle-label");
        hint.setWrapText(true);

        Label formLabel = new Label("Produktdaten");
        formLabel.getStyleClass().add("section-label");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(10, 0, 0, 0));

        Label nameLabel = new Label("Name:");
        nameLabel.getStyleClass().add("subtitle-label");
        grid.add(nameLabel, 0, 0);
        grid.add(nameField, 1, 0);

        Label preisLabel = new Label("Preis in €:");
        preisLabel.getStyleClass().add("subtitle-label");
        grid.add(preisLabel, 0, 1);
        grid.add(preisField, 1, 1);

        Label bestandLabel = new Label("Anfangsbestand:");
        bestandLabel.getStyleClass().add("subtitle-label");
        grid.add(bestandLabel, 0, 2);
        grid.add(bestandField, 1, 2);

        Label bildLabel = new Label("Produktbild:");
        bildLabel.getStyleClass().add("subtitle-label");

        bildVorschau.setFitWidth(60);
        bildVorschau.setFitHeight(60);
        bildVorschau.setPreserveRatio(true);

        bildPfadField.setPromptText("Pfad zum Produktbild");
        bildPfadField.setEditable(false);

        Button bildButton = new Button("Bild auswählen");
        bildButton.getStyleClass().add("secondary-button");
        bildButton.setOnAction(event -> waehleBild());

        HBox bildBox = new HBox(8, bildPfadField, bildButton, bildVorschau);
        bildBox.setAlignment(Pos.CENTER_LEFT);

        grid.add(bildLabel, 0, 3);
        grid.add(bildBox, 1, 3);

        Button saveButton = new Button("Produkt speichern");
        saveButton.getStyleClass().add("primary-button");
        saveButton.setOnAction(event -> speichern());

        HBox actions = new HBox(10, saveButton);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(10, 0, 0, 0));

        statusLabel.getStyleClass().add("subtitle-label");

        mainContainer.getChildren().addAll(
                title,
                hint,
                formLabel,
                grid,
                actions,
                statusLabel
        );

        getChildren().add(mainContainer);
    }

    private void waehleBild() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Produktbild auswählen");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Bilder", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );

        File datei = chooser.showOpenDialog(getScene().getWindow());
        if (datei != null) {
            gewaehlterBildPfad = datei.getAbsolutePath();
            bildPfadField.setText(gewaehlterBildPfad);
            aktualisiereBildVorschau(gewaehlterBildPfad);
        }
    }

    private void aktualisiereBildVorschau(String bildPfad) {
        bildVorschau.setImage(ImageUtil.loadProductImage(bildPfad));
    }

    private void speichern() {
        try {
            String name = nameField.getText();
            double preis = Double.parseDouble(preisField.getText().trim());
            int bestand = Integer.parseInt(bestandField.getText().trim());

            produktService.produktHinzufuegen(name, preis, bestand, gewaehlterBildPfad);
            statusLabel.setText("Produkt wurde gespeichert. Es ist jetzt im Lager und in der Produktübersicht sichtbar.");
            nameField.clear();
            preisField.clear();
            bestandField.clear();
            bildPfadField.clear();
            bildVorschau.setImage(null);
            gewaehlterBildPfad = null;
        } catch (NumberFormatException ex) {
            statusLabel.setText("Bitte gültige Zahlen für Preis und Bestand eingeben.");
        } catch (IllegalArgumentException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }
}
