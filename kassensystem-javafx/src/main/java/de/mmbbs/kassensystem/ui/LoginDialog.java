package de.mmbbs.kassensystem.ui;

import de.mmbbs.kassensystem.auth.AuthService;
import de.mmbbs.kassensystem.auth.Benutzer;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

/**
 * Login-Dialog fuer die lokalen Testnutzer.
 */
public class LoginDialog extends Dialog<Benutzer> {
    private final AuthService authService;
    private final TextField benutzerField = new TextField();
    private final PasswordField passwortField = new PasswordField();
    private final Label fehlerLabel = new Label();

    public LoginDialog(AuthService authService) {
        this.authService = authService;

        setTitle("Anmeldung");
        setHeaderText("Kassensystem anmelden");

        benutzerField.setPromptText("admin oder kassierer");
        passwortField.setPromptText("Passwort");
        fehlerLabel.getStyleClass().add("error-label");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(10));
        form.add(new Label("Benutzer:"), 0, 0);
        form.add(benutzerField, 1, 0);
        form.add(new Label("Passwort:"), 0, 1);
        form.add(passwortField, 1, 1);
        form.add(fehlerLabel, 1, 2);

        getDialogPane().setContent(form);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().lookupButton(ButtonType.OK).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (authService.anmelden(benutzerField.getText(), passwortField.getText()).isEmpty()) {
                fehlerLabel.setText("Benutzer oder Passwort ist falsch.");
                event.consume();
            }
        });

        setResultConverter(button -> {
            if (button != ButtonType.OK) {
                return null;
            }
            return authService.anmelden(benutzerField.getText(), passwortField.getText()).orElse(null);
        });
    }
}
