package com.nexus.client.controller;

import com.nexus.client.NexusLauncherApp;
import com.nexus.client.service.GameLauncher;
import com.nexus.client.service.GameService;
import com.nexus.client.util.PlaceholderImageUtil;
import com.nexus.model.Game;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the Game Details view.
 */
public class GameDetailsController implements Initializable {

    @FXML private StackPane rootPane;
    @FXML private ImageView heroBackground;
    @FXML private ImageView coverImage;
    @FXML private Label titleLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label developerLabel;
    @FXML private Label platformLabel;
    @FXML private Label releaseDateLabel;
    @FXML private Label statusLabel;
    @FXML private ImageView statusIcon;
    @FXML private HBox statusBadgeContainer;
    @FXML private Button playButton;
    @FXML private Button editButton;
    @FXML private Button backButton;
    @FXML private Button favoriteButton;
    @FXML private ImageView favoriteIcon;
    @FXML private HBox platformBadge;

    private MainController mainController;
    private Game currentGame;
    private final GameLauncher gameLauncher = new GameLauncher();
    private final GameService gameService = GameService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Bind hero background to fill the root pane
        heroBackground.fitWidthProperty().bind(rootPane.widthProperty());
        heroBackground.fitHeightProperty().bind(rootPane.heightProperty());
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setGame(Game game) {
        this.currentGame = game;
        populateView();
    }

    private void populateView() {
        if (currentGame == null) return;

        // Set title
        titleLabel.setText(currentGame.getTitle());

        // Set description
        String description = currentGame.getDescription();
        if (description != null && !description.isEmpty()) {
            descriptionLabel.setText(description);
        } else {
            descriptionLabel.setText("No description available.");
        }

        // Set metadata
        developerLabel.setText(currentGame.getDeveloper() != null ? currentGame.getDeveloper() : "Unknown");
        platformLabel.setText(currentGame.getPlatform().getDisplayName());
        releaseDateLabel.setText(currentGame.getReleaseDate() != null ? currentGame.getReleaseDate() : "Unknown");

        // Set status
        if (currentGame.getStatus() == Game.Status.READY) {
            statusLabel.setText("Ready to Play");
            statusLabel.getStyleClass().add("status-ready");
            if (statusIcon != null) {
                statusIcon.setImage(new Image(getClass().getResourceAsStream("/assets/check.png")));
            }
            playButton.setText("PLAY");
            playButton.getStyleClass().remove("locate-button");
        } else if (currentGame.getStatus() == Game.Status.MISSING) {
            statusLabel.setText("Executable Missing");
            statusLabel.getStyleClass().add("status-missing");
            if (statusIcon != null) {
                statusIcon.setImage(new Image(getClass().getResourceAsStream("/assets/folder.png")));
            }
            playButton.setText("LOCATE");
            playButton.getStyleClass().add("locate-button");
        }

        // Set platform badge styling
        platformBadge.getStyleClass().add("badge-" + currentGame.getPlatform().name().toLowerCase());

        // Set favorite button state
        updateFavoriteButton();

        // Load hero background image
        loadHeroImage();

        // Load cover image
        loadCoverImage();
    }

    private void loadHeroImage() {
        String heroUrl = currentGame.getHeroImageUrl();
        if (heroUrl == null || heroUrl.isEmpty()) {
            heroUrl = currentGame.getCoverImageUrl();
        }

        if (heroUrl != null && !heroUrl.isEmpty()) {
            try {
                Image image = new Image(heroUrl, 1280, 720, false, true, true);
                heroBackground.setImage(image);
            } catch (Exception e) {
                // Load placeholder hero image
                loadPlaceholderHeroImage();
            }
        } else {
            // Load placeholder hero image
            loadPlaceholderHeroImage();
        }
    }

    private void loadPlaceholderHeroImage() {
        String gameTitle = currentGame.getTitle() != null ? currentGame.getTitle() : "Game";
        String placeholderUrl = PlaceholderImageUtil.getHeroPlaceholder(gameTitle, 1280, 720);
        try {
            Image image = new Image(placeholderUrl, 1280, 720, false, true, true);
            heroBackground.setImage(image);
        } catch (Exception e) {
            // Keep default background
        }
    }

    private void loadCoverImage() {
        if (currentGame.getCoverImageUrl() != null && !currentGame.getCoverImageUrl().isEmpty()) {
            try {
                Image image = new Image(currentGame.getCoverImageUrl(), 240, 320, false, true, true);
                image.errorProperty().addListener((obs, wasError, isError) -> {
                    if (isError) {
                        loadPlaceholderCoverImage();
                    }
                });
                image.progressProperty().addListener((obs, oldProgress, newProgress) -> {
                    if (newProgress.doubleValue() >= 1.0 && !image.isError()) {
                        coverImage.setImage(image);
                    }
                });
                if (image.getProgress() >= 1.0 && !image.isError()) {
                    coverImage.setImage(image);
                } else if (image.isError()) {
                    loadPlaceholderCoverImage();
                }
            } catch (Exception e) {
                loadPlaceholderCoverImage();
            }
        } else {
            loadPlaceholderCoverImage();
        }
    }

    private void loadPlaceholderCoverImage() {
        String gameTitle = currentGame.getTitle() != null ? currentGame.getTitle() : "Game";
        String placeholderUrl = PlaceholderImageUtil.getCoverPlaceholder(gameTitle, 480, 640);
        try {
            Image image = new Image(placeholderUrl, 240, 320, false, true, true);
            image.errorProperty().addListener((obs, wasError, isError) -> {
                if (isError) {
                    // Set a fallback background color on the cover container
                    coverImage.setStyle("-fx-background-color: linear-gradient(to bottom, #4f46e5, #1f2937);");
                }
            });
            image.progressProperty().addListener((obs, oldProgress, newProgress) -> {
                if (newProgress.doubleValue() >= 1.0 && !image.isError()) {
                    coverImage.setImage(image);
                }
            });
            if (image.getProgress() >= 1.0 && !image.isError()) {
                coverImage.setImage(image);
            }
        } catch (Exception e) {
            // Keep default if placeholder fails
        }
    }

    private void updateFavoriteButton() {
        if (currentGame.isFavorite()) {
            favoriteButton.getStyleClass().add("favorite-active");
            if (favoriteIcon != null) {
                favoriteIcon.setOpacity(1.0);
            }
        } else {
            favoriteButton.getStyleClass().remove("favorite-active");
            if (favoriteIcon != null) {
                favoriteIcon.setOpacity(0.5);
            }
        }
    }

    @FXML
    private void onBackClick() {
        if (mainController != null) {
            mainController.backToLibrary();
        }
    }

    @FXML
    private void onPlayClick() {
        if (currentGame.getStatus() == Game.Status.MISSING) {
            // Open file chooser to locate executable
            locateExecutable();
            return;
        }

        // Launch game in background thread
        Task<Boolean> launchTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return gameLauncher.play(currentGame);
            }
        };

        launchTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                if (mainController != null) {
                    mainController.showToast("Game Launched", "Starting " + currentGame.getTitle() + "...");
                }
            });
        });

        launchTask.setOnFailed(e -> {
            Throwable ex = launchTask.getException();
            Platform.runLater(() -> {
                if (mainController != null) {
                    mainController.showToast("Launch Failed", ex.getMessage());
                }
            });
        });

        Thread launchThread = new Thread(launchTask);
        launchThread.setDaemon(true);
        launchThread.start();
    }

    /**
     * Opens a file chooser to locate the game executable.
     */
    private void locateExecutable() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Locate Game Executable");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Executable Files", "*.exe"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
        if (selectedFile != null) {
            currentGame.setExecutablePath(selectedFile.getAbsolutePath());
            currentGame.setInstallPath(selectedFile.getParent());
            currentGame.setStatus(Game.Status.READY);

            // Save to database
            gameService.saveGame(currentGame);

            // Update UI
            populateView();

            if (mainController != null) {
                mainController.showToast("Executable Located", "Game is now ready to play!");
            }
        }
    }

    @FXML
    private void onEditClick() {
        System.out.println("[EDIT] Opening edit dialog for: " + currentGame.getTitle());
        openEditDialog();
    }

    private void openEditDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nexus/client/views/EditGameDialog.fxml"));
            VBox dialogContent = loader.load();
            EditGameDialogController dialogController = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(NexusLauncherApp.getPrimaryStage());
            dialogStage.initStyle(StageStyle.TRANSPARENT);
            dialogStage.setTitle("Edit Game");

            Scene scene = new Scene(dialogContent);
            scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
            scene.getStylesheets().add(getClass().getResource("/com/nexus/client/styles/application.css").toExternalForm());

            dialogStage.setScene(scene);
            dialogController.setDialogStage(dialogStage);
            dialogController.setGame(currentGame);

            // Handle game update callback
            dialogController.setOnGameUpdated(updatedGame -> {
                populateView(); // Refresh the view with updated data
                if (mainController != null) {
                    mainController.showToast("Game Updated", updatedGame.getTitle() + " has been updated.");
                }
            });

            // Handle game deletion callback
            dialogController.setOnGameDeleted(() -> {
                if (mainController != null) {
                    mainController.showToast("Game Deleted", currentGame.getTitle() + " has been removed.");
                    mainController.backToLibrary();
                }
            });

            dialogStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("[ERROR] Failed to open edit dialog: " + e.getMessage());
        }
    }

    @FXML
    private void onFavoriteClick() {
        // Toggle favorite in background
        Task<Void> favoriteTask = new Task<>() {
            @Override
            protected Void call() {
                gameService.toggleFavorite(currentGame);
                return null;
            }
        };

        favoriteTask.setOnSucceeded(e -> {
            Platform.runLater(() -> {
                updateFavoriteButton();
                String message = currentGame.isFavorite()
                        ? currentGame.getTitle() + " added to favorites"
                        : currentGame.getTitle() + " removed from favorites";
                if (mainController != null) {
                    mainController.showToast("Favorites", message);
                }
            });
        });

        Thread thread = new Thread(favoriteTask);
        thread.setDaemon(true);
        thread.start();
    }
}

