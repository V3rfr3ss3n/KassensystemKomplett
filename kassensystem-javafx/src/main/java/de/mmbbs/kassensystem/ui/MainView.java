package de.mmbbs.kassensystem.ui;

import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.service.ProduktService;
import de.mmbbs.kassensystem.util.GeldFormatter;
import de.mmbbs.kassensystem.util.MengenFormatter;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;
import java.util.Optional;

public class MainView extends VBox {
    private static final String ADMIN_URL = AdminWebView.ADMIN_URL;
    private final ProduktService produktService;
    private final Label valueLabel = new Label("Gesamtwert im Lager: " + GeldFormatter.formatiereBetrag(0));
    private final Label summaryLabel = new Label("Produkte insgesamt: 0");
    private final Label stockLabel = new Label("Gesamtbestand: 0");
    private final TableView<Produkt> produktListe = new TableView<>();
    private final ProduktTableHelper.FilterFields produktFilter;

    public MainView(ProduktService produktService) {
        this.produktService = produktService;
        this.produktFilter = new ProduktTableHelper.FilterFields(this::refresh);

        setSpacing(0);
        setPadding(new Insets(40));
        setAlignment(Pos.TOP_CENTER);

        VBox mainContainer = new VBox();
        mainContainer.getStyleClass().add("main-container");
        mainContainer.setSpacing(20);
        mainContainer.setMaxWidth(900);

        Label title = new Label("Kassensystem MVP");
        title.getStyleClass().add("title-label");

        Label info = new Label("Startseite mit Produktübersicht, Kassenfunktion und Lagerverwaltung.");
        info.getStyleClass().add("subtitle-label");
        info.setWrapText(true);

        Label hint = new Label("Die Übersicht zeigt sofort an, wie viele Produkte aktuell verfügbar sind.");
        hint.getStyleClass().add("subtitle-label");
        hint.setWrapText(true);

        Label overview = new Label("Aktueller Produktbestand");
        overview.getStyleClass().add("section-label");

        summaryLabel.getStyleClass().add("subtitle-label");
        stockLabel.getStyleClass().add("subtitle-label");
        valueLabel.getStyleClass().add("subtitle-label");

        HBox summaryBox = new HBox(16, summaryLabel, stockLabel, valueLabel);
        summaryBox.setAlignment(Pos.CENTER_LEFT);
        summaryBox.setPadding(new Insets(5, 0, 0, 0));

        produktListe.setPrefHeight(300);
        produktListe.getColumns().setAll(
                ProduktTableHelper.bildColumn(),
                ProduktTableHelper.idColumn(produktFilter),
                ProduktTableHelper.nameColumn(produktFilter),
                ProduktTableHelper.preisColumn(produktFilter),
                ProduktTableHelper.lagerColumn(produktFilter)
        );
        aktualisiereTable(produktListe, summaryLabel, stockLabel, produktService);

        Button demoButton = new Button("MVP-Status anzeigen");
        demoButton.getStyleClass().add("secondary-button");
        demoButton.setOnAction(event -> {
            AlertUtil.showInfo("MVP-Status",
                    "Die aktuelle Version enthält Modell-, Repository-, Service- und UI-Bausteine für Kasse, Lager und Produktanlegen.");
        });

        Button adminButton = new Button("Adminbereich öffnen");
        adminButton.getStyleClass().add("secondary-button");
        adminButton.setOnAction(event -> oeffneAdminbereich());

        HBox actions = new HBox(10, demoButton, adminButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        mainContainer.getChildren().addAll(title, info, hint, overview, summaryBox, produktListe, actions);
        getChildren().add(mainContainer);
    }

    public void refresh() {
        aktualisiereTable(produktListe, summaryLabel, stockLabel, produktService);
    }

    private void aktualisiereTable(TableView<Produkt> table, Label summaryLabel, Label stockLabel, ProduktService produktService) {
        List<Produkt> alleProdukte = produktService.alleProdukte();
        List<Produkt> gefilterteProdukte = alleProdukte.stream()
                .filter(produktFilter::matches)
                .toList();

        table.setItems(FXCollections.observableArrayList(gefilterteProdukte));
        summaryLabel.setText("Produkte insgesamt: " + alleProdukte.size());
        double gesamtbestand = alleProdukte.stream().mapToDouble(Produkt::getLagerbestand).sum();
        double gesamtwert = alleProdukte.stream()
                .mapToDouble(produkt -> produkt.getPreis() * produkt.getLagerbestand())
                .sum();
        stockLabel.setText("Gesamtbestand: " + MengenFormatter.formatiereMenge(gesamtbestand));
        valueLabel.setText("Gesamtwert im Lager: " + GeldFormatter.formatiereBetrag(gesamtwert));
    }

    private void oeffneAdminbereich() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Adminzugang");
        dialog.setHeaderText("Adminbereich öffnen");
        dialog.setContentText("Passwort:");

        Optional<String> passwort = dialog.showAndWait();
        if (passwort.isEmpty()) {
            return;
        }
        if (!"1234".equals(passwort.get())) {
            AlertUtil.showWarning("Adminzugang", "Passwort ist falsch.");
            return;
        }

        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(ADMIN_URL));
            } else {
                AlertUtil.showInfo("Adminbereich", "Admin-Webadresse: " + ADMIN_URL);
            }
        } catch (Exception ex) {
            AlertUtil.showWarning("Adminbereich", "Admin-Webadresse: " + ADMIN_URL);
        }
    }
}
