package de.mmbbs.kassensystem.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Parent;
import javafx.scene.control.DialogPane;
import java.util.prefs.Preferences;
import java.util.concurrent.TimeUnit;
import java.nio.charset.StandardCharsets;

/**
 * Zentrale Darkmode-Umschaltung fuer JavaFX-Ansichten und Dialoge.
 */
public final class ThemeManager {
    private static final String DARK_MODE_CLASS = "dark-mode";
    private static final String STYLESHEET = ThemeManager.class.getResource("/styles.css").toExternalForm();
    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(ThemeManager.class);
    private static final BooleanProperty darkMode = new SimpleBooleanProperty(initialDarkMode());

    private ThemeManager() {
    }

    private static boolean initialDarkMode() {
        String gespeichert = PREFERENCES.get("darkMode", null);
        if (gespeichert != null) return Boolean.parseBoolean(gespeichert);
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) return false;
        try {
            Process prozess = new ProcessBuilder("reg", "query",
                    "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                    "/v", "AppsUseLightTheme").start();
            if (!prozess.waitFor(1, TimeUnit.SECONDS) || prozess.exitValue() != 0) {
                prozess.destroyForcibly();
                return false;
            }
            String ausgabe = new String(prozess.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return ausgabe.lines().anyMatch(zeile -> zeile.contains("AppsUseLightTheme") && zeile.trim().endsWith("0x0"));
        } catch (Exception ignoriert) {
            return false;
        }
    }

    public static BooleanProperty darkModeProperty() {
        return darkMode;
    }

    public static boolean isDarkMode() {
        return darkMode.get();
    }

    public static void setDarkMode(boolean enabled) {
        darkMode.set(enabled);
        PREFERENCES.putBoolean("darkMode", enabled);
    }

    public static void applyToRoot(Parent root) {
        aktualisiereStyleClass(root, darkMode.get());
        darkMode.addListener((obs, oldValue, newValue) -> aktualisiereStyleClass(root, newValue));
    }

    public static void applyToDialogPane(DialogPane dialogPane) {
        if (!dialogPane.getStylesheets().contains(STYLESHEET)) {
            dialogPane.getStylesheets().add(STYLESHEET);
        }
        aktualisiereStyleClass(dialogPane, darkMode.get());
        darkMode.addListener((obs, oldValue, newValue) -> aktualisiereStyleClass(dialogPane, newValue));
    }

    private static void aktualisiereStyleClass(Parent parent, boolean enabled) {
        parent.getStyleClass().remove(DARK_MODE_CLASS);
        if (enabled) {
            parent.getStyleClass().add(DARK_MODE_CLASS);
        }
    }
}
