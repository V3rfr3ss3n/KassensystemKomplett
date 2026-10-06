package de.mmbbs.kassensystem.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.function.Consumer;

/** Eingabe für USB-Scanner im Tastaturmodus mit Enter als Abschluss. */
final class ScannerEingabe extends HBox {
    private final TextField eingabe = new TextField();

    ScannerEingabe(Consumer<String> beiScan) {
        super(10);
        eingabe.setPromptText("QR-Code oder Barcode scannen und Enter drücken");
        eingabe.setAccessibleText("Code scannen");
        eingabe.setMaxWidth(Double.MAX_VALUE);
        eingabe.setOnAction(event -> {
            String code = eingabe.getText().trim();
            eingabe.clear();
            beiScan.accept(code);
        });
        setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(new Label("Code scannen"), eingabe);
        HBox.setHgrow(eingabe, Priority.ALWAYS);
    }

    void fokussieren() {
        eingabe.requestFocus();
    }
}
