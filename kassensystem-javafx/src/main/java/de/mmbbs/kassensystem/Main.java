package de.mmbbs.kassensystem;

import de.mmbbs.kassensystem.auth.AuthService;
import de.mmbbs.kassensystem.auth.Benutzer;
import de.mmbbs.kassensystem.repository.ProduktRepository;
import de.mmbbs.kassensystem.repository.SqlBonHistorieRepository;
import de.mmbbs.kassensystem.repository.SqlProduktRepository;
import de.mmbbs.kassensystem.service.KassenService;
import de.mmbbs.kassensystem.service.ProduktService;
import de.mmbbs.kassensystem.ui.AdminWebView;
import de.mmbbs.kassensystem.ui.HauptmenuView;
import de.mmbbs.kassensystem.ui.KassenView;
import de.mmbbs.kassensystem.ui.LoginDialog;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
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

    /**
     * Baut das Hauptfenster nach erfolgreicher Anmeldung auf.
     *
     * @param stage Primaere JavaFX-Stage der Anwendung.
     */
    @Override
    public void start(Stage stage) {
        Optional<Benutzer> angemeldeterBenutzer = new LoginDialog(new AuthService()).showAndWait();
        if (angemeldeterBenutzer.isEmpty()) {
            Platform.exit();
            return;
        }

        Benutzer benutzer = angemeldeterBenutzer.get();
        ProduktRepository repository = new SqlProduktRepository();
        ProduktService produktService = new ProduktService(repository);
        KassenService kassenService = new KassenService(repository, new SqlBonHistorieRepository(repository));

        Scene scene = new Scene(erstelleAnsicht(stage, benutzer, produktService, kassenService), 1200, 720);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        Image icon = new Image(getClass().getResource("/Icon.png").toExternalForm());
        stage.getIcons().add(icon);

        stage.setTitle("Kassensystem MVP - " + benutzer.benutzername());
        stage.setScene(scene);
        stage.show();
    }

    private TabPane erstelleAnsicht(Stage stage, Benutzer benutzer,
                                    ProduktService produktService,
                                    KassenService kassenService) {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab kasseTab = new Tab("Kasse", new KassenView(produktService, kassenService));

        if (!benutzer.istAdmin()) {
            tabPane.getTabs().add(kasseTab);
            return tabPane;
        }

        Tab adminTab = new Tab("Verwaltung", new AdminWebView());
        Tab menuTab = new Tab("Hauptmenue", new HauptmenuView(
                benutzer,
                () -> tabPane.getSelectionModel().select(kasseTab),
                () -> tabPane.getSelectionModel().select(adminTab),
                stage::close
        ));

        tabPane.getTabs().addAll(menuTab, kasseTab, adminTab);
        tabPane.getSelectionModel().select(menuTab);
        return tabPane;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
