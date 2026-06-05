package de.mmbbs.kassensystem.ui;

import de.mmbbs.kassensystem.model.Bon;
import de.mmbbs.kassensystem.model.BonPosition;
import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.model.Steuersatz;
import de.mmbbs.kassensystem.model.Verkaufseinheit;
import de.mmbbs.kassensystem.service.BonService;
import de.mmbbs.kassensystem.service.KassenService;
import de.mmbbs.kassensystem.service.ProduktService;
import de.mmbbs.kassensystem.util.GeldFormatter;
import de.mmbbs.kassensystem.util.ImageUtil;
import de.mmbbs.kassensystem.util.MengenFormatter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.time.format.DateTimeFormatter;

public class KassenView extends VBox {
    private static final DateTimeFormatter BON_HISTORIE_FORMAT = DateTimeFormatter.ofPattern("dd.MM. HH:mm");
    private final ProduktService produktService;
    private final KassenService kassenService;
    private final BonService bonFormatService = new BonService();

    private final FlowPane produktGrid = new FlowPane();
    private final ListView<BonPosition> warenkorbListe = new ListView<>();
    private final ListView<Bon> bonHistorieListe = new ListView<>();
    private final Label gesamtPreisLabel = new Label("Gesamtpreis: " + GeldFormatter.formatiereBetrag(0));
    private final TextField produktSucheField = new TextField();
    private final ComboBox<String> einheitFilterBox = new ComboBox<>();
    private final ComboBox<String> steuerFilterBox = new ComboBox<>();
    private final TextField preisVonField = new TextField();
    private final TextField preisBisField = new TextField();
    private final CheckBox nurVerfuegbarCheck = new CheckBox("Nur verfügbar");
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
        mainContainer.setMaxWidth(1180);

        // Header
        Label title = new Label("Kasse");
        title.getStyleClass().add("title-label");

        Label hint = new Label("Wählen Sie ein Produkt, geben Sie eine Menge ein und legen Sie es in den Warenkorb.");
        hint.getStyleClass().add("subtitle-label");
        hint.setWrapText(true);

        // Products Section
        Label prodLabel = new Label("Produkte:");
        prodLabel.getStyleClass().add("section-label");

        produktSucheField.setPromptText("Produkt suchen...");
        produktSucheField.textProperty().addListener((obs, oldValue, newValue) -> aktualisiereProduktGrid());
        Button filterButton = new Button("Filter");
        filterButton.getStyleClass().add("secondary-button");
        Button clearFilterButton = new Button("x");
        clearFilterButton.getStyleClass().add("secondary-button");
        clearFilterButton.setOnAction(event -> setzeProduktFilterZurueck());
        Button refreshButton = new Button("Aktualisieren");
        refreshButton.getStyleClass().add("secondary-button");
        refreshButton.setOnAction(event -> {
            aktualisiereProduktGrid();
            aktualisiereBonHistorie();
            statusLabel.setText("Daten aktualisiert.");
        });
        HBox filterBar = new HBox(8, produktSucheField, filterButton, refreshButton, clearFilterButton);
        HBox.setHgrow(produktSucheField, Priority.ALWAYS);

        VBox erweiterteFilter = erstelleErweiterteFilter();
        erweiterteFilter.setVisible(false);
        erweiterteFilter.setManaged(false);
        filterButton.setOnAction(event -> {
            boolean sichtbar = !erweiterteFilter.isVisible();
            erweiterteFilter.setVisible(sichtbar);
            erweiterteFilter.setManaged(sichtbar);
        });

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

        checkoutButton.getStyleClass().add("success-button");
        checkoutButton.setOnAction(event -> abschliessen());
        checkoutButton.setDisable(true);

        Button clearButton = new Button("Warenkorb leeren");
        clearButton.getStyleClass().add("primary-button");
        clearButton.setOnAction(event -> {
            kassenService.warenkorbLeeren();
            aktualisiereWarenkorb();
            statusLabel.setText("Warenkorb wurde geleert.");
        });

        controls.getChildren().addAll(mengeLabel, mengeField, addButton, clearButton);

        // Cart Section
        Label cartLabel = new Label("Warenkorb");
        cartLabel.getStyleClass().add("section-label");

        warenkorbListe.setPrefHeight(150);
        warenkorbListe.getStyleClass().add("cart-area");
        warenkorbListe.setCellFactory(list -> new WarenkorbCell());

        gesamtPreisLabel.getStyleClass().add("section-label");
        gesamtPreisLabel.setText("Gesamtpreis: " + GeldFormatter.formatiereBetrag(0));

        HBox checkoutRow = new HBox(checkoutButton);
        checkoutRow.setAlignment(Pos.CENTER_RIGHT);
        checkoutButton.setPrefWidth(220);
        checkoutButton.setPrefHeight(44);

        // Bottom Info Box
        VBox infoBox = new VBox();
        infoBox.getStyleClass().add("info-box");

        bonArea.setEditable(false);
        bonArea.setPrefHeight(340);
        bonArea.setPrefWidth(360);
        bonArea.setPromptText("Der erzeugte Bon erscheint hier nach dem Kaufabschluss.");
        bonArea.getStyleClass().add("info-box-text");
        bonArea.setWrapText(false);
        bonArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace;");

        infoBox.getChildren().add(bonArea);

        Label bonLabel = new Label("Bon");
        bonLabel.getStyleClass().add("section-label");

        Label historieLabel = new Label("Bon-Historie");
        historieLabel.getStyleClass().add("section-label");
        bonHistorieListe.setPrefHeight(170);
        bonHistorieListe.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Bon bon, boolean empty) {
                super.updateItem(bon, empty);
                if (empty || bon == null) {
                    setText(null);
                    return;
                }
                setText("Bon " + bon.getBonnummer() + " · "
                        + BON_HISTORIE_FORMAT.format(bon.getDatumUhrzeit()) + " · "
                        + GeldFormatter.formatiereBetrag(bon.getGesamtpreis()));
            }
        });
        bonHistorieListe.getSelectionModel().selectedItemProperty().addListener((obs, oldBon, bon) -> {
            if (bon != null) {
                bonArea.setText(bonFormatService.formatiereBon(bon));
            }
        });

        VBox leftContent = new VBox(16, prodLabel, filterBar, erweiterteFilter, produktGrid, controls,
                cartLabel, warenkorbListe, gesamtPreisLabel, checkoutRow);
        leftContent.setPrefWidth(720);
        HBox.setHgrow(leftContent, Priority.ALWAYS);

        VBox rightContent = new VBox(10, bonLabel, infoBox, historieLabel, bonHistorieListe);
        rightContent.setPrefWidth(390);

        HBox arbeitsbereich = new HBox(24, leftContent, rightContent);
        arbeitsbereich.setAlignment(Pos.TOP_LEFT);

        // Assemble Main Container
        mainContainer.getChildren().addAll(
            title,
            hint,
            arbeitsbereich,
            statusLabel
        );

        getChildren().add(mainContainer);
        aktualisiereWarenkorb();
        aktualisiereBonHistorie();
    }

    private void aktualisiereProduktGrid() {
        produktGrid.getChildren().clear();
        for (Produkt p : produktService.alleProdukte()) {
            if (matchesProduktSuche(p)) {
                produktGrid.getChildren().add(createProductCard(p));
            }
        }
    }

    private boolean matchesProduktSuche(Produkt produkt) {
        String suche = produktSucheField.getText();
        if (suche != null && !suche.isBlank()) {
            String text = (produkt.getId() + " " + produkt.getName() + " "
                    + produkt.getEinheitLabel() + " "
                    + GeldFormatter.formatiereBetrag(produkt.getPreis())).toLowerCase();
            if (!text.contains(suche.toLowerCase().trim())) {
                return false;
            }
        }

        String einheitFilter = einheitFilterBox.getValue();
        if (einheitFilter != null && !"Alle Einheiten".equals(einheitFilter)
                && !produkt.getEinheitLabel().equals(einheitFilter)) {
            return false;
        }

        String steuerFilter = steuerFilterBox.getValue();
        if (steuerFilter != null && !"Alle Steuersätze".equals(steuerFilter)
                && !String.format("%.0f %%", produkt.getSteuerSatz()).equals(steuerFilter)) {
            return false;
        }

        double preisVon = parseOptionalDouble(preisVonField.getText());
        double preisBis = parseOptionalDouble(preisBisField.getText());
        if (!Double.isNaN(preisVon) && produkt.getPreis() < preisVon) {
            return false;
        }
        if (!Double.isNaN(preisBis) && produkt.getPreis() > preisBis) {
            return false;
        }

        return !nurVerfuegbarCheck.isSelected() || produkt.getLagerbestand() > 0;
    }

    private VBox createProductCard(Produkt p) {
        VBox card = new VBox(10);
        card.getStyleClass().add("product-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefWidth(260);

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

        Label detailsLabel = new Label("Lager: " + MengenFormatter.formatiereMenge(p.getLagerbestand(), p.getEinheitLabel())
                + " · " + GeldFormatter.formatiereBetrag(p.getPreis()) + "/" + p.getEinheitLabel()
                + " · USt " + String.format("%.0f %%", p.getSteuerSatz()));
        detailsLabel.getStyleClass().add("product-card-details");

        textContainer.getChildren().addAll(nameLabel, detailsLabel);
        HBox.setHgrow(textContainer, Priority.ALWAYS);

        Button plusButton = new Button("+");
        plusButton.getStyleClass().add("primary-button");
        plusButton.setOnAction(event -> {
            event.consume();
            fuegeDirektHinzu(p);
        });

        Label badge = new Label();
        badge.getStyleClass().add("cart-badge");
        double mengeImWarenkorb = mengeImWarenkorb(p);
        badge.setText(MengenFormatter.formatiereMenge(mengeImWarenkorb));
        badge.setVisible(mengeImWarenkorb > 0);
        badge.setManaged(mengeImWarenkorb > 0);

        StackPane plusWrapper = new StackPane(plusButton, badge);
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        plusWrapper.setPrefSize(48, 38);

        content.getChildren().addAll(imageView, textContainer, plusWrapper);

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
            if (event.getClickCount() == 2) {
                zeigeProduktDetails(p);
            }
        });

        return card;
    }

    private void hinzufuegen() {
        if (selectedProdukt == null) {
            statusLabel.setText("Bitte Produkt auswählen.");
            return;
        }

        double menge;
        try {
            menge = MengenFormatter.parseMenge(mengeField.getText());
        } catch (NumberFormatException ex) {
            statusLabel.setText("Menge muss eine Zahl größer als 0 sein.");
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
            bonArea.setText(bonFormatService.formatiereBon(bon));
            aktualisiereWarenkorb();
            aktualisiereBonHistorie();

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
        aktualisiereProduktGrid();
    }

    private double mengeImWarenkorb(Produkt produkt) {
        return kassenService.getWarenkorb().stream()
                .filter(position -> position.getProdukt().getId() == produkt.getId())
                .mapToDouble(BonPosition::getMenge)
                .sum();
    }

    private void fuegeDirektHinzu(Produkt produkt) {
        try {
            kassenService.positionHinzufuegen(produkt.getId(), 1.0);
            aktualisiereWarenkorb();
            statusLabel.setText(produkt.getName() + " wurde hinzugefügt.");
        } catch (IllegalArgumentException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }

    private void aktualisiereBonHistorie() {
        bonHistorieListe.getItems().setAll(kassenService.getBonHistorie());
    }

    private VBox erstelleErweiterteFilter() {
        einheitFilterBox.getItems().setAll("Alle Einheiten",
                Verkaufseinheit.STUECK.getLabel(),
                Verkaufseinheit.KILOGRAMM.getLabel(),
                Verkaufseinheit.LITER.getLabel(),
                Verkaufseinheit.PACKUNG.getLabel());
        einheitFilterBox.getSelectionModel().select("Alle Einheiten");

        steuerFilterBox.getItems().setAll("Alle Steuersätze",
                String.format("%.0f %%", Steuersatz.REGELSTEUERSATZ.getProzent()),
                String.format("%.0f %%", Steuersatz.ERMAESSIGT.getProzent()));
        steuerFilterBox.getSelectionModel().select("Alle Steuersätze");

        preisVonField.setPromptText("Preis ab");
        preisVonField.setPrefWidth(90);
        preisBisField.setPromptText("Preis bis");
        preisBisField.setPrefWidth(90);

        Button resetButton = new Button("Zurücksetzen");
        resetButton.getStyleClass().add("secondary-button");
        resetButton.setOnAction(event -> setzeProduktFilterZurueck());

        einheitFilterBox.setOnAction(event -> aktualisiereProduktGrid());
        steuerFilterBox.setOnAction(event -> aktualisiereProduktGrid());
        preisVonField.textProperty().addListener((obs, oldValue, newValue) -> aktualisiereProduktGrid());
        preisBisField.textProperty().addListener((obs, oldValue, newValue) -> aktualisiereProduktGrid());
        nurVerfuegbarCheck.selectedProperty().addListener((obs, oldValue, newValue) -> aktualisiereProduktGrid());

        HBox row = new HBox(8, einheitFilterBox, steuerFilterBox, preisVonField, preisBisField, nurVerfuegbarCheck, resetButton);
        row.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(row);
        box.getStyleClass().add("filter-panel");
        return box;
    }

    private void setzeProduktFilterZurueck() {
        produktSucheField.clear();
        einheitFilterBox.getSelectionModel().select("Alle Einheiten");
        steuerFilterBox.getSelectionModel().select("Alle Steuersätze");
        preisVonField.clear();
        preisBisField.clear();
        nurVerfuegbarCheck.setSelected(false);
        aktualisiereProduktGrid();
    }

    private double parseOptionalDouble(String text) {
        if (text == null || text.isBlank()) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(text.trim().replace(",", "."));
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    private void zeigeProduktDetails(Produkt produkt) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Produktdetails");
        dialog.setHeaderText(produkt.getName());

        ImageView bild = new ImageView(ImageUtil.loadProductImage(produkt.getBildPfad()));
        bild.setFitWidth(180);
        bild.setFitHeight(180);
        bild.setPreserveRatio(true);
        bild.getStyleClass().add("detail-image");

        VBox daten = new VBox(8,
                new Label("Produktnummer: " + produkt.getId()),
                new Label("Preis: " + GeldFormatter.formatiereBetrag(produkt.getPreis()) + "/" + produkt.getEinheitLabel()),
                new Label("Lagerbestand: " + MengenFormatter.formatiereMenge(produkt.getLagerbestand(), produkt.getEinheitLabel())),
                new Label("Umsatzsteuer: " + String.format("%.0f %%", produkt.getSteuerSatz())),
                new Label("Im Warenkorb: " + MengenFormatter.formatiereMenge(mengeImWarenkorb(produkt), produkt.getEinheitLabel()))
        );
        daten.setAlignment(Pos.CENTER_LEFT);

        HBox content = new HBox(20, bild, daten);
        content.setPadding(new Insets(10));
        content.setAlignment(Pos.CENTER_LEFT);

        dialog.getDialogPane().setContent(content);
        ThemeManager.applyToDialogPane(dialog.getDialogPane());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.showAndWait();
    }

    private final class WarenkorbCell extends ListCell<BonPosition> {
        private final Label text = new Label();
        private final Button minusButton = new Button("-");
        private final Button plusButton = new Button("+");
        private final Button removeButton = new Button("Entfernen");
        private final HBox content = new HBox(8, text, minusButton, plusButton, removeButton);

        private WarenkorbCell() {
            text.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(text, Priority.ALWAYS);
            content.setAlignment(Pos.CENTER_LEFT);
            minusButton.getStyleClass().add("secondary-button");
            plusButton.getStyleClass().add("secondary-button");
            removeButton.getStyleClass().add("secondary-button");

            minusButton.setOnAction(event -> {
                BonPosition position = getItem();
                if (position != null) {
                    kassenService.positionVerringern(position);
                    aktualisiereWarenkorb();
                }
            });
            plusButton.setOnAction(event -> {
                BonPosition position = getItem();
                if (position != null) {
                    try {
                        kassenService.positionErhoehen(position);
                        aktualisiereWarenkorb();
                    } catch (IllegalArgumentException ex) {
                        statusLabel.setText(ex.getMessage());
                    }
                }
            });
            removeButton.setOnAction(event -> {
                BonPosition position = getItem();
                if (position != null) {
                    kassenService.positionEntfernen(position);
                    aktualisiereWarenkorb();
                }
            });
        }

        @Override
        protected void updateItem(BonPosition position, boolean empty) {
            super.updateItem(position, empty);
            if (empty || position == null) {
                setGraphic(null);
                return;
            }
            text.setText(position.toString());
            setGraphic(content);
        }
    }
}
