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
                         Runnable produktAnlegen,
                         Runnable warenzugangOeffnen,
                         Runnable lagerbestandOeffnen) {
        setSpacing(18);
        setPadding(new Insets(40));
        setAlignment(Pos.TOP_CENTER);

        VBox mainContainer = new VBox(16);
        mainContainer.getStyleClass().add("main-container");
        mainContainer.setMaxWidth(520);

        Label title = new Label("Hauptmenü");
        title.getStyleClass().add("title-label");

        Label info = new Label("Angemeldet als " + benutzer.benutzername() + " (" + benutzer.rolle() + ")");
        info.getStyleClass().add("subtitle-label");

        Button kasseButton = menuButton("Kassenvorgang starten", kasseOeffnen);
        Button produktButton = menuButton("Neues Produkt hinzufügen", produktAnlegen);
        Button zugangButton = menuButton("Warenzugang erfassen", warenzugangOeffnen);
        Button lagerButton = menuButton("Lagerbestand anzeigen", lagerbestandOeffnen);

        mainContainer.getChildren().addAll(title, info, kasseButton, produktButton, zugangButton, lagerButton);
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
