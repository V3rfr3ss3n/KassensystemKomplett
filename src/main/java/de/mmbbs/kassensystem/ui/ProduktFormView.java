package de.mmbbs.kassensystem.ui;

import de.mmbbs.kassensystem.service.ProduktService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
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
    private final Label statusLabel = new Label();
    private String gewaehlterBildPfad;

    public ProduktFormView(ProduktService produktService) {
        this.produktService = produktService;

        setSpacing(10);
        setPadding(new Insets(16));

        Label title = new Label("Produkt hinzufügen");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label hint = new Label("Geben Sie Name, Preis, Startbestand und optional ein Produktbild ein. Ohne Bild fällt das Produkt auf einen Fallback zurück.");
        hint.setWrapText(true);

        Button bildButton = new Button("Bild auswählen");
        bildButton.setOnAction(event -> waehleBild());

        HBox bildBox = new HBox(8, bildPfadField, bildButton);
        bildPfadField.setPromptText("Pfad zum Produktbild");
        bildPfadField.setEditable(false);

        Button saveButton = new Button("Produkt speichern");
        saveButton.setOnAction(event -> speichern());

        getChildren().addAll(
                title,
                hint,
                new Label("Name:"), nameField,
                new Label("Preis in €:"), preisField,
                new Label("Anfangsbestand:"), bestandField,
                new Label("Produktbild:"), bildBox,
                saveButton,
                statusLabel
        );
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
        }
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
            gewaehlterBildPfad = null;
        } catch (NumberFormatException ex) {
            statusLabel.setText("Bitte gültige Zahlen für Preis und Bestand eingeben.");
        } catch (IllegalArgumentException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }
}
