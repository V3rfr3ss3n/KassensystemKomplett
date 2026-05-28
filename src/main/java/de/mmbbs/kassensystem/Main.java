package de.mmbbs.kassensystem;

import de.mmbbs.kassensystem.repository.InMemoryProduktRepository;
import de.mmbbs.kassensystem.service.KassenService;
import de.mmbbs.kassensystem.service.ProduktService;
import de.mmbbs.kassensystem.ui.KassenView;
import de.mmbbs.kassensystem.ui.LagerView;
import de.mmbbs.kassensystem.ui.MainView;
import de.mmbbs.kassensystem.ui.ProduktFormView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        InMemoryProduktRepository repository = new InMemoryProduktRepository();
        ProduktService produktService = new ProduktService(repository);
        KassenService kassenService = new KassenService(repository);

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab startTab = new Tab("Start", new MainView(produktService));
        Tab kasseTab = new Tab("Kasse", new KassenView(produktService, kassenService));
        Tab produktTab = new Tab("Produkt hinzufügen", new ProduktFormView(produktService));
        Tab lagerTab = new Tab("Lagerbestand", new LagerView(produktService));

        tabPane.getTabs().addAll(startTab, kasseTab, produktTab, lagerTab);

        Scene scene = new Scene(tabPane, 900, 600);
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
