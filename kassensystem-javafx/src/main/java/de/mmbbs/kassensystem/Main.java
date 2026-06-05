package de.mmbbs.kassensystem;

import de.mmbbs.kassensystem.repository.ProduktRepository;
import de.mmbbs.kassensystem.repository.SqlBonHistorieRepository;
import de.mmbbs.kassensystem.repository.SqlProduktRepository;
import de.mmbbs.kassensystem.service.KassenService;
import de.mmbbs.kassensystem.service.ProduktService;
import de.mmbbs.kassensystem.ui.AdminWebView;
import de.mmbbs.kassensystem.ui.KassenView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Startpunkt der JavaFX-Kassenanwendung.
 *
 * <p>Hier werden Repository, Services und die Haupttabs verbunden. Die Kasse
 * bleibt lokal in JavaFX, waehrend administrative Funktionen ueber den
 * Verwaltungstab an den Spring-Adminbereich angebunden sind.</p>
 */
public class Main extends Application {

    /**
     * Baut das Hauptfenster mit Kassen- und Verwaltungstab auf.
     *
     * @param stage Primaere JavaFX-Stage der Anwendung.
     */
    @Override
    public void start(Stage stage) {
        ProduktRepository repository = new SqlProduktRepository();
        ProduktService produktService = new ProduktService(repository);
        KassenService kassenService = new KassenService(repository, new SqlBonHistorieRepository(repository));

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab kasseTab = new Tab("Kasse", new KassenView(produktService, kassenService));
        Tab adminTab = new Tab("Verwaltung", new AdminWebView());

        tabPane.getTabs().addAll(kasseTab, adminTab);
        tabPane.getSelectionModel().select(0);

        Scene scene = new Scene(tabPane, 1200, 720);
        scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());

        Image icon = new Image(getClass().getResource("/Icon.png").toExternalForm());
        stage.getIcons().add(icon);

        stage.setTitle("Kassensystem MVP");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
