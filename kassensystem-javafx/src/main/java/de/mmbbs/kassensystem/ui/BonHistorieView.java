package de.mmbbs.kassensystem.ui;

import de.mmbbs.kassensystem.model.Bon;
import de.mmbbs.kassensystem.service.BonPdfService;
import de.mmbbs.kassensystem.service.BonService;
import de.mmbbs.kassensystem.service.KassenService;
import de.mmbbs.kassensystem.util.GeldFormatter;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/** Anzeige und Export der aktuellen und historischen Bons. */
final class BonHistorieView extends VBox {
    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM. HH:mm");
    private final BonService formatter = new BonService();
    private final BonPdfService pdf = new BonPdfService();
    private final ListView<Bon> historie = new ListView<>();
    private final TextArea bonText = new TextArea();
    private final Button speichern = new Button("Speichern");
    private final Consumer<String> status;
    private Bon angezeigterBon;

    BonHistorieView(Consumer<String> status) {
        super(10);
        this.status = status;
        Label bonLabel = new Label("Bon");
        bonLabel.getStyleClass().add("section-label");
        Label historieLabel = new Label("Bon-Historie");
        historieLabel.getStyleClass().add("section-label");
        bonText.setEditable(false);
        bonText.setPrefHeight(340);
        bonText.setPrefWidth(320);
        bonText.setPromptText("Der erzeugte Bon erscheint hier nach dem Kaufabschluss.");
        bonText.getStyleClass().add("info-box-text");
        bonText.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace;");
        VBox info = new VBox(bonText);
        info.getStyleClass().add("info-box");
        speichern.getStyleClass().add("secondary-button");
        speichern.setDisable(true);
        speichern.setOnAction(event -> exportiereBon());
        historie.setPrefHeight(170);
        historie.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Bon bon, boolean empty) {
                super.updateItem(bon, empty);
                setText(empty || bon == null ? null : "Bon " + bon.getBonnummer() + " · "
                        + DATUM.format(bon.getDatumUhrzeit()) + " · "
                        + GeldFormatter.formatiereBetrag(bon.getGesamtpreis()));
            }
        });
        historie.getSelectionModel().selectedItemProperty().addListener((obs, alt, bon) -> {
            if (bon != null) zeigeBon(bon);
        });
        getChildren().addAll(bonLabel, info, new HBox(8, speichern), historieLabel, historie);
        setPrefWidth(340);
    }

    void aktualisiere(KassenService kasse) {
        historie.getItems().setAll(kasse.getBonHistorie());
    }

    void zeigeBon(Bon bon) {
        angezeigterBon = bon;
        bonText.setText(formatter.formatiereBon(bon));
        speichern.setDisable(false);
        historie.getSelectionModel().select(bon);
    }

    void exportiereBon() {
        if (angezeigterBon == null) return;
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Bon speichern");
        chooser.setInitialFileName("bon-" + angezeigterBon.getBonnummer());
        FileChooser.ExtensionFilter pdfFilter = new FileChooser.ExtensionFilter("PDF-Datei (*.pdf)", "*.pdf");
        FileChooser.ExtensionFilter txtFilter = new FileChooser.ExtensionFilter("Textdatei (*.txt)", "*.txt");
        chooser.getExtensionFilters().addAll(pdfFilter, txtFilter);
        File ziel = chooser.showSaveDialog(getScene().getWindow());
        if (ziel == null) return;
        try {
            String name = ziel.getName().toLowerCase(java.util.Locale.ROOT);
            boolean text = name.endsWith(".txt") || (!name.endsWith(".pdf") && chooser.getSelectedExtensionFilter() == txtFilter);
            File datei = name.endsWith(".txt") || name.endsWith(".pdf") ? ziel
                    : new File(ziel.getParentFile(), ziel.getName() + (text ? ".txt" : ".pdf"));
            if (text) Files.writeString(datei.toPath(), formatter.formatiereBon(angezeigterBon), StandardCharsets.UTF_8);
            else pdf.exportiere(angezeigterBon, datei.toPath());
            status.accept("Bon " + angezeigterBon.getBonnummer() + " gespeichert: " + datei.getName());
        } catch (Exception ex) {
            status.accept("Bon konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }
}
