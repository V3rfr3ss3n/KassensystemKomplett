package de.mmbbs.kassensystem.ui;

import de.mmbbs.kassensystem.util.ImageProcessor;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.Window;

import java.io.File;
import java.util.Optional;

final class ImageCropDialog {
    private static final double PREVIEW_SIZE = 320;
    private static final double MAX_ZOOM = 4.0;

    private ImageCropDialog() {
    }

    static Optional<ImageProcessor.CropArea> show(Window owner, File imageFile) {
        Image image = new Image(imageFile.toURI().toString(), false);
        if (image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0) {
            AlertUtil.showError("Bild konnte nicht geladen werden",
                    "Die ausgewählte Datei konnte nicht als Bild geöffnet werden.");
            return Optional.empty();
        }

        CropEditor editor = new CropEditor(image);
        ButtonType applyButton = new ButtonType("Uebernehmen", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);

        Dialog<ImageProcessor.CropArea> dialog = new Dialog<>();
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setTitle("Bild zuschneiden");
        dialog.setHeaderText("Ausschnitt festlegen");
        dialog.getDialogPane().setContent(editor.createContent());
        dialog.getDialogPane().getButtonTypes().addAll(applyButton, cancelButton);
        dialog.getDialogPane().setPrefWidth(420);

        Node applyNode = dialog.getDialogPane().lookupButton(applyButton);
        applyNode.getStyleClass().add("primary-button");
        Node cancelNode = dialog.getDialogPane().lookupButton(cancelButton);
        cancelNode.getStyleClass().add("secondary-button");

        dialog.setResultConverter(button -> button == applyButton ? editor.getCropArea() : null);
        return dialog.showAndWait();
    }

    private static final class CropEditor {
        private final Image image;
        private final ImageView imageView;
        private final Slider zoomSlider;
        private final StackPane cropPane;
        private final double minScale;
        private double dragStartX;
        private double dragStartY;
        private double startTranslateX;
        private double startTranslateY;

        private CropEditor(Image image) {
            this.image = image;
            this.imageView = new ImageView(image);
            this.imageView.setPreserveRatio(false);
            this.imageView.setSmooth(true);
            this.minScale = Math.max(PREVIEW_SIZE / image.getWidth(), PREVIEW_SIZE / image.getHeight());

            this.zoomSlider = new Slider(1.0, MAX_ZOOM, 1.0);
            this.zoomSlider.setBlockIncrement(0.1);
            this.zoomSlider.setMajorTickUnit(1.0);
            this.zoomSlider.setShowTickMarks(true);
            this.zoomSlider.valueProperty().addListener((obs, oldValue, newValue) -> updateImageSize());

            this.cropPane = createCropPane();
            updateImageSize();
        }

        private Node createContent() {
            Label zoomLabel = new Label("Zoom");
            zoomLabel.getStyleClass().add("subtitle-label");

            Button centerButton = new Button("Zentrieren");
            centerButton.getStyleClass().add("secondary-button");
            centerButton.setOnAction(event -> resetImage());

            HBox zoomControls = new HBox(10, zoomLabel, zoomSlider, centerButton);
            zoomControls.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(zoomSlider, Priority.ALWAYS);

            VBox content = new VBox(14, cropPane, zoomControls);
            content.setAlignment(Pos.CENTER);
            content.setPadding(new Insets(10));
            return content;
        }

        private StackPane createCropPane() {
            StackPane pane = new StackPane(imageView);
            pane.setMinSize(PREVIEW_SIZE, PREVIEW_SIZE);
            pane.setPrefSize(PREVIEW_SIZE, PREVIEW_SIZE);
            pane.setMaxSize(PREVIEW_SIZE, PREVIEW_SIZE);
            pane.setClip(new Rectangle(PREVIEW_SIZE, PREVIEW_SIZE));
            pane.setCursor(Cursor.OPEN_HAND);
            pane.setStyle("-fx-background-color: #111827; -fx-border-color: #3b82f6; -fx-border-width: 2;");

            pane.setOnMousePressed(event -> {
                dragStartX = event.getSceneX();
                dragStartY = event.getSceneY();
                startTranslateX = imageView.getTranslateX();
                startTranslateY = imageView.getTranslateY();
                pane.setCursor(Cursor.CLOSED_HAND);
            });
            pane.setOnMouseDragged(event -> {
                double nextX = startTranslateX + event.getSceneX() - dragStartX;
                double nextY = startTranslateY + event.getSceneY() - dragStartY;
                setClampedTranslate(nextX, nextY);
            });
            pane.setOnMouseReleased(event -> pane.setCursor(Cursor.OPEN_HAND));
            pane.setOnMouseExited(event -> pane.setCursor(Cursor.OPEN_HAND));
            pane.setOnScroll(event -> {
                double step = event.getDeltaY() > 0 ? 0.1 : -0.1;
                zoomSlider.setValue(clamp(zoomSlider.getValue() + step, 1.0, MAX_ZOOM));
                event.consume();
            });

            return pane;
        }

        private void updateImageSize() {
            double centerX = image.getWidth() / 2.0;
            double centerY = image.getHeight() / 2.0;
            if (imageView.getFitWidth() > 0 && imageView.getFitHeight() > 0) {
                ImageProcessor.CropArea currentArea = getCropArea();
                centerX = currentArea.x() + currentArea.size() / 2.0;
                centerY = currentArea.y() + currentArea.size() / 2.0;
            }

            imageView.setFitWidth(displayWidth());
            imageView.setFitHeight(displayHeight());
            setTranslateForCenter(centerX, centerY);
        }

        private void resetImage() {
            zoomSlider.setValue(1.0);
            imageView.setTranslateX(0);
            imageView.setTranslateY(0);
            updateImageSize();
        }

        private ImageProcessor.CropArea getCropArea() {
            double scale = currentScale();
            double imageLeft = (PREVIEW_SIZE - imageView.getFitWidth()) / 2.0 + imageView.getTranslateX();
            double imageTop = (PREVIEW_SIZE - imageView.getFitHeight()) / 2.0 + imageView.getTranslateY();
            double cropSize = PREVIEW_SIZE / scale;
            double cropX = clamp(-imageLeft / scale, 0, image.getWidth() - cropSize);
            double cropY = clamp(-imageTop / scale, 0, image.getHeight() - cropSize);

            return new ImageProcessor.CropArea(cropX, cropY, cropSize);
        }

        private void setTranslateForCenter(double centerX, double centerY) {
            double nextX = (image.getWidth() / 2.0 - centerX) * currentScale();
            double nextY = (image.getHeight() / 2.0 - centerY) * currentScale();
            setClampedTranslate(nextX, nextY);
        }

        private void setClampedTranslate(double nextX, double nextY) {
            double maxX = Math.max(0, (displayWidth() - PREVIEW_SIZE) / 2.0);
            double maxY = Math.max(0, (displayHeight() - PREVIEW_SIZE) / 2.0);
            imageView.setTranslateX(clamp(nextX, -maxX, maxX));
            imageView.setTranslateY(clamp(nextY, -maxY, maxY));
        }

        private double displayWidth() {
            return image.getWidth() * currentScale();
        }

        private double displayHeight() {
            return image.getHeight() * currentScale();
        }

        private double currentScale() {
            return minScale * zoomSlider.getValue();
        }

        private static double clamp(double value, double min, double max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}
