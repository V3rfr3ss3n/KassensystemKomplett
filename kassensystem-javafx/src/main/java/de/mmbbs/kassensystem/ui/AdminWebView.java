package de.mmbbs.kassensystem.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

import java.awt.Desktop;
import java.net.URI;
import java.util.Optional;

public class AdminWebView extends BorderPane {
    public static final String ADMIN_URL = "http://localhost:8080/kassensystem/admin/";
    private final WebView webView = new WebView();
    private final Label statusLabel = new Label("Adminbereich gesperrt.");
    private boolean entsperrt;

    public AdminWebView() {
        getStyleClass().add("admin-web-view");
        setPadding(new Insets(18));

        Button unlockButton = new Button("Admin entsperren");
        unlockButton.getStyleClass().add("primary-button");
        unlockButton.setOnAction(event -> entsperrenUndLaden());

        Button reloadButton = new Button("Neu laden");
        reloadButton.getStyleClass().add("secondary-button");
        reloadButton.setOnAction(event -> ladeAdminseite());

        Button browserButton = new Button("Im Browser oeffnen");
        browserButton.getStyleClass().add("secondary-button");
        browserButton.setOnAction(event -> oeffneImBrowser());

        HBox toolbar = new HBox(10, unlockButton, reloadButton, browserButton, statusLabel);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 12, 0));
        HBox.setHgrow(statusLabel, Priority.ALWAYS);

        Label title = new Label("Verwaltung");
        title.getStyleClass().add("title-label");
        Label hint = new Label("Spring Boot muss laufen: " + ADMIN_URL);
        hint.getStyleClass().add("subtitle-label");
        hint.setWrapText(true);

        VBox lockedBox = new VBox(12, title, hint, unlockButton);
        lockedBox.setAlignment(Pos.CENTER);
        lockedBox.setPadding(new Insets(40));

        setTop(toolbar);
        setCenter(lockedBox);
    }

    private void entsperrenUndLaden() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Adminzugang");
        dialog.setHeaderText("Adminbereich oeffnen");
        dialog.setContentText("Passwort:");

        Optional<String> passwort = dialog.showAndWait();
        if (passwort.isEmpty()) {
            return;
        }
        if (!"1234".equals(passwort.get())) {
            AlertUtil.showWarning("Adminzugang", "Passwort ist falsch.");
            return;
        }
        entsperrt = true;
        ladeAdminseite();
    }

    private void ladeAdminseite() {
        if (!entsperrt) {
            entsperrenUndLaden();
            return;
        }
        webView.getEngine().load(ADMIN_URL);
        setCenter(webView);
        statusLabel.setText("Adminbereich geladen.");
    }

    private void oeffneImBrowser() {
        if (!entsperrt) {
            entsperrenUndLaden();
            if (!entsperrt) {
                return;
            }
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
