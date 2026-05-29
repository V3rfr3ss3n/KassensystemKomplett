package de.mmbbs.kassensystem.ui;

import de.mmbbs.kassensystem.model.Produkt;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Locale;

final class ProduktTableHelper {
    private ProduktTableHelper() {
    }

    static final class FilterFields {
        private final TextField idFilter = createFilterField("ID");
        private final TextField nameFilter = createFilterField("Name");
        private final TextField preisFilter = createFilterField("Preis");
        private final TextField lagerFilter = createFilterField("Lager");

        FilterFields(Runnable refreshAction) {
            allFields().forEach(field -> field.textProperty()
                    .addListener((obs, oldValue, newValue) -> refreshAction.run()));
        }

        boolean matches(Produkt produkt) {
            return matches(produkt, "");
        }

        boolean matches(Produkt produkt, String globalFilter) {
            return matchesGlobal(produkt, globalFilter)
                    && enthaelt(String.valueOf(produkt.getId()), idFilter.getText())
                    && enthaelt(produkt.getName(), nameFilter.getText())
                    && enthaelt(preisSuchtext(produkt), preisFilter.getText())
                    && enthaelt(String.valueOf(produkt.getLagerbestand()), lagerFilter.getText());
        }

        private List<TextField> allFields() {
            return List.of(idFilter, nameFilter, preisFilter, lagerFilter);
        }
    }

    static TableColumn<Produkt, String> bildColumn() {
        TableColumn<Produkt, String> column = new TableColumn<>("Bild");
        column.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBildPfad()));
        column.setCellFactory(col -> new TableCell<>() {
            private final ImageView imageView = new ImageView();

            {
                imageView.setFitWidth(48);
                imageView.setFitHeight(48);
                imageView.setPreserveRatio(true);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }

                Produkt produkt = getTableView().getItems().get(getIndex());
                Image image = ladeBild(produkt);
                imageView.setImage(image);
                setGraphic(imageView);
            }
        });
        column.setPrefWidth(68);
        return column;
    }

    static TableColumn<Produkt, Number> idColumn(FilterFields filters) {
        TableColumn<Produkt, Number> column = new TableColumn<>();
        column.setGraphic(header("ID", filters.idFilter));
        column.setCellValueFactory(data -> data.getValue().getIdProperty());
        column.setPrefWidth(80);
        return column;
    }

    static TableColumn<Produkt, String> nameColumn(FilterFields filters) {
        TableColumn<Produkt, String> column = new TableColumn<>();
        column.setGraphic(header("Name", filters.nameFilter));
        column.setCellValueFactory(data -> data.getValue().nameProperty());
        column.setPrefWidth(180);
        return column;
    }

    static TableColumn<Produkt, Number> preisColumn(FilterFields filters) {
        TableColumn<Produkt, Number> column = new TableColumn<>();
        column.setGraphic(header("Preis", filters.preisFilter));
        column.setCellValueFactory(data -> data.getValue().preisProperty());
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number preis, boolean empty) {
                super.updateItem(preis, empty);
                setText(empty || preis == null ? null : String.format(Locale.GERMAN, "%.2f \u20ac", preis.doubleValue()));
            }
        });
        column.setPrefWidth(110);
        return column;
    }

    static TableColumn<Produkt, Number> lagerColumn(FilterFields filters) {
        TableColumn<Produkt, Number> column = new TableColumn<>();
        column.setGraphic(header("Lager", filters.lagerFilter));
        column.setCellValueFactory(data -> data.getValue().lagerbestandProperty());
        column.setPrefWidth(100);
        return column;
    }

    private static Image ladeBild(Produkt produkt) {
        String bildPfad = produkt.getBildPfad();
        if (bildPfad != null && !bildPfad.isBlank()) {
            java.io.File file = new java.io.File(bildPfad);
            if (file.exists() && file.isFile()) {
                return new Image(file.toURI().toString(), true);
            }
        }

        return new Image("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAAXNSR0IArs4c6QAAAAERFTkSuQmCC", true);
    }

    private static VBox header(String title, TextField filterField) {
        Label label = new Label(title);
        label.setStyle("-fx-font-weight: bold;");

        VBox headerBox = new VBox(4, label, filterField);
        headerBox.setPadding(new Insets(2, 0, 2, 0));
        return headerBox;
    }

    private static TextField createFilterField(String prompt) {
        TextField field = new TextField();
        field.setPromptText("Filter " + prompt);
        field.getStyleClass().add("column-filter-field");
        field.setMaxWidth(Double.MAX_VALUE);
        field.setFocusTraversable(true);
        return field;
    }

    private static boolean matchesGlobal(Produkt produkt, String globalFilter) {
        if (istLeer(globalFilter)) {
            return true;
        }

        String globalSuchtext = String.join(" ",
                String.valueOf(produkt.getId()),
                produkt.getName(),
                preisSuchtext(produkt),
                String.valueOf(produkt.getLagerbestand()));
        return enthaelt(globalSuchtext, globalFilter);
    }

    private static boolean enthaelt(String wert, String filter) {
        if (istLeer(filter)) {
            return true;
        }
        return normalisiere(wert).contains(normalisiere(filter));
    }

    private static boolean istLeer(String text) {
        return text == null || text.isBlank();
    }

    private static String normalisiere(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
    }

    private static String preisSuchtext(Produkt produkt) {
        return String.format(Locale.GERMAN, "%.2f EUR %.2f", produkt.getPreis(), produkt.getPreis());
    }
}
