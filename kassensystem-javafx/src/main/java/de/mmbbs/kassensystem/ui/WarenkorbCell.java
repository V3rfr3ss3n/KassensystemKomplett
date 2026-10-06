package de.mmbbs.kassensystem.ui;

import de.mmbbs.kassensystem.model.BonPosition;
import de.mmbbs.kassensystem.service.KassenService;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.function.Consumer;

/** Bedienbare Warenkorbposition mit Bestandsprüfung über den Kassenservice. */
final class WarenkorbCell extends ListCell<BonPosition> {
    private final Label text = new Label();
    private final Button minus = new Button("-");
    private final Button plus = new Button("+");
    private final Button entfernen = new Button("Entfernen");
    private final HBox inhalt = new HBox(8, text, minus, plus, entfernen);

    WarenkorbCell(KassenService kasse, Runnable aktualisieren, Consumer<String> status) {
        text.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(text, Priority.ALWAYS);
        inhalt.setAlignment(Pos.CENTER_LEFT);
        for (Button button : new Button[]{minus, plus, entfernen}) button.getStyleClass().add("secondary-button");
        minus.setOnAction(event -> {
            if (getItem() == null) return;
            kasse.positionVerringern(getItem());
            aktualisieren.run();
        });
        plus.setOnAction(event -> {
            if (getItem() == null) return;
            try {
                kasse.positionErhoehen(getItem());
                aktualisieren.run();
            } catch (IllegalArgumentException ex) {
                status.accept(ex.getMessage());
            }
        });
        entfernen.setOnAction(event -> {
            if (getItem() == null) return;
            kasse.positionEntfernen(getItem());
            aktualisieren.run();
        });
    }

    @Override protected void updateItem(BonPosition position, boolean empty) {
        super.updateItem(position, empty);
        if (empty || position == null) {
            setGraphic(null);
            return;
        }
        text.setText(position.toString());
        setGraphic(inhalt);
    }
}
