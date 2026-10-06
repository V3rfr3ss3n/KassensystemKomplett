package de.mmbbs.kassensystem.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mmbbs.kassensystem.auth.Benutzer;
import de.mmbbs.kassensystem.repository.ApiClient;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.concurrent.Worker;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import netscape.javascript.JSObject;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

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
    private final ApiClient api;
    private final ReadOnlyBooleanProperty darkModeProperty;
    private final WebView webView = new WebView();
    private final Label statusLabel = new Label("Verwaltung wird geladen …");
    private boolean geladen;
    private String zielNachLaden;
    private final AdminBridge adminBridge = new AdminBridge();

    public AdminWebView(Benutzer benutzer, ApiClient api, ReadOnlyBooleanProperty darkModeProperty) {
        this.benutzer = benutzer;
        this.api = api;
        this.darkModeProperty = darkModeProperty;

        getStyleClass().add("admin-web-view");
        setPadding(new Insets(18));

        statusLabel.setPadding(new Insets(0, 0, 12, 0));
        statusLabel.managedProperty().bind(statusLabel.visibleProperty());
        setTop(statusLabel);
        setCenter(webView);

        webView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, state) -> {
            if (state == Worker.State.SUCCEEDED) {
                if (istAdminseite()) {
                    JSObject fenster = (JSObject) webView.getEngine().executeScript("window");
                    fenster.setMember("kassensystemBridge", adminBridge);
                    warteAufAdminSkripte(0);
                }
            } else if (state == Worker.State.FAILED) {
                geladen = false;
                statusLabel.setVisible(true);
                statusLabel.setText("Verwaltung konnte nicht geladen werden. Läuft das Backend?");
            }
        });
        darkModeProperty.addListener((obs, oldValue, newValue) -> synchronisiereWebTheme());
    }

    private void warteAufAdminSkripte(int versuch) {
        if (!istAdminseite()) return;
        Object bereit = webView.getEngine().executeScript(
                "typeof window.focusAdminArea === 'function' && typeof window.refreshProducts === 'function'");
        if (Boolean.TRUE.equals(bereit)) {
            synchronisiereWebTheme();
            statusLabel.setText("Verwaltung geladen für " + benutzer.benutzername() + ".");
            statusLabel.setVisible(false);
            webView.getEngine().executeScript("document.querySelector('.settings-menu').hidden = true");
            if (zielNachLaden != null) navigiereZu(zielNachLaden);
            else aktualisiereProdukte();
            return;
        }
        if (versuch >= 100) {
            geladen = false;
            statusLabel.setText("Verwaltungsskripte konnten nicht geladen werden.");
            statusLabel.setVisible(true);
            return;
        }
        PauseTransition pause = new PauseTransition(Duration.millis(100));
        pause.setOnFinished(event -> warteAufAdminSkripte(versuch + 1));
        pause.play();
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
        if (url == null) return false;
        try {
            URI aktuell = URI.create(url);
            URI erlaubt = URI.create(ADMIN_URL);
            return aktuell.getScheme().equalsIgnoreCase(erlaubt.getScheme())
                    && aktuell.getHost().equalsIgnoreCase(erlaubt.getHost())
                    && aktuell.getPort() == erlaubt.getPort()
                    && aktuell.getPath().startsWith(erlaubt.getPath());
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private void ladeAdminseite() {
        try {
            String url = erstelleSsoUrl();
            geladen = true;
            webView.getEngine().load(url);
            statusLabel.setVisible(true);
            statusLabel.setText(verwaltungsTitel() + " wird geöffnet …");
        } catch (RuntimeException ex) {
            geladen = false;
            statusLabel.setVisible(true);
            statusLabel.setText("Verwaltung konnte nicht geöffnet werden: " + ex.getMessage());
        }
    }

    public void oeffneImBrowser() {
        oeffneImBrowser(false);
    }

    private void oeffneImBrowser(boolean scannerStarten) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(erstelleSsoUrl(scannerStarten)));
            } else {
                AlertUtil.showInfo("Adminbereich", "Admin-Webadresse: " + ADMIN_URL);
            }
        } catch (Exception ex) {
            AlertUtil.showWarning("Adminbereich", "Admin-Webadresse: " + ADMIN_URL);
        }
    }

    private String erstelleSsoUrl() {
        return erstelleSsoUrl(false);
    }

    private String erstelleSsoUrl(boolean scannerStarten) {
        String ticket = URLEncoder.encode(api.post("/api/auth/browser-ticket", null).path("ticket").asText(), StandardCharsets.UTF_8);
        String theme = darkModeProperty.get() ? "dark" : "light";
        return SSO_LOGIN_URL + "?ticket=" + ticket + "&theme=" + theme
                + (scannerStarten ? "&scan=true" : "");
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

    public final class AdminBridge {
        public void openScannerInBrowser() {
            if (!istAdminseite()) return;
            Platform.runLater(() -> oeffneImBrowser(true));
        }

        public void saveLabels(String idsJson) {
            try {
                if (!istAdminseite()) throw new IllegalStateException("Etiketten sind nur in der Verwaltung verfügbar.");
                var wurzel = new ObjectMapper().readTree(idsJson);
                if (!wurzel.isArray() || wurzel.isEmpty() || wurzel.size() > 1000) {
                    throw new IllegalArgumentException("Ungültige Produktauswahl.");
                }
                List<Integer> ids = new java.util.ArrayList<>();
                for (var eintrag : wurzel) {
                    if (!eintrag.isIntegralNumber() || eintrag.asInt() <= 0) throw new IllegalArgumentException("Ungültige Produkt-ID.");
                    ids.add(eintrag.asInt());
                }
                Platform.runLater(() -> {
                    FileChooser dialog = new FileChooser();
                    dialog.setTitle("QR-Etiketten speichern");
                    dialog.setInitialFileName("qr-etiketten.pdf");
                    dialog.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
                    var datei = dialog.showSaveDialog(getScene().getWindow());
                    if (datei == null) return;
                    Thread.ofVirtual().name("qr-etiketten-download").start(() -> {
                        try {
                            byte[] pdf = api.postBytes("/api/produkte/etiketten", Map.of("produktIds", ids));
                            Files.write(datei.toPath(), pdf);
                            Platform.runLater(() -> AlertUtil.showInfo("QR-Etiketten", "PDF wurde gespeichert."));
                        } catch (Exception ex) {
                            Platform.runLater(() -> AlertUtil.showError("QR-Etiketten", ex.getMessage()));
                        }
                    });
                });
            } catch (Exception ex) {
                Platform.runLater(() -> AlertUtil.showError("QR-Etiketten", ex.getMessage()));
            }
        }
    }
}
