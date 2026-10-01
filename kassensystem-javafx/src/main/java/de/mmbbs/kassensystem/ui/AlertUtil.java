package de.mmbbs.kassensystem.ui;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class AlertUtil {

    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        show(alert, title, message);
    }

    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        show(alert, title, message);
    }

    public static boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        ThemeManager.applyToDialogPane(alert.getDialogPane());
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    public static void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        show(alert, title, message);
    }

    private static void show(Alert alert, String title, String message) {
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        ThemeManager.applyToDialogPane(alert.getDialogPane());
        alert.showAndWait();
    }
}
