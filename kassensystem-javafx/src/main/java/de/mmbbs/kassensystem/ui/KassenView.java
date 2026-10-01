package de.mmbbs.kassensystem.ui;

import de.mmbbs.kassensystem.model.Bon;
import de.mmbbs.kassensystem.model.BonPosition;
import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.model.Steuersatz;
import de.mmbbs.kassensystem.model.Verkaufseinheit;
import de.mmbbs.kassensystem.service.BonService;
import de.mmbbs.kassensystem.service.BonPdfService;
import de.mmbbs.kassensystem.service.KassenService;
import de.mmbbs.kassensystem.service.ProduktService;
import de.mmbbs.kassensystem.util.GeldFormatter;
import de.mmbbs.kassensystem.util.ImageUtil;
import de.mmbbs.kassensystem.util.MengenFormatter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.prefs.Preferences;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

import java.time.format.DateTimeFormatter;
import java.io.File;
import javafx.stage.FileChooser;

public class KassenView extends VBox {
    private static final DateTimeFormatter BON_HISTORIE_FORMAT = DateTimeFormatter.ofPattern("dd.MM. HH:mm");
    private static final int PRODUKTE_PRO_SEITE = 18;
    private static final int SCHNELLPLAETZE = 5;
    private final ProduktService produktService;
    private final KassenService kassenService;
    private final BonService bonFormatService = new BonService();
    private final BonPdfService bonPdfService = new BonPdfService();

    private final FlowPane produktGrid = new FlowPane();
    private final FlowPane schnellauswahl = new FlowPane(8, 8);
    private final StackPane produktOverlay = new StackPane();
    private final Preferences einstellungen = Preferences.userNodeForPackage(KassenView.class);
    private final Set<Integer> favoriten = new LinkedHashSet<>();
    private final Label ergebnisLabel = new Label();
    private final Label seitenLabel = new Label();
    private final Button vorherigeSeiteButton = new Button("Zurück");
    private final Button naechsteSeiteButton = new Button("Weiter");
    private final ImageView auswahlBild = new ImageView();
    private final Label auswahlBildHinweis = new Label("Kein Bild");
    private final Label auswahlName = new Label("Noch kein Produkt ausgewählt");
    private final Label auswahlDetails = new Label("Öffnen Sie die Produktauswahl, um ein Produkt zu wählen.");
    private List<Produkt> produktKatalog = List.of();
    private int produktSeite;
    private final ListView<BonPosition> warenkorbListe = new ListView<>();
    private final ListView<Bon> bonHistorieListe = new ListView<>();
    private final Label gesamtPreisLabel = new Label("Gesamtpreis: " + GeldFormatter.formatiereBetrag(0));
    private final TextField produktSucheField = new TextField();
    private final ComboBox<String> einheitFilterBox = new ComboBox<>();
    private final ComboBox<String> kategorieFilterBox = new ComboBox<>();
    private final ComboBox<String> steuerFilterBox = new ComboBox<>();
    private final TextField preisVonField = new TextField();
    private final TextField preisBisField = new TextField();
    private final CheckBox nurVerfuegbarCheck = new CheckBox("Nur verfügbar");
    private final TextField mengeField = new TextField();
    private final TextArea bonArea = new TextArea();
    private final Label statusLabel = new Label();
    private final Button checkoutButton = new Button("Kauf abschließen");

    private Produkt selectedProdukt = null;
    private Bon angezeigterBon;
    private final Button speichernButton = new Button("Speichern");

    public KassenView(ProduktService produktService, KassenService kassenService) {
        this.produktService = produktService;
        this.kassenService = kassenService;
        for (String id : einstellungen.get("favoriten", "").split(",")) {
            try { if (!id.isBlank()) favoriten.add(Integer.parseInt(id)); }
            catch (NumberFormatException ignoriert) { /* Veralteten Eintrag überspringen. */ }
        }

        // Listener registrieren für automatische Aktualisierung
        produktService.addListener(this::ladeProdukte);

        // root setup
        setSpacing(0);
        setPadding(Insets.EMPTY);
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
        Label prodLabel = new Label("Produkt auswählen");
        prodLabel.getStyleClass().add("section-label");

        produktSucheField.setPromptText("Produkt suchen...");
        produktSucheField.textProperty().addListener((obs, oldValue, newValue) -> filterGeaendert());
        Button filterButton = new Button("Filter");
        filterButton.getStyleClass().add("secondary-button");
        Button clearFilterButton = new Button("Filter zurücksetzen");
        clearFilterButton.getStyleClass().add("secondary-button");
        clearFilterButton.setOnAction(event -> setzeProduktFilterZurueck());
        HBox filterBar = new HBox(8, produktSucheField, filterButton, clearFilterButton);
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
        produktGrid.setPadding(new Insets(12));
        baueProduktauswahl(filterBar, erweiterteFilter);
        ladeProdukte();

        Button auswahlButton = new Button("Produktauswahl");
        auswahlButton.getStyleClass().add("primary-button");
        auswahlButton.setOnAction(event -> {
            ladeProdukte();
            produktOverlay.setVisible(true);
            produktOverlay.setManaged(true);
            produktOverlay.requestFocus();
        });
        HBox auswahlAktionen = new HBox(10, auswahlButton);
        auswahlAktionen.setAlignment(Pos.CENTER_LEFT);
        auswahlBild.setFitWidth(72);
        auswahlBild.setFitHeight(72);
        auswahlBild.setPreserveRatio(true);
        auswahlName.getStyleClass().add("product-card-name");
        auswahlDetails.getStyleClass().add("product-card-details");
        auswahlDetails.setWrapText(true);
        VBox auswahlText = new VBox(5, auswahlName, auswahlDetails);
        StackPane auswahlBildRahmen = new StackPane(auswahlBild, auswahlBildHinweis);
        auswahlBildRahmen.getStyleClass().add("selected-product-image");
        HBox auswahlBox = new HBox(14, auswahlBildRahmen, auswahlText);
        auswahlBox.setAlignment(Pos.CENTER_LEFT);
        auswahlBox.getStyleClass().add("selected-product-box");
        HBox.setHgrow(auswahlText, Priority.ALWAYS);
        aktualisiereAuswahl();

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
        bonArea.setPrefWidth(320);
        bonArea.setPromptText("Der erzeugte Bon erscheint hier nach dem Kaufabschluss.");
        bonArea.getStyleClass().add("info-box-text");
        bonArea.setWrapText(false);
        bonArea.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace;");

        infoBox.getChildren().add(bonArea);

        speichernButton.getStyleClass().add("secondary-button");
        speichernButton.setDisable(true);
        speichernButton.setOnAction(event -> exportiereBon());
        HBox bonAktionen = new HBox(8, speichernButton);

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
                zeigeBon(bon);
            }
        });

        Label schnellTitel = new Label("Schnellauswahl");
        schnellTitel.getStyleClass().add("section-label");
        VBox leftContent = new VBox(16, prodLabel, auswahlAktionen, schnellTitel, schnellauswahl, auswahlBox, controls,
                cartLabel, warenkorbListe, gesamtPreisLabel, checkoutRow);
        leftContent.setPrefWidth(700);
        HBox.setHgrow(leftContent, Priority.ALWAYS);

        VBox rightContent = new VBox(10, bonLabel, infoBox, bonAktionen, historieLabel, bonHistorieListe);
        rightContent.setPrefWidth(340);

        HBox arbeitsbereich = new HBox(24, leftContent, rightContent);
        arbeitsbereich.setAlignment(Pos.TOP_LEFT);

        // Assemble Main Container
        mainContainer.getChildren().addAll(
            title,
            hint,
            arbeitsbereich,
            statusLabel
        );

        StackPane seite = new StackPane(mainContainer);
        seite.setPadding(new Insets(24));
        StackPane.setAlignment(mainContainer, Pos.TOP_CENTER);
        ScrollPane seitenScroll = new ScrollPane(seite);
        seitenScroll.setFitToWidth(true);
        seitenScroll.getStyleClass().add("page-scroll");
        StackPane ansicht = new StackPane(seitenScroll, produktOverlay);
        produktOverlay.setVisible(false);
        produktOverlay.setManaged(false);
        getChildren().add(ansicht);
        VBox.setVgrow(ansicht, Priority.ALWAYS);
        aktualisiereWarenkorb();
        aktualisiereBonHistorie();
    }

    private void aktualisiereProduktGrid() {
        produktGrid.getChildren().clear();
        List<Produkt> passend = produktKatalog.stream().filter(this::matchesProduktSuche).toList();
        int seiten = Math.max(1, (passend.size() + PRODUKTE_PRO_SEITE - 1) / PRODUKTE_PRO_SEITE);
        produktSeite = Math.min(produktSeite, seiten - 1);
        int beginn = produktSeite * PRODUKTE_PRO_SEITE;
        for (Produkt p : passend.subList(beginn, Math.min(beginn + PRODUKTE_PRO_SEITE, passend.size()))) {
            produktGrid.getChildren().add(createProductCard(p));
        }
        if (passend.isEmpty()) produktGrid.getChildren().add(new Label("Keine passenden Produkte gefunden."));
        ergebnisLabel.setText(passend.size() + " Produkte");
        seitenLabel.setText("Seite " + (produktSeite + 1) + " von " + seiten);
        vorherigeSeiteButton.setDisable(produktSeite == 0);
        naechsteSeiteButton.setDisable(produktSeite >= seiten - 1);
    }

    private void filterGeaendert() {
        produktSeite = 0;
        aktualisiereProduktGrid();
    }

    private void ladeProdukte() {
        try {
            produktKatalog = produktService.alleProdukte();
            List<String> kategorien = produktKatalog.stream().map(Produkt::getKategorie).distinct().sorted(String.CASE_INSENSITIVE_ORDER).toList();
            String gewaehlteKategorie = kategorieFilterBox.getValue();
            kategorieFilterBox.getItems().setAll("Alle Kategorien");
            kategorieFilterBox.getItems().addAll(kategorien);
            kategorieFilterBox.setValue(kategorien.contains(gewaehlteKategorie) ? gewaehlteKategorie : "Alle Kategorien");
            if (selectedProdukt != null) {
                int id = selectedProdukt.getId();
                selectedProdukt = produktKatalog.stream().filter(p -> p.getId() == id).findFirst().orElse(null);
            }
            aktualisiereAuswahl();
            aktualisiereProduktGrid();
            aktualisiereSchnellauswahl();
        } catch (RuntimeException ex) {
            statusLabel.setText("Produkte konnten nicht geladen werden: " + ex.getMessage());
        }
    }

    public void aktualisiereDaten() {
        ladeProdukte();
        aktualisiereBonHistorie();
    }

    private void aktualisiereSchnellauswahl() {
        schnellauswahl.getChildren().clear();
        Map<Integer, Double> absatz = new HashMap<>();
        for (Bon bon : kassenService.getBonHistorie()) {
            for (BonPosition position : bon.getPositionen()) {
                absatz.merge(position.getProdukt().getId(), position.getMenge(), Double::sum);
            }
        }
        List<Produkt> verfuegbar = produktKatalog.stream().filter(p -> p.getLagerbestand() > 0).toList();
        List<Produkt> auswahl = new ArrayList<>();
        for (Integer id : favoriten) {
            verfuegbar.stream().filter(p -> p.getId() == id).findFirst().ifPresent(auswahl::add);
        }
        verfuegbar.stream()
                .filter(p -> !auswahl.contains(p))
                .sorted(Comparator.<Produkt>comparingDouble(p -> absatz.getOrDefault(p.getId(), 0.0)).reversed()
                        .thenComparing(Produkt::getName, String.CASE_INSENSITIVE_ORDER))
                .limit(Math.max(0, SCHNELLPLAETZE - auswahl.size()))
                .forEach(auswahl::add);
        for (Produkt produkt : auswahl.stream().limit(SCHNELLPLAETZE).toList()) {
            VBox karte = new VBox(5);
            karte.getStyleClass().add("quick-product-card");
            Label preis = new Label(GeldFormatter.formatiereBetrag(produkt.getPreis()));
            Button waehlen = new Button(produkt.getName());
            waehlen.setMaxWidth(Double.MAX_VALUE);
            waehlen.setTooltip(new Tooltip("Produkt auswählen: " + produkt.getName()));
            waehlen.setOnAction(event -> { waehleProdukt(produkt); mengeField.setText("1"); });
            Button direkt = new Button("+ 1");
            direkt.setOnAction(event -> fuegeDirektHinzu(produkt));
            Button stern = new Button(favoriten.contains(produkt.getId()) ? "★" : "☆");
            stern.setAccessibleText(favoriten.contains(produkt.getId()) ? "Favorit entfernen" : "Als Favorit speichern");
            stern.setOnAction(event -> wechsleFavorit(produkt.getId()));
            HBox aktionen = new HBox(4, direkt, stern);
            karte.getChildren().addAll(waehlen, preis, aktionen);
            schnellauswahl.getChildren().add(karte);
        }
        if (auswahl.isEmpty()) schnellauswahl.getChildren().add(new Label("Noch keine verfügbaren Produkte."));
    }

    private void wechsleFavorit(int id) {
        if (!favoriten.remove(id)) {
            if (favoriten.size() >= SCHNELLPLAETZE) {
                statusLabel.setText("Sie können höchstens fünf Favoriten speichern.");
                return;
            }
            favoriten.add(id);
        }
        einstellungen.put("favoriten", String.join(",", favoriten.stream().map(String::valueOf).toList()));
        aktualisiereSchnellauswahl();
        aktualisiereProduktGrid();
    }

    private void aktualisiereAuswahl() {
        if (selectedProdukt == null) {
            auswahlBild.setImage(null);
            auswahlBildHinweis.setVisible(true);
            auswahlName.setText("Noch kein Produkt ausgewählt");
            auswahlDetails.setText("Öffnen Sie die Produktauswahl, um ein Produkt zu wählen.");
            return;
        }
        boolean hatBild = selectedProdukt.getBildPfad() != null && !selectedProdukt.getBildPfad().isBlank();
        auswahlBild.setImage(hatBild ? ImageUtil.loadProductImage(selectedProdukt.getBildPfad()) : null);
        auswahlBildHinweis.setVisible(!hatBild);
        auswahlName.setText(selectedProdukt.getName());
        auswahlDetails.setText(GeldFormatter.formatiereBetrag(selectedProdukt.getPreis()) + "/"
                + selectedProdukt.getEinheitLabel() + " · Lagerbestand: "
                + MengenFormatter.formatiereMenge(selectedProdukt.getLagerbestand(), selectedProdukt.getEinheitLabel()));
    }

    private void baueProduktauswahl(HBox filterBar, VBox erweiterteFilter) {
        ScrollPane produktScroll = new ScrollPane(produktGrid);
        produktScroll.setFitToWidth(true);
        produktScroll.setPrefViewportHeight(300);
        produktScroll.setMinHeight(120);
        produktScroll.getStyleClass().add("product-picker-scroll");
        vorherigeSeiteButton.setOnAction(event -> { produktSeite--; aktualisiereProduktGrid(); produktScroll.setVvalue(0); });
        naechsteSeiteButton.setOnAction(event -> { produktSeite++; aktualisiereProduktGrid(); produktScroll.setVvalue(0); });
        HBox seitenSteuerung = new HBox(12, ergebnisLabel, new Region(), vorherigeSeiteButton,
                seitenLabel, naechsteSeiteButton);
        HBox.setHgrow(seitenSteuerung.getChildren().get(1), Priority.ALWAYS);
        seitenSteuerung.setAlignment(Pos.CENTER_LEFT);
        Label titel = new Label("Produktauswahl");
        titel.getStyleClass().add("title-label");
        Button schliessen = new Button("Schließen");
        schliessen.setOnAction(event -> schliesseProduktauswahl());
        Region abstand = new Region();
        HBox.setHgrow(abstand, Priority.ALWAYS);
        HBox kopf = new HBox(12, titel, abstand, schliessen);
        kopf.setAlignment(Pos.CENTER_LEFT);
        VBox inhalt = new VBox(12, kopf, filterBar, erweiterteFilter, produktScroll, seitenSteuerung);
        inhalt.setPrefWidth(850);
        inhalt.setMaxWidth(900);
        inhalt.maxHeightProperty().bind(produktOverlay.heightProperty().subtract(36));
        VBox.setVgrow(produktScroll, Priority.ALWAYS);
        inhalt.getStyleClass().add("product-picker-panel");
        produktOverlay.getStyleClass().add("product-picker-overlay");
        produktOverlay.setFocusTraversable(true);
        produktOverlay.getChildren().add(inhalt);
        produktOverlay.setOnMouseClicked(event -> {
            if (event.getTarget() == produktOverlay) schliesseProduktauswahl();
        });
        produktOverlay.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) schliesseProduktauswahl();
        });
    }

    private void schliesseProduktauswahl() {
        produktOverlay.setVisible(false);
        produktOverlay.setManaged(false);
    }

    private boolean matchesProduktSuche(Produkt produkt) {
        String suche = produktSucheField.getText();
        if (suche != null && !suche.isBlank()) {
            String text = (produkt.getId() + " " + produkt.getName() + " "
                    + produkt.getKategorie() + " "
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

        String kategorieFilter = kategorieFilterBox.getValue();
        if (kategorieFilter != null && !"Alle Kategorien".equals(kategorieFilter)
                && !produkt.getKategorie().equals(kategorieFilter)) return false;

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
        VBox card = new VBox(8);
        card.getStyleClass().add("product-card");
        card.setAlignment(Pos.TOP_LEFT);
        card.setPrefWidth(188);
        card.setMinWidth(188);
        card.setMaxWidth(188);

        boolean hatBild = p.getBildPfad() != null && !p.getBildPfad().isBlank();
        ImageView imageView = new ImageView(hatBild ? ImageUtil.loadProductImage(p.getBildPfad()) : null);
        imageView.setFitWidth(158);
        imageView.setFitHeight(126);
        imageView.setPreserveRatio(true);
        StackPane bildRahmen = new StackPane(hatBild ? imageView : new Label("Kein Bild"));
        bildRahmen.getStyleClass().add("product-picker-image");
        bildRahmen.setMinHeight(135);
        bildRahmen.setMaxHeight(135);

        Label nameLabel = new Label(p.getName());
        nameLabel.getStyleClass().add("product-card-name");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(175);
        Label preisLabel = new Label(GeldFormatter.formatiereBetrag(p.getPreis()) + "/" + p.getEinheitLabel());
        preisLabel.getStyleClass().add("product-card-price");
        Label kategorieLabel = new Label(p.getKategorie());
        kategorieLabel.getStyleClass().add("product-card-details");
        Label bestandLabel = new Label("Lagerbestand: "
                + MengenFormatter.formatiereMenge(p.getLagerbestand(), p.getEinheitLabel()));
        bestandLabel.getStyleClass().add("product-card-details");

        Button waehlenButton = new Button("Auswählen");
        waehlenButton.getStyleClass().add("primary-button");
        waehlenButton.setMaxWidth(Double.MAX_VALUE);
        waehlenButton.setDisable(p.getLagerbestand() <= 0);
        waehlenButton.setOnAction(event -> {
            waehleProdukt(p);
            schliesseProduktauswahl();
        });
        Button favoritenButton = new Button(favoriten.contains(p.getId()) ? "★ Favorit" : "☆ Merken");
        favoritenButton.setOnAction(event -> wechsleFavorit(p.getId()));
        card.getChildren().addAll(bildRahmen, nameLabel, kategorieLabel, preisLabel, bestandLabel, waehlenButton, favoritenButton);
        if (p.getLagerbestand() <= 0) card.getStyleClass().add("product-card-unavailable");
        return card;
    }

    private void waehleProdukt(Produkt produkt) {
        selectedProdukt = produkt;
        aktualisiereAuswahl();
        Platform.runLater(mengeField::requestFocus);
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
            aktualisiereWarenkorb();
            aktualisiereBonHistorie();
            bonHistorieListe.getSelectionModel().select(bon);
            zeigeBon(bon);

            AlertUtil.showInfo("Kauf erfolgreich abgeschlossen",
                    "Bon Nr. " + bon.getBonnummer() + "\nGesamtpreis: " + GeldFormatter.formatiereBetrag(bon.getGesamtpreis()));

            statusLabel.setText("Kauf abgeschlossen. Lagerbestand wurde aktualisiert.");
            ladeProdukte();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }

    private void zeigeBon(Bon bon) {
        angezeigterBon = bon;
        bonArea.setText(bonFormatService.formatiereBon(bon));
        speichernButton.setDisable(false);
    }

    private void exportiereBon() {
        if (angezeigterBon == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Bon speichern");
        chooser.setInitialFileName("bon-" + angezeigterBon.getBonnummer());
        FileChooser.ExtensionFilter pdfFilter = new FileChooser.ExtensionFilter("PDF-Datei (*.pdf)", "*.pdf");
        FileChooser.ExtensionFilter txtFilter = new FileChooser.ExtensionFilter("Textdatei (*.txt)", "*.txt");
        chooser.getExtensionFilters().addAll(pdfFilter, txtFilter);
        File ziel = chooser.showSaveDialog(getScene().getWindow());
        if (ziel == null) {
            return;
        }
        try {
            String dateiname = ziel.getName().toLowerCase(java.util.Locale.ROOT);
            boolean text = dateiname.endsWith(".txt") || (!dateiname.endsWith(".pdf") && chooser.getSelectedExtensionFilter() == txtFilter);
            File datei = dateiname.endsWith(".txt") || dateiname.endsWith(".pdf") ? ziel
                    : new File(ziel.getParentFile(), ziel.getName() + (text ? ".txt" : ".pdf"));
            if (text) Files.writeString(datei.toPath(), bonFormatService.formatiereBon(angezeigterBon), StandardCharsets.UTF_8);
            else bonPdfService.exportiere(angezeigterBon, datei.toPath());
            statusLabel.setText("Bon " + angezeigterBon.getBonnummer() + " gespeichert: " + datei.getName());
        } catch (Exception ex) {
            statusLabel.setText("Bon konnte nicht gespeichert werden: " + ex.getMessage());
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

        einheitFilterBox.setOnAction(event -> filterGeaendert());
        steuerFilterBox.setOnAction(event -> filterGeaendert());
        preisVonField.textProperty().addListener((obs, oldValue, newValue) -> filterGeaendert());
        preisBisField.textProperty().addListener((obs, oldValue, newValue) -> filterGeaendert());
        nurVerfuegbarCheck.selectedProperty().addListener((obs, oldValue, newValue) -> filterGeaendert());

        kategorieFilterBox.getItems().setAll("Alle Kategorien");
        kategorieFilterBox.setValue("Alle Kategorien");
        kategorieFilterBox.setOnAction(event -> filterGeaendert());
        FlowPane row = new FlowPane(8, 8, kategorieFilterBox, einheitFilterBox, steuerFilterBox, preisVonField, preisBisField, nurVerfuegbarCheck, resetButton);
        row.setAlignment(Pos.CENTER_LEFT);
        VBox box = new VBox(row);
        box.getStyleClass().add("filter-panel");
        return box;
    }

    private void setzeProduktFilterZurueck() {
        produktSucheField.clear();
        einheitFilterBox.getSelectionModel().select("Alle Einheiten");
        kategorieFilterBox.getSelectionModel().select("Alle Kategorien");
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
