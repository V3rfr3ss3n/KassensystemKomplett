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
 * Anmeldung gegen das Backend.
 */
public class LoginDialog extends Dialog<LoginDialog.Anmeldung> {
    public record Anmeldung(Benutzer benutzer, String passwort) {}
    private final AuthService authService;
    private final TextField benutzerField = new TextField();
    private final PasswordField passwortField = new PasswordField();
    private final Label fehlerLabel = new Label();
    private Anmeldung validiert;

    public LoginDialog(AuthService authService) {
        this.authService = authService;

        setTitle("Anmeldung");
        setHeaderText("Kassensystem anmelden");

        benutzerField.setPromptText("Benutzername");
        passwortField.setPromptText("Passwort");
        fehlerLabel.getStyleClass().add("error-label");
        fehlerLabel.setWrapText(true);
        fehlerLabel.setMaxWidth(300);

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
            try {
                var result = authService.anmelden(benutzerField.getText(), passwortField.getText());
                if (result.isEmpty()) {
                    fehlerLabel.setText("Benutzer oder Passwort ist falsch.");
                    event.consume();
                    return;
                }
                Benutzer benutzer = result.get();
                String password = passwortField.getText();
                if (benutzer.passwortwechselNoetig()) {
                    var neuesPasswort = frageNeuesPasswort();
                    if (neuesPasswort.isEmpty()) { event.consume(); return; }
                    authService.passwortAendern(benutzer.benutzername(), password, neuesPasswort.get());
                    password = neuesPasswort.get();
                    benutzer = authService.anmelden(benutzer.benutzername(), password).orElseThrow();
                }
                validiert = new Anmeldung(benutzer, password);
            } catch (RuntimeException ex) {
                fehlerLabel.setText(ex.getMessage() == null ? "Anmeldung fehlgeschlagen." : ex.getMessage());
                event.consume();
            }
        });

        setResultConverter(button -> {
            if (button != anmelden) {
                return null;
            }
            return validiert;
        });
    }

    private java.util.Optional<String> frageNeuesPasswort() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Passwort ändern");
        dialog.setHeaderText("Bitte das Startpasswort ersetzen");
        PasswordField neu = new PasswordField();
        PasswordField wiederholen = new PasswordField();
        Label hinweis = new Label("Mindestens 10 Zeichen.");
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(12));
        form.add(new Label("Neues Passwort:"), 0, 0);
        form.add(neu, 1, 0);
        form.add(new Label("Wiederholen:"), 0, 1);
        form.add(wiederholen, 1, 1);
        form.add(hinweis, 1, 2);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (neu.getText().length() < 10 || !neu.getText().equals(wiederholen.getText())) {
                hinweis.setText("Mindestens 10 Zeichen; beide Eingaben müssen übereinstimmen.");
                event.consume();
            }
        });
        dialog.setResultConverter(button -> button == ButtonType.OK ? neu.getText() : null);
        ThemeManager.applyToDialogPane(dialog.getDialogPane());
        return dialog.showAndWait();
    }
}
