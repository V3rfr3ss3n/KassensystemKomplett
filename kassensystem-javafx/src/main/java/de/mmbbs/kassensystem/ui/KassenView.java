package de.mmbbs.kassensystem.ui;

import de.mmbbs.kassensystem.model.Bon;
import de.mmbbs.kassensystem.model.BonPosition;
import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.service.BonService;
import de.mmbbs.kassensystem.service.KassenService;
import de.mmbbs.kassensystem.service.ProduktService;
import de.mmbbs.kassensystem.util.GeldFormatter;
import de.mmbbs.kassensystem.util.ImageUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class KassenView extends VBox {
    private final ProduktService produktService;
    private final KassenService kassenService;

    private final FlowPane produktGrid = new FlowPane();
    private final ListView<BonPosition> warenkorbListe = new ListView<>();
    private final Label gesamtPreisLabel = new Label("Gesamtpreis: " + GeldFormatter.formatiereBetrag(0));
    private final TextField mengeField = new TextField();
    private final TextArea bonArea = new TextArea();
    private final Label statusLabel = new Label();
    private final Button checkoutButton = new Button("Kauf abschließen");

    private Produkt selectedProdukt = null;

    public KassenView(ProduktService produktService, KassenService kassenService) {
        this.produktService = produktService;
        this.kassenService = kassenService;

        // Listener registrieren für automatische Aktualisierung
        produktService.addListener(() -> aktualisiereProduktGrid());

        // root setup
        setSpacing(0);
        setPadding(new Insets(40));
        setAlignment(Pos.TOP_CENTER);

        // Main Container (the white card in the screenshot)
        VBox mainContainer = new VBox();
        mainContainer.getStyleClass().add("main-container");
        mainContainer.setSpacing(20);
        mainContainer.setMaxWidth(900);

        // Header
        Label title = new Label("Kasse");
        title.getStyleClass().add("title-label");

        Label hint = new Label("Wählen Sie ein Produkt, geben Sie eine Menge ein und legen Sie es in den Warenkorb.");
        hint.getStyleClass().add("subtitle-label");
        hint.setWrapText(true);

        // Products Section
        Label prodLabel = new Label("Produkte:");
        prodLabel.getStyleClass().add("section-label");

        produktGrid.setHgap(15);
        produktGrid.setVgap(15);
        produktGrid.setPadding(new Insets(10, 0, 20, 0));
        aktualisiereProduktGrid();

        // Controls Section
        HBox controls = new HBox(15);
        controls.setAlignment(Pos.CENTER_LEFT);

        Label mengeLabel = new Label("Menge:");
        mengeLabel.getStyleClass().add("subtitle-label");

        mengeField.setPrefWidth(150);

        Button addButton = new Button("Zum Warenkorb hinzufügen");
        addButton.getStyleClass().add("primary-button");
        addButton.setOnAction(event -> hinzufuegen());

        checkoutButton.getStyleClass().add("secondary-button");
        checkoutButton.setOnAction(event -> abschliessen());
        checkoutButton.setDisable(true);

        Button clearButton = new Button("Warenkorb leeren");
        clearButton.getStyleClass().add("primary-button");
        clearButton.setOnAction(event -> {
            kassenService.warenkorbLeeren();
            aktualisiereWarenkorb();
            statusLabel.setText("Warenkorb wurde geleert.");
        });

        controls.getChildren().addAll(mengeLabel, mengeField, addButton, checkoutButton, clearButton);

        // Cart Section
        Label cartLabel = new Label("Warenkorb");
        cartLabel.getStyleClass().add("section-label");

        warenkorbListe.setPrefHeight(150);
        warenkorbListe.getStyleClass().add("cart-area");

        gesamtPreisLabel.getStyleClass().add("section-label");
        gesamtPreisLabel.setText("Gesamtpreis: " + GeldFormatter.formatiereBetrag(0));

        // Bottom Info Box
        VBox infoBox = new VBox();
        infoBox.getStyleClass().add("info-box");

        bonArea.setEditable(false);
        bonArea.setPrefHeight(80);
        bonArea.setPromptText("Der erzeugte Bon erscheint hier nach dem Kaufabschluss.");
        bonArea.getStyleClass().add("info-box-text");
        bonArea.setWrapText(true);

        infoBox.getChildren().add(bonArea);

        // Assemble Main Container
        mainContainer.getChildren().addAll(
            title,
            hint,
            prodLabel,
            produktGrid,
            controls,
            cartLabel,
            warenkorbListe,
            gesamtPreisLabel,
            infoBox,
            statusLabel
        );

        getChildren().add(mainContainer);
        aktualisiereWarenkorb();
    }

    private void aktualisiereProduktGrid() {
        produktGrid.getChildren().clear();
        for (Produkt p : produktService.alleProdukte()) {
            produktGrid.getChildren().add(createProductCard(p));
        }
    }

    private VBox createProductCard(Produkt p) {
        VBox card = new VBox(10);
        card.getStyleClass().add("product-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefWidth(220);

        HBox content = new HBox(15);
        content.setAlignment(Pos.CENTER_LEFT);

        // Image
        ImageView imageView = new ImageView();
        imageView.setFitHeight(40);
        imageView.setFitWidth(40);
        imageView.setPreserveRatio(true);
        imageView.setImage(ImageUtil.loadProductImage(p.getBildPfad()));

        // Text
        VBox textContainer = new VBox(2);
        Label nameLabel = new Label(p.getName());
        nameLabel.getStyleClass().add("product-card-name");

        Label detailsLabel = new Label("Lager: " + p.getLagerbestand() + " - " + GeldFormatter.formatiereBetrag(p.getPreis()));
        detailsLabel.getStyleClass().add("product-card-details");

        textContainer.getChildren().addAll(nameLabel, detailsLabel);
        content.getChildren().addAll(imageView, textContainer);

        card.getChildren().add(content);

        card.setOnMouseClicked(event -> {
            selectedProdukt = p;
            // Visual update for selection
            produktGrid.getChildren().forEach(node -> {
                if (node instanceof VBox) {
                    ((VBox) node).getStyleClass().remove("product-card-selected");
                }
            });
            card.getStyleClass().add("product-card-selected");
        });

        return card;
    }

    private void hinzufuegen() {
        if (selectedProdukt == null) {
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
            kassenService.positionHinzufuegen(selectedProdukt.getId(), menge);
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
            BonService bonService = new BonService();
            bonArea.setText(bonService.formatiereBon(bon));
            aktualisiereWarenkorb();

            AlertUtil.showInfo("Kauf erfolgreich abgeschlossen",
                    "Bon Nr. " + bon.getBonnummer() + "\nGesamtpreis: " + GeldFormatter.formatiereBetrag(bon.getGesamtpreis()));

            statusLabel.setText("Kauf abgeschlossen. Lagerbestand wurde aktualisiert.");
            aktualisiereProduktGrid();
        } catch (IllegalArgumentException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }

    private void aktualisiereWarenkorb() {
        warenkorbListe.getItems().setAll(kassenService.getWarenkorb());
        gesamtPreisLabel.setText("Gesamtpreis: " + GeldFormatter.formatiereBetrag(kassenService.berechneGesamtpreis()));
        checkoutButton.setDisable(kassenService.getWarenkorb().isEmpty());
    }
}
