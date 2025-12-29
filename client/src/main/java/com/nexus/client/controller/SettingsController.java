package com.nexus.client.controller;

import com.nexus.client.service.GameService;
import com.nexus.shared.model.AppSettings;
import com.nexus.shared.repository.SettingsRepository;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the Settings view.
 */
public class SettingsController implements Initializable {

    @FXML private HBox launchOnStartupToggle;
    @FXML private HBox closeToTrayToggle;
    @FXML private HBox darkModeToggle;
    @FXML private Button clearLibraryButton;
    @FXML private Label dbPathLabel;

    private final SettingsRepository settingsRepository = new SettingsRepository();
    private final GameService gameService = GameService.getInstance();
    private AppSettings settings;

    private boolean launchOnStartup = false;
    private boolean closeToTray = false;
    private boolean darkMode = true;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadSettings();

        // Show database path
        if (dbPathLabel != null) {
            File dbFile = new File("nexus.db");
            dbPathLabel.setText(dbFile.getAbsolutePath());
        }
    }

    private void loadSettings() {
        Task<AppSettings> loadTask = new Task<>() {
            @Override
            protected AppSettings call() {
                return settingsRepository.getSettings();
            }
        };

        loadTask.setOnSucceeded(e -> {
            settings = loadTask.getValue();
            Platform.runLater(() -> {
                launchOnStartup = settings.isLaunchOnStartup();
                closeToTray = settings.isCloseToTray();
                darkMode = settings.isDarkMode();
                setupToggles();
            });
        });

        loadTask.setOnFailed(e -> {
            Platform.runLater(this::setupToggles);
        });

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void setupToggles() {
        // Set initial states
        updateToggleVisual(launchOnStartupToggle, launchOnStartup);
        updateToggleVisual(closeToTrayToggle, closeToTray);
        updateToggleVisual(darkModeToggle, darkMode);

        // Add click handlers
        launchOnStartupToggle.setOnMouseClicked(e -> {
            launchOnStartup = !launchOnStartup;
            updateToggleVisual(launchOnStartupToggle, launchOnStartup);
            saveSetting("launchOnStartup", launchOnStartup);
        });

        closeToTrayToggle.setOnMouseClicked(e -> {
            closeToTray = !closeToTray;
            updateToggleVisual(closeToTrayToggle, closeToTray);
            saveSetting("closeToTray", closeToTray);
        });

        darkModeToggle.setOnMouseClicked(e -> {
            darkMode = !darkMode;
            updateToggleVisual(darkModeToggle, darkMode);
            saveSetting("darkMode", darkMode);
        });
    }

    private void saveSetting(String name, Object value) {
        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() {
                settingsRepository.updateSetting(name, value);
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> {
            System.out.println("[SETTINGS] Saved " + name + ": " + value);
        });

        Thread thread = new Thread(saveTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void updateToggleVisual(HBox toggle, boolean isOn) {
        toggle.getStyleClass().removeAll("toggle-on", "toggle-off");
        toggle.getStyleClass().add(isOn ? "toggle-on" : "toggle-off");
    }

    @FXML
    private void onClearLibrary() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear Library");
        alert.setHeaderText("Clear all scanned games?");
        alert.setContentText("This will remove all automatically detected games from your library. Manually added games will be preserved. A rescan will start automatically.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (clearLibraryButton != null) {
                    clearLibraryButton.setDisable(true);
                    clearLibraryButton.setText("Clearing...");
                }

                Task<Void> clearTask = new Task<>() {
                    @Override
                    protected Void call() {
                        gameService.clearAllGames();
                        return null;
                    }
                };

                clearTask.setOnSucceeded(e -> {
                    Platform.runLater(() -> {
                        if (clearLibraryButton != null) {
                            clearLibraryButton.setDisable(false);
                            clearLibraryButton.setText("Clear & Rescan");
                        }

                        // Show success message
                        Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                        successAlert.setTitle("Library Cleared");
                        successAlert.setHeaderText("Library has been cleared");
                        successAlert.setContentText("Please go to the Library view or use Scan Games to rescan your games.");
                        successAlert.showAndWait();
                    });
                });

                clearTask.setOnFailed(e -> {
                    Platform.runLater(() -> {
                        if (clearLibraryButton != null) {
                            clearLibraryButton.setDisable(false);
                            clearLibraryButton.setText("Clear & Rescan");
                        }

                        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                        errorAlert.setTitle("Error");
                        errorAlert.setHeaderText("Failed to clear library");
                        errorAlert.setContentText("An error occurred while clearing the library.");
                        errorAlert.showAndWait();
                    });
                });

                Thread thread = new Thread(clearTask);
                thread.setDaemon(true);
                thread.start();
            }
        });
    }
}

