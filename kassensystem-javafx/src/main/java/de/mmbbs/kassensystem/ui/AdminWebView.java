package de.mmbbs.kassensystem.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

import java.awt.Desktop;
import java.net.URI;

/**
 * Eingebetteter Zugriff auf den Spring-Adminbereich.
 *
 * <p>Die JavaFX-Rolle entscheidet, ob dieser Tab sichtbar ist. Die Webseite
 * selbst ist zusaetzlich serverseitig ueber Spring Security geschuetzt.</p>
 */
public class AdminWebView extends BorderPane {
    public static final String ADMIN_URL = "http://localhost:8080/kassensystem/admin/";
    private final WebView webView = new WebView();
    private final Label statusLabel = new Label("Spring Boot muss fuer die Verwaltung laufen.");

    public AdminWebView() {
        getStyleClass().add("admin-web-view");
        setPadding(new Insets(18));

        Button loadButton = new Button("Admin laden");
        loadButton.getStyleClass().add("primary-button");
        loadButton.setOnAction(event -> ladeAdminseite());

        Button reloadButton = new Button("Neu laden");
        reloadButton.getStyleClass().add("secondary-button");
        reloadButton.setOnAction(event -> ladeAdminseite());

        Button browserButton = new Button("Im Browser oeffnen");
        browserButton.getStyleClass().add("secondary-button");
        browserButton.setOnAction(event -> oeffneImBrowser());

        HBox toolbar = new HBox(10, loadButton, reloadButton, browserButton, statusLabel);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(0, 0, 12, 0));
        HBox.setHgrow(statusLabel, Priority.ALWAYS);

        Label title = new Label("Verwaltung");
        title.getStyleClass().add("title-label");
        Label hint = new Label("Admin-Webadresse: " + ADMIN_URL);
        hint.getStyleClass().add("subtitle-label");
        hint.setWrapText(true);

        VBox startBox = new VBox(12, title, hint, loadButton);
        startBox.setAlignment(Pos.CENTER);
        startBox.setPadding(new Insets(40));

        setTop(toolbar);
        setCenter(startBox);
    }

    private void ladeAdminseite() {
        webView.getEngine().load(ADMIN_URL);
        setCenter(webView);
        statusLabel.setText("Adminbereich geladen. Bei Bedarf im Webformular anmelden.");
    }

    private void oeffneImBrowser() {
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
