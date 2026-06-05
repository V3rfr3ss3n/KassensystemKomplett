package de.mmbbs.kassensystem.ui;

import de.mmbbs.kassensystem.auth.Benutzer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Hauptmenue fuer Admins.
 *
 * <p>Damit wird die Hauptmenue-Anforderung aus dem Lastenheft abgebildet. Die
 * einzelnen Menuepunkte verweisen auf Kasse oder Verwaltung.</p>
 */
public class HauptmenuView extends VBox {
    public HauptmenuView(Benutzer benutzer,
                         Runnable kasseOeffnen,
                         Runnable verwaltungOeffnen,
                         Runnable beenden) {
        setSpacing(18);
        setPadding(new Insets(40));
        setAlignment(Pos.TOP_CENTER);

        VBox mainContainer = new VBox(16);
        mainContainer.getStyleClass().add("main-container");
        mainContainer.setMaxWidth(520);

        Label title = new Label("Hauptmenue");
        title.getStyleClass().add("title-label");

        Label info = new Label("Angemeldet als " + benutzer.benutzername() + " (" + benutzer.rolle() + ")");
        info.getStyleClass().add("subtitle-label");

        Button kasseButton = menuButton("Kassenvorgang starten", kasseOeffnen);
        Button produktButton = menuButton("Neues Produkt hinzufuegen", verwaltungOeffnen);
        Button zugangButton = menuButton("Warenzugang erfassen", verwaltungOeffnen);
        Button lagerButton = menuButton("Lagerbestand anzeigen", verwaltungOeffnen);
        Button beendenButton = menuButton("Programm beenden", beenden);
        beendenButton.getStyleClass().add("danger-button");

        mainContainer.getChildren().addAll(title, info, kasseButton, produktButton, zugangButton, lagerButton, beendenButton);
        getChildren().add(mainContainer);
    }

    private Button menuButton(String text, Runnable action) {
        Button button = new Button(text);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setPrefHeight(44);
        button.getStyleClass().add("primary-button");
        button.setOnAction(event -> action.run());
        return button;
    }
}
