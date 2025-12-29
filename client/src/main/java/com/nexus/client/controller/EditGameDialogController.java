package com.nexus.client.controller;

import com.nexus.client.service.GameService;
import com.nexus.shared.model.Game;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

/**
 * Controller for the Edit Game dialog.
 */
public class EditGameDialogController implements Initializable {

    @FXML private TextField gameTitleField;
    @FXML private TextField executablePathField;
    @FXML private TextField coverImageField;
    @FXML private TextField developerField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<String> platformCombo;
    @FXML private Button browseButton;
    @FXML private Button cancelButton;
    @FXML private Button saveButton;
    @FXML private Button deleteButton;
    @FXML private Label errorLabel;

    private Stage dialogStage;
    private Game currentGame;
    private Consumer<Game> onGameUpdated;
    private Runnable onGameDeleted;
    private final GameService gameService = GameService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize platform dropdown
        platformCombo.getItems().addAll("Manual", "Steam", "Epic");

        // Clear error on input
        gameTitleField.textProperty().addListener((obs, old, newVal) -> clearError());
        executablePathField.textProperty().addListener((obs, old, newVal) -> clearError());
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setGame(Game game) {
        this.currentGame = game;
        populateFields();
    }

    public void setOnGameUpdated(Consumer<Game> callback) {
        this.onGameUpdated = callback;
    }

    public void setOnGameDeleted(Runnable callback) {
        this.onGameDeleted = callback;
    }

    private void populateFields() {
        if (currentGame == null) return;

        gameTitleField.setText(currentGame.getTitle());
        executablePathField.setText(currentGame.getExecutablePath() != null ? currentGame.getExecutablePath() : "");
        coverImageField.setText(currentGame.getCoverImageUrl() != null ? currentGame.getCoverImageUrl() : "");
        developerField.setText(currentGame.getDeveloper() != null ? currentGame.getDeveloper() : "");
        descriptionArea.setText(currentGame.getDescription() != null ? currentGame.getDescription() : "");

        // Set platform
        switch (currentGame.getPlatform()) {
            case STEAM -> platformCombo.setValue("Steam");
            case EPIC -> platformCombo.setValue("Epic");
            default -> platformCombo.setValue("Manual");
        }
    }

    @FXML
    private void onBrowse() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Game Executable");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Executable Files", "*.exe"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        // Start from current path if it exists
        String currentPath = executablePathField.getText();
        if (currentPath != null && !currentPath.isEmpty()) {
            File currentFile = new File(currentPath);
            if (currentFile.getParentFile() != null && currentFile.getParentFile().exists()) {
                fileChooser.setInitialDirectory(currentFile.getParentFile());
            }
        }

        File selectedFile = fileChooser.showOpenDialog(dialogStage);
        if (selectedFile != null) {
            executablePathField.setText(selectedFile.getAbsolutePath());
        }
    }

    @FXML
    private void onCancel() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    @FXML
    private void onSave() {
        String title = gameTitleField.getText().trim();
        String path = executablePathField.getText().trim();

        // Validate
        if (title.isEmpty()) {
            showError("Please enter a game title.");
            return;
        }

        // Update game object
        currentGame.setTitle(title);
        currentGame.setExecutablePath(path);
        currentGame.setCoverImageUrl(coverImageField.getText().trim());
        currentGame.setDeveloper(developerField.getText().trim());
        currentGame.setDescription(descriptionArea.getText().trim());

        // Update install path from executable
        if (!path.isEmpty()) {
            File exeFile = new File(path);
            if (exeFile.exists()) {
                currentGame.setInstallPath(exeFile.getParent());
            }
        }

        // Update status based on executable
        if (path.isEmpty() || !new File(path).exists()) {
            currentGame.setStatus(Game.Status.MISSING);
        } else {
            currentGame.setStatus(Game.Status.READY);
        }

        // Set platform
        String platform = platformCombo.getValue();
        switch (platform) {
            case "Steam" -> currentGame.setPlatform(Game.Platform.STEAM);
            case "Epic" -> currentGame.setPlatform(Game.Platform.EPIC);
            case "System" -> currentGame.setPlatform(Game.Platform.SYSTEM);
            default -> currentGame.setPlatform(Game.Platform.MANUAL);
        }

        // Save to database
        gameService.saveGame(currentGame);

        // Callback
        if (onGameUpdated != null) {
            onGameUpdated.accept(currentGame);
        }

        System.out.println("[EDIT GAME] Updated: " + title);

        // Close dialog
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    @FXML
    private void onDelete() {
        // Show confirmation dialog
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Game");
        alert.setHeaderText("Delete " + currentGame.getTitle() + "?");
        alert.setContentText("This action cannot be undone. The game will be removed from your library.");

        // Style the alert
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: #1f2937;");
        dialogPane.lookup(".content.label").setStyle("-fx-text-fill: #d1d5db;");
        dialogPane.lookup(".header-panel").setStyle("-fx-background-color: #1f2937;");
        dialogPane.lookup(".header-panel .label").setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Delete from database
                gameService.deleteGame(currentGame);

                System.out.println("[DELETE GAME] Deleted: " + currentGame.getTitle());

                if (onGameDeleted != null) {
                    onGameDeleted.run();
                }

                if (dialogStage != null) {
                    dialogStage.close();
                }
            }
        });
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

