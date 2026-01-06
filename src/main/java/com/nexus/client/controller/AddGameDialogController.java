package com.nexus.client.controller;

import com.nexus.client.NexusLauncherApp;
import com.nexus.model.Game;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Controller for the Add Game dialog.
 */
public class AddGameDialogController implements Initializable {

    @FXML private TextField gameTitleField;
    @FXML private TextField executablePathField;
    @FXML private Button browseButton;
    @FXML private ComboBox<String> platformCombo;
    @FXML private Button cancelButton;
    @FXML private Button addButton;
    @FXML private Label errorLabel;

    private Stage dialogStage;
    private Consumer<Game> onGameAdded;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize platform dropdown
        platformCombo.getItems().addAll("Manual", "Steam", "Epic");
        platformCombo.setValue("Manual");

        // Clear error on input
        gameTitleField.textProperty().addListener((obs, old, newVal) -> clearError());
        executablePathField.textProperty().addListener((obs, old, newVal) -> clearError());

        // Validate on add
        addButton.setOnAction(e -> onAdd());
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setOnGameAdded(Consumer<Game> callback) {
        this.onGameAdded = callback;
    }

    @FXML
    private void onBrowse() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Game Executable");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Executable Files", "*.exe"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(dialogStage);
        if (selectedFile != null) {
            executablePathField.setText(selectedFile.getAbsolutePath());

            // Auto-fill title if empty
            if (gameTitleField.getText().isEmpty()) {
                String fileName = selectedFile.getName();
                // Remove .exe extension
                if (fileName.endsWith(".exe")) {
                    fileName = fileName.substring(0, fileName.length() - 4);
                }
                // Clean up the name
                fileName = fileName.replace("_", " ").replace("-", " ");
                gameTitleField.setText(fileName);
            }
        }
    }

    @FXML
    private void onCancel() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    private void onAdd() {
        String title = gameTitleField.getText().trim();
        String path = executablePathField.getText().trim();

        // Validate
        if (title.isEmpty()) {
            showError("Please enter a game title.");
            return;
        }

        if (path.isEmpty()) {
            showError("Please select an executable file.");
            return;
        }

        File exeFile = new File(path);
        if (!exeFile.exists()) {
            showError("The selected file does not exist.");
            return;
        }

        // Create game object
        Game newGame = new Game();
        newGame.setTitle(title);
        newGame.setExecutablePath(path);
        newGame.setInstallPath(exeFile.getParent());
        newGame.setStatus(Game.Status.READY);

        // Set platform
        String platform = platformCombo.getValue();
        Game.Platform gamePlatform;
        switch (platform) {
            case "Steam" -> gamePlatform = Game.Platform.STEAM;
            case "Epic" -> gamePlatform = Game.Platform.EPIC;
            default -> gamePlatform = Game.Platform.MANUAL;
        }
        newGame.setPlatform(gamePlatform);

        // Generate unique ID
        newGame.setUniqueId(Game.generateUniqueId(gamePlatform, title));

        // Set a placeholder cover image
        newGame.setCoverImageUrl("");

        // Callback
        if (onGameAdded != null) {
            onGameAdded.accept(newGame);
        }

        System.out.println("[ADD GAME] Added: " + title + " at " + path);

        // Close dialog
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }
}

