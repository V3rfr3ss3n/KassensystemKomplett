package de.mmbbs.kassensystem.ui;

import de.mmbbs.kassensystem.auth.Benutzer;
import de.mmbbs.kassensystem.auth.SsoTicketService;
import de.mmbbs.kassensystem.repository.ApiClient;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.concurrent.Worker;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
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
    public static final String ADMIN_URL = ApiClient.basisUrl() + "/admin/";
    private static final String SSO_LOGIN_URL = ApiClient.basisUrl() + "/auth/javafx-login";

    private final Benutzer benutzer;
    private final ReadOnlyBooleanProperty darkModeProperty;
    private final SsoTicketService ssoTicketService = new SsoTicketService();
    private final WebView webView = new WebView();
    private final Label statusLabel = new Label("Verwaltung wird geladen …");
    private boolean geladen;
    private String zielNachLaden;

    public AdminWebView(Benutzer benutzer, ReadOnlyBooleanProperty darkModeProperty) {
        this.benutzer = benutzer;
        this.darkModeProperty = darkModeProperty;

        getStyleClass().add("admin-web-view");
        setPadding(new Insets(18));

        statusLabel.setPadding(new Insets(0, 0, 12, 0));
        statusLabel.managedProperty().bind(statusLabel.visibleProperty());
        setTop(statusLabel);
        setCenter(webView);

        webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                synchronisiereWebTheme();
                statusLabel.setText("Verwaltung geladen für " + benutzer.benutzername() + ".");
                statusLabel.setVisible(false);
                if (istAdminseite()) {
                    webView.getEngine().executeScript("document.querySelector('.settings-menu').hidden = true");
                    if (zielNachLaden != null) {
                        navigiereZu(zielNachLaden);
                    } else {
                        aktualisiereProdukte();
                    }
                }
            } else if (state == Worker.State.FAILED) {
                geladen = false;
                statusLabel.setVisible(true);
                statusLabel.setText("Verwaltung konnte nicht geladen werden. Läuft das Backend?");
            }
        });
        darkModeProperty.addListener((obs, oldValue, newValue) -> synchronisiereWebTheme());
    }

    public void ladeWennNoetig() {
        if (!geladen) ladeAdminseite();
        else aktualisiereProdukte();
    }

    public void navigiereZu(String ziel) {
        zielNachLaden = ziel;
        if (!geladen) {
            ladeAdminseite();
        } else if (istAdminseite()) {
            Object ausgefuehrt = webView.getEngine().executeScript(
                    "typeof window.focusAdminArea === 'function' && (window.focusAdminArea('" + ziel + "'), true)");
            if (Boolean.TRUE.equals(ausgefuehrt)) zielNachLaden = null;
        }
    }

    private void aktualisiereProdukte() {
        if (istAdminseite()) {
            webView.getEngine().executeScript("window.refreshProducts && window.refreshProducts()");
        }
    }

    private boolean istAdminseite() {
        String url = webView.getEngine().getLocation();
        return url != null && url.contains("/admin/");
    }

    private void ladeAdminseite() {
        geladen = true;
        webView.getEngine().load(erstelleSsoUrl());
        statusLabel.setVisible(true);
        statusLabel.setText(verwaltungsTitel() + " wird geöffnet …");
    }

    public void oeffneImBrowser() {
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

    private String verwaltungsTitel() {
        return benutzer.istLagerist() ? "Warenzugang" : "Verwaltung";
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
