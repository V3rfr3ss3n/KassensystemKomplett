package de.mmbbs.kassensystem;

import de.mmbbs.kassensystem.auth.AuthService;
import de.mmbbs.kassensystem.auth.Benutzer;
import de.mmbbs.kassensystem.repository.ProduktRepository;
import de.mmbbs.kassensystem.repository.ApiClient;
import de.mmbbs.kassensystem.repository.ApiBonHistorieRepository;
import de.mmbbs.kassensystem.repository.ApiProduktRepository;
import de.mmbbs.kassensystem.service.KassenService;
import de.mmbbs.kassensystem.service.ProduktService;
import de.mmbbs.kassensystem.ui.AdminWebView;
import de.mmbbs.kassensystem.ui.HauptmenuView;
import de.mmbbs.kassensystem.ui.KassenView;
import de.mmbbs.kassensystem.ui.LoginDialog;
import de.mmbbs.kassensystem.ui.ThemeManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.MenuItem;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.geometry.Side;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.util.Optional;

/**
 * Startpunkt der JavaFX-Kassenanwendung.
 *
 * <p>Hier werden Anmeldung, Repository, Services und die rollenabhaengigen
 * Hauptansichten verbunden. Kassierer landen direkt in der Kasse; Admins sehen
 * das Hauptmenue und die Verwaltung.</p>
 */
public class Main extends Application {
    private final AuthService authService = new AuthService();
    private Image hellesIcon;
    private Image dunklesIcon;
    private Stage primaryStage;
    private AdminWebView verwaltungAnsicht;

    /**
     * Baut das Hauptfenster nach erfolgreicher Anmeldung auf.
     *
     * @param stage Primaere JavaFX-Stage der Anwendung.
     */
    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        hellesIcon = new Image(getClass().getResource("/Icon.png").toExternalForm());
        dunklesIcon = erstelleDunklesIcon(hellesIcon);

        aktualisiereFensterIcon();
        ThemeManager.darkModeProperty().addListener((obs, vorher, aktuell) -> aktualisiereFensterIcon());

        zeigeLogin();
    }

    private void aktualisiereFensterIcon() {
        primaryStage.getIcons().setAll(ThemeManager.isDarkMode() ? dunklesIcon : hellesIcon);
    }

    private static Image erstelleDunklesIcon(Image quelle) {
        int breite = (int) quelle.getWidth();
        int hoehe = (int) quelle.getHeight();
        WritableImage bild = new WritableImage(breite, hoehe);
        PixelReader leser = quelle.getPixelReader();
        PixelWriter schreiber = bild.getPixelWriter();
        Color hintergrund = Color.web("#172033");
        Color kontur = Color.web("#e5e7eb");
        for (int y = 0; y < hoehe; y++) {
            for (int x = 0; x < breite; x++) {
                Color original = leser.getColor(x, y);
                double staerke = 1 - (original.getRed() + original.getGreen() + original.getBlue()) / 3;
                schreiber.setColor(x, y, hintergrund.interpolate(kontur, staerke));
            }
        }
        return bild;
    }

    private void zeigeLogin() {
        Optional<LoginDialog.Anmeldung> angemeldeterBenutzer = new LoginDialog(authService).showAndWait();
        if (angemeldeterBenutzer.isEmpty()) {
            Platform.exit();
            return;
        }

        LoginDialog.Anmeldung anmeldung = angemeldeterBenutzer.get();
        try {
            zeigeAnwendung(anmeldung.benutzer(), anmeldung.passwort());
        } catch (RuntimeException fehler) {
            de.mmbbs.kassensystem.ui.AlertUtil.showError("Backend nicht erreichbar", fehler.getMessage());
            zeigeLogin();
        }
    }

    private void zeigeAnwendung(Benutzer benutzer, String passwort) {
        ApiClient api = new ApiClient(benutzer.benutzername(), passwort);
        api.get("/api/session");
        ProduktRepository repository = new ApiProduktRepository(api);
        ProduktService produktService = new ProduktService(repository);
        KassenService kassenService = benutzer.darfKassieren()
                ? new KassenService(repository, new ApiBonHistorieRepository(api)) : null;

        Parent root = erstelleAppShell(benutzer, produktService, kassenService);
        ThemeManager.applyToRoot(root);

        Scene scene = new Scene(root, 1200, 720);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        String build = System.getProperty("kassensystem.build", "").trim();
        primaryStage.setTitle("Kassensystem – " + benutzer.benutzername()
                + (build.isEmpty() ? "" : " · " + build));
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private BorderPane erstelleAppShell(Benutzer benutzer,
                                        ProduktService produktService,
                                        KassenService kassenService) {
        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("app-shell");
        shell.setCenter(erstelleAnsicht(benutzer, produktService, kassenService));
        shell.setTop(erstelleKopfzeile(benutzer));
        return shell;
    }

    private HBox erstelleKopfzeile(Benutzer benutzer) {
        Label appLabel = new Label("Kassensystem");
        appLabel.getStyleClass().add("app-title-label");

        Label roleLabel = new Label(benutzer.benutzername() + " - " + benutzer.rolle().getAnzeigename());
        roleLabel.getStyleClass().add("role-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        CheckMenuItem darkModeEintrag = new CheckMenuItem("Dunkelmodus");
        darkModeEintrag.setSelected(ThemeManager.isDarkMode());
        darkModeEintrag.selectedProperty().addListener((obs, vorher, aktuell) -> ThemeManager.setDarkMode(aktuell));
        MenuItem logoutEintrag = new MenuItem("Abmelden");
        logoutEintrag.setOnAction(event -> logout());
        ContextMenu einstellungsMenue = new ContextMenu(darkModeEintrag, logoutEintrag);
        einstellungsMenue.getStyleClass().add("settings-popup");
        String stylesheet = getClass().getResource("/styles.css").toExternalForm();
        einstellungsMenue.setOnShowing(event -> {
            if (!einstellungsMenue.getScene().getStylesheets().contains(stylesheet)) {
                einstellungsMenue.getScene().getStylesheets().add(stylesheet);
            }
        });
        if (ThemeManager.isDarkMode()) einstellungsMenue.getStyleClass().add("dark-mode");
        ThemeManager.darkModeProperty().addListener((obs, vorher, aktuell) -> {
            einstellungsMenue.getStyleClass().remove("dark-mode");
            if (aktuell) einstellungsMenue.getStyleClass().add("dark-mode");
        });
        Button einstellungen = new Button("⚙");
        einstellungen.setTooltip(new Tooltip("Einstellungen"));
        einstellungen.setAccessibleText("Einstellungen");
        einstellungen.setOnAction(event -> {
            if (einstellungsMenue.isShowing()) einstellungsMenue.hide();
            else einstellungsMenue.show(einstellungen, Side.BOTTOM, 0, 4);
        });
        einstellungen.getStyleClass().addAll("secondary-button", "header-icon-button");

        HBox kopfzeile = new HBox(8, appLabel, roleLabel, spacer);
        if (verwaltungAnsicht != null) {
            Button browser = new Button("🌐");
            browser.setTooltip(new Tooltip("Verwaltung im Browser öffnen"));
            browser.setAccessibleText("Verwaltung im Browser öffnen");
            browser.setOnAction(event -> verwaltungAnsicht.oeffneImBrowser());
            browser.getStyleClass().addAll("secondary-button", "header-icon-button");
            kopfzeile.getChildren().add(browser);
        }
        kopfzeile.getChildren().add(einstellungen);
        kopfzeile.getStyleClass().add("app-topbar");
        kopfzeile.setAlignment(Pos.CENTER_LEFT);
        kopfzeile.setPadding(new Insets(10, 16, 10, 16));
        return kopfzeile;
    }

    private void logout() {
        primaryStage.hide();
        zeigeLogin();
    }

    private TabPane erstelleAnsicht(Benutzer benutzer,
                                    ProduktService produktService,
                                    KassenService kassenService) {
        verwaltungAnsicht = null;
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab kasseTab = null;
        if (benutzer.darfKassieren()) {
            KassenView kasse = new KassenView(produktService, kassenService);
            kasseTab = new Tab("Kasse", kasse);
            kasseTab.selectedProperty().addListener((obs, vorher, ausgewaehlt) -> {
                if (ausgewaehlt) kasse.aktualisiereDaten();
            });
        }

        Tab verwaltungTab = null;
        AdminWebView verwaltung = null;
        if (benutzer.darfWebVerwaltungNutzen()) {
            String label = benutzer.istLagerist() ? "Warenzugang" : "Verwaltung";
            verwaltung = new AdminWebView(benutzer, ThemeManager.darkModeProperty());
            verwaltungAnsicht = verwaltung;
            verwaltungTab = new Tab(label, verwaltung);
            AdminWebView verwaltungAnsicht = verwaltung;
            verwaltungTab.selectedProperty().addListener((obs, oldValue, selected) -> {
                if (selected) verwaltungAnsicht.ladeWennNoetig();
            });
        }

        if (benutzer.istAdmin() && kasseTab != null && verwaltungTab != null) {
            Tab finalKasseTab = kasseTab;
            Tab finalVerwaltungTab = verwaltungTab;
            AdminWebView finalVerwaltung = verwaltung;
            Tab menuTab = new Tab("Hauptmenü", new HauptmenuView(
                    benutzer,
                    () -> tabPane.getSelectionModel().select(finalKasseTab),
                    () -> { tabPane.getSelectionModel().select(finalVerwaltungTab); finalVerwaltung.navigiereZu("new"); },
                    () -> { tabPane.getSelectionModel().select(finalVerwaltungTab); finalVerwaltung.navigiereZu("stock"); },
                    () -> { tabPane.getSelectionModel().select(finalVerwaltungTab); finalVerwaltung.navigiereZu("inventory"); }
            ));

            tabPane.getTabs().addAll(menuTab, kasseTab, verwaltungTab);
            tabPane.getSelectionModel().select(menuTab);
            return tabPane;
        }

        if (kasseTab != null) {
            tabPane.getTabs().add(kasseTab);
        }
        if (verwaltungTab != null) {
            tabPane.getTabs().add(verwaltungTab);
        }
        if (!tabPane.getTabs().isEmpty()) {
            tabPane.getSelectionModel().selectFirst();
            if (verwaltungTab != null && tabPane.getSelectionModel().getSelectedItem() == verwaltungTab) {
                ((AdminWebView) verwaltungTab.getContent()).ladeWennNoetig();
            }
        }
        return tabPane;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
