package de.mmbbs.kassensystem.ui;

import de.mmbbs.kassensystem.service.ProduktService;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class ProduktFormView extends VBox {
    private final ProduktService produktService;
    private final TextField nameField = new TextField();
    private final TextField preisField = new TextField();
    private final TextField bestandField = new TextField();
    private final Label statusLabel = new Label();

    public ProduktFormView(ProduktService produktService) {
        this.produktService = produktService;

        setSpacing(10);
        setPadding(new Insets(16));

        Label title = new Label("Produkt hinzufügen");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button saveButton = new Button("Produkt speichern");
        saveButton.setOnAction(event -> speichern());

        getChildren().addAll(
                title,
                new Label("Name:"), nameField,
                new Label("Preis:"), preisField,
                new Label("Anfangsbestand:"), bestandField,
                saveButton,
                statusLabel
        );
    }

    private void speichern() {
        try {
            String name = nameField.getText();
            double preis = Double.parseDouble(preisField.getText().trim());
            int bestand = Integer.parseInt(bestandField.getText().trim());

            produktService.produktHinzufuegen(name, preis, bestand);
            statusLabel.setText("Produkt wurde gespeichert.");
            nameField.clear();
            preisField.clear();
            bestandField.clear();
        } catch (NumberFormatException ex) {
            statusLabel.setText("Bitte gültige Zahlen für Preis und Bestand eingeben.");
        } catch (IllegalArgumentException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }
}
