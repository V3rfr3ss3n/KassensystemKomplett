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
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
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
    private Stage primaryStage;

    /**
     * Baut das Hauptfenster nach erfolgreicher Anmeldung auf.
     *
     * @param stage Primaere JavaFX-Stage der Anwendung.
     */
    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        Image icon = new Image(getClass().getResource("/Icon.png").toExternalForm());
        stage.getIcons().add(icon);

        zeigeLogin();
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

        primaryStage.setTitle("Kassensystem MVP - " + benutzer.benutzername());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private BorderPane erstelleAppShell(Benutzer benutzer,
                                        ProduktService produktService,
                                        KassenService kassenService) {
        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("app-shell");
        shell.setTop(erstelleKopfzeile(benutzer));
        shell.setCenter(erstelleAnsicht(benutzer, produktService, kassenService));
        return shell;
    }

    private HBox erstelleKopfzeile(Benutzer benutzer) {
        Label appLabel = new Label("Kassensystem");
        appLabel.getStyleClass().add("app-title-label");

        Label roleLabel = new Label(benutzer.benutzername() + " - " + benutzer.rolle().getAnzeigename());
        roleLabel.getStyleClass().add("role-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        ToggleButton darkModeButton = new ToggleButton(ThemeManager.isDarkMode() ? "Hellmodus" : "Darkmode");
        darkModeButton.getStyleClass().add("secondary-button");
        darkModeButton.setSelected(ThemeManager.isDarkMode());
        darkModeButton.selectedProperty().addListener((obs, oldValue, selected) -> {
            ThemeManager.setDarkMode(selected);
            darkModeButton.setText(selected ? "Hellmodus" : "Darkmode");
        });

        Button logoutButton = new Button("Ausloggen");
        logoutButton.getStyleClass().add("secondary-button");
        logoutButton.setOnAction(event -> logout());

        Button beendenButton = new Button("Beenden");
        beendenButton.getStyleClass().add("danger-button");
        beendenButton.setOnAction(event -> primaryStage.close());

        HBox kopfzeile = new HBox(12, appLabel, roleLabel, spacer, darkModeButton, logoutButton, beendenButton);
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
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab kasseTab = null;
        if (benutzer.darfKassieren()) {
            kasseTab = new Tab("Kasse", new KassenView(produktService, kassenService));
        }

        Tab verwaltungTab = null;
        if (benutzer.darfWebVerwaltungNutzen()) {
            String label = benutzer.istLagerist() ? "Warenzugang" : "Verwaltung";
            verwaltungTab = new Tab(label, new AdminWebView(benutzer, ThemeManager.darkModeProperty()));
        }

        if (benutzer.istAdmin() && kasseTab != null && verwaltungTab != null) {
            Tab finalKasseTab = kasseTab;
            Tab finalVerwaltungTab = verwaltungTab;
            Tab menuTab = new Tab("Hauptmenue", new HauptmenuView(
                    benutzer,
                    () -> tabPane.getSelectionModel().select(finalKasseTab),
                    () -> tabPane.getSelectionModel().select(finalVerwaltungTab),
                    primaryStage::close
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
        }
        return tabPane;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
