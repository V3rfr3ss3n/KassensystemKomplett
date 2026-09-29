package de.mmbbs.kassensystem.ui;

import de.mmbbs.kassensystem.auth.AuthService;
import de.mmbbs.kassensystem.auth.Benutzer;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;

/**
 * Login-Dialog fuer die lokalen Testnutzer.
 */
public class LoginDialog extends Dialog<LoginDialog.Anmeldung> {
    public record Anmeldung(Benutzer benutzer, String passwort) {}
    private final AuthService authService;
    private final TextField benutzerField = new TextField();
    private final PasswordField passwortField = new PasswordField();
    private final Label fehlerLabel = new Label();

    public LoginDialog(AuthService authService) {
        this.authService = authService;

        setTitle("Anmeldung");
        setHeaderText("Kassensystem anmelden");

        benutzerField.setPromptText("admin, kassierer oder lagerist");
        passwortField.setPromptText("Passwort");
        fehlerLabel.getStyleClass().add("error-label");

        ToggleButton darkModeButton = new ToggleButton(ThemeManager.isDarkMode() ? "Hellmodus" : "Darkmode");
        darkModeButton.getStyleClass().add("secondary-button");
        darkModeButton.setSelected(ThemeManager.isDarkMode());
        darkModeButton.selectedProperty().addListener((obs, oldValue, selected) -> {
            ThemeManager.setDarkMode(selected);
            darkModeButton.setText(selected ? "Hellmodus" : "Darkmode");
        });

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(10));
        form.add(new Label("Benutzer:"), 0, 0);
        form.add(benutzerField, 1, 0);
        form.add(new Label("Passwort:"), 0, 1);
        form.add(passwortField, 1, 1);
        form.add(fehlerLabel, 1, 2);
        form.add(darkModeButton, 1, 3);

        getDialogPane().setContent(form);
        ThemeManager.applyToDialogPane(getDialogPane());
        ButtonType anmelden = new ButtonType("Anmelden", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        ButtonType abbrechen = new ButtonType("Abbrechen", javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(anmelden, abbrechen);
        getDialogPane().lookupButton(anmelden).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (authService.anmelden(benutzerField.getText(), passwortField.getText()).isEmpty()) {
                fehlerLabel.setText("Benutzer oder Passwort ist falsch.");
                event.consume();
            }
        });

        setResultConverter(button -> {
            if (button != anmelden) {
                return null;
            }
            return authService.anmelden(benutzerField.getText(), passwortField.getText())
                    .map(benutzer -> new Anmeldung(benutzer, passwortField.getText())).orElse(null);
        });
    }
}
