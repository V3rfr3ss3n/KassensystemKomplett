package de.mmbbs.kassensystem.ui;

import com.sun.net.httpserver.HttpServer;
import de.mmbbs.kassensystem.auth.Benutzer;
import de.mmbbs.kassensystem.auth.BenutzerRolle;
import de.mmbbs.kassensystem.repository.ApiClient;
import de.mmbbs.kassensystem.repository.ProduktRepository;
import de.mmbbs.kassensystem.repository.KaufRepository;
import de.mmbbs.kassensystem.model.Produkt;
import de.mmbbs.kassensystem.model.Verkaufseinheit;
import de.mmbbs.kassensystem.model.Bon;
import de.mmbbs.kassensystem.service.ProduktService;
import de.mmbbs.kassensystem.service.KassenService;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** Lokaler Windows-Smoke-Test für ES-Module und JavaScript-Bridge in JavaFX WebView. */
@EnabledIfSystemProperty(named = "kassensystem.gui.smoke", matches = "true")
class AdminWebViewSmokeTest {
    @Test
    void adminModuleUndBridgeLadenInWebView() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        Path statisch = Path.of("../kassensystem-backend/src/main/resources/static/admin");
        server.createContext("/", austausch -> {
            String pfad = austausch.getRequestURI().getPath();
            byte[] antwort;
            String typ = "application/json";
            if (pfad.equals("/api/auth/browser-ticket")) antwort = "{\"ticket\":\"test\"}".getBytes(StandardCharsets.UTF_8);
            else if (pfad.equals("/auth/javafx-login")) {
                austausch.getResponseHeaders().add("Location", "/admin/");
                austausch.sendResponseHeaders(302, -1);
                austausch.close();
                return;
            } else if (pfad.equals("/api/session")) antwort = "{\"username\":\"admin\",\"roles\":[\"ADMIN\"],\"permissions\":{\"products.read\":true,\"manageProducts\":true,\"bookStock\":true,\"users.manage\":false},\"csrfToken\":\"test\"}".getBytes(StandardCharsets.UTF_8);
            else if (pfad.equals("/api/produkte")) antwort = "[]".getBytes(StandardCharsets.UTF_8);
            else {
                String datei = pfad.equals("/admin/") ? "index.html" : pfad.substring("/admin/".length());
                antwort = Files.readAllBytes(statisch.resolve(datei));
                typ = datei.endsWith(".js") ? "text/javascript" : datei.endsWith(".css") ? "text/css" : "text/html";
            }
            austausch.getResponseHeaders().add("Content-Type", typ + "; charset=utf-8");
            austausch.sendResponseHeaders(200, antwort.length);
            austausch.getResponseBody().write(antwort);
            austausch.close();
        });
        server.start();
        String alt = System.getProperty("kassensystem.api.url");
        System.setProperty("kassensystem.api.url", "http://127.0.0.1:" + server.getAddress().getPort());
        CountDownLatch geladen = new CountDownLatch(1);
        AtomicReference<String> ergebnis = new AtomicReference<>();
        AtomicReference<Stage> fenster = new AtomicReference<>();
        try {
            Platform.startup(() -> {
                try {
                AdminWebView verwaltung = new AdminWebView(new Benutzer("admin", BenutzerRolle.ADMIN),
                        new ApiClient("admin", "test"), new SimpleBooleanProperty(false));
                Stage stage = new Stage();
                stage.setOpacity(0);
                stage.setScene(new Scene(verwaltung, 800, 600));
                stage.show();
                fenster.set(stage);
                WebView web = (WebView) verwaltung.getCenter();
                web.getEngine().getLoadWorker().stateProperty().addListener((obs, altZustand, zustand) -> {
                    ergebnis.set(zustand + " " + web.getEngine().getLocation());
                    if (zustand == Worker.State.FAILED) {
                        ergebnis.set("FAILED " + web.getEngine().getLocation() + " " + web.getEngine().getLoadWorker().getException());
                        geladen.countDown();
                    }
                    if (zustand == Worker.State.SUCCEEDED && web.getEngine().getLocation().contains("/admin/")) {
                        PauseTransition pause = new PauseTransition(Duration.seconds(2));
                        pause.setOnFinished(event -> {
                            ergebnis.set(String.valueOf(web.getEngine().executeScript(
                                    "typeof window.refreshProducts + ':' + typeof window.kassensystemBridge.saveLabels" +
                                    " + ':' + typeof window.kassensystemBridge.openScannerInBrowser")));
                            geladen.countDown();
                        });
                        pause.play();
                    }
                });
                verwaltung.ladeWennNoetig();
                } catch (Throwable ex) {
                    ergebnis.set(ex.toString());
                    geladen.countDown();
                }
            });
            assertTrue(geladen.await(20, TimeUnit.SECONDS), "WebView lud die Verwaltung nicht: " + ergebnis.get());
            assertEquals("function:function:function", ergebnis.get());
            pruefeScanner(fenster.get());
        } finally {
            Platform.runLater(() -> { if (fenster.get() != null) fenster.get().close(); });
            Platform.exit();
            server.stop(0);
            if (alt == null) System.clearProperty("kassensystem.api.url");
            else System.setProperty("kassensystem.api.url", alt);
        }
    }

    private void pruefeScanner(Stage stage) throws Exception {
        CountDownLatch geprueft = new CountDownLatch(1);
        AtomicReference<Throwable> fehler = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                Produkt stueck = new Produkt(1, "Apfel", 1, 5);
                stueck.setScanCode("100");
                Produkt gewicht = new Produkt(2, "Kartoffeln", 2, 5, null, Verkaufseinheit.KILOGRAMM, 7);
                gewicht.setScanCode("200");
                ProduktRepository katalog = new ProduktRepository() {
                    public List<Produkt> findeAlle() { return List.of(stueck, gewicht); }
                    public Optional<Produkt> findeNachId(int id) { return findeAlle().stream().filter(p -> p.getId() == id).findFirst(); }
                };
                KaufRepository kaufe = new KaufRepository() {
                    public List<Bon> ladeBonHistorie() { return List.of(); }
                    public Bon schliesseKaufAb(Map<Integer, Double> mengen) { return new Bon(1, List.of()); }
                };
                KassenService service = new KassenService(katalog, kaufe);
                KassenView kasse = new KassenView(new ProduktService(katalog), service);
                stage.setScene(new Scene(kasse, 1200, 720));
                var feld = KassenView.class.getDeclaredField("scanner");
                feld.setAccessible(true);
                ScannerEingabe scanner = (ScannerEingabe) feld.get(kasse);
                TextField eingabe = (TextField) scanner.getChildren().get(1);
                for (int i = 0; i < 2; i++) {
                    eingabe.setText("100");
                    eingabe.fireEvent(new ActionEvent());
                }
                assertEquals(2, service.getWarenkorb().getFirst().getMenge());
                eingabe.setText("200");
                eingabe.fireEvent(new ActionEvent());
                assertEquals(1, service.getWarenkorb().size());
                assertEquals("", eingabe.getText());
            } catch (Throwable ex) {
                fehler.set(ex);
            } finally {
                geprueft.countDown();
            }
        });
        assertTrue(geprueft.await(10, TimeUnit.SECONDS), "Scanner-Smoke-Test blieb hängen.");
        if (fehler.get() != null) throw new AssertionError("Scanner-Smoke-Test fehlgeschlagen", fehler.get());
    }
}
