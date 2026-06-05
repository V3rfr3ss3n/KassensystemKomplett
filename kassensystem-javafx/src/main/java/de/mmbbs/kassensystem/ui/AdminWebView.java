package de.mmbbs.kassensystem.ui;

import de.mmbbs.kassensystem.auth.Benutzer;
import de.mmbbs.kassensystem.auth.SsoTicketService;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.concurrent.Worker;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Eingebetteter Zugriff auf den Spring-Adminbereich.
 *
 * <p>Die JavaFX-Rolle entscheidet, ob dieser Tab sichtbar ist. Die Webseite
 * selbst ist zusaetzlich serverseitig ueber Spring Security geschuetzt.</p>
 */
public class AdminWebView extends BorderPane {
    public static final String ADMIN_URL = "http://localhost:8080/kassensystem/admin/";
    private static final String SSO_LOGIN_URL = "http://localhost:8080/kassensystem/auth/javafx-login";

    private final Benutzer benutzer;
    private final ReadOnlyBooleanProperty darkModeProperty;
    private final SsoTicketService ssoTicketService = new SsoTicketService();
    private final WebView webView = new WebView();
    private final Label statusLabel = new Label("Spring Boot muss fuer die Verwaltung laufen.");

    public AdminWebView(Benutzer benutzer, ReadOnlyBooleanProperty darkModeProperty) {
        this.benutzer = benutzer;
        this.darkModeProperty = darkModeProperty;

        getStyleClass().add("admin-web-view");
        setPadding(new Insets(18));

        Button loadButton = new Button("Admin laden");
        loadButton.getStyleClass().add("primary-button");
        loadButton.setOnAction(event -> ladeAdminseite());

        Button startLoadButton = new Button("Verwaltung laden");
        startLoadButton.getStyleClass().add("primary-button");
        startLoadButton.setOnAction(event -> ladeAdminseite());

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

        setTop(toolbar);
        VBox startBox = new VBox(12, title, hint, startLoadButton);
        startBox.setAlignment(Pos.CENTER);
        startBox.setPadding(new Insets(40));
        setCenter(startBox);

        webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                synchronisiereWebTheme();
                statusLabel.setText("Verwaltung geladen fuer " + benutzer.benutzername() + ".");
            } else if (state == Worker.State.FAILED) {
                statusLabel.setText("Verwaltung konnte nicht geladen werden. Laeuft Spring Boot?");
            }
        });
        darkModeProperty.addListener((obs, oldValue, newValue) -> synchronisiereWebTheme());
    }

    private void ladeAdminseite() {
        webView.getEngine().load(erstelleSsoUrl());
        setCenter(webView);
        statusLabel.setText("Verwaltung wird mit JavaFX-Anmeldung geoeffnet.");
    }

    private void oeffneImBrowser() {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(erstelleSsoUrl()));
            } else {
                AlertUtil.showInfo("Adminbereich", "Admin-Webadresse: " + ADMIN_URL);
            }
        } catch (Exception ex) {
            AlertUtil.showWarning("Adminbereich", "Admin-Webadresse: " + ADMIN_URL);
        }
    }

    private String erstelleSsoUrl() {
        String ticket = URLEncoder.encode(ssoTicketService.erstelleTicket(benutzer), StandardCharsets.UTF_8);
        String theme = darkModeProperty.get() ? "dark" : "light";
        return SSO_LOGIN_URL + "?ticket=" + ticket + "&theme=" + theme;
    }

    private void synchronisiereWebTheme() {
        String location = webView.getEngine().getLocation();
        if (location == null || location.isBlank()) {
            return;
        }

        String theme = darkModeProperty.get() ? "dark" : "light";
        try {
            webView.getEngine().executeScript("window.setThemeFromJavaFx && window.setThemeFromJavaFx('" + theme + "')");
        } catch (Exception ignored) {
            // Die Seite kann noch im Spring-Login oder Fehlerzustand sein.
        }
    }
}
