package com.nexus.controller;

import com.nexus.NexusLauncherApp;
import com.nexus.service.GameLauncher;
import com.nexus.service.GameService;
import com.nexus.util.PlaceholderImageUtil;
import com.nexus.model.Game;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ResourceBundle;

/**
 * Controller for the Game Details view.
 * Modern layout with hero header, content body, and metadata sidebar.
 */
public class GameDetailsController implements Initializable {

    // Root elements
    @FXML private ScrollPane rootScrollPane;
    @FXML private VBox mainContainer;
    @FXML private StackPane heroHeader;

    // Hero section elements
    @FXML private ImageView heroBackground;
    @FXML private ImageView coverImage;
    @FXML private Label titleLabel;
    @FXML private Label platformBadgeLabel;
    @FXML private HBox statusBadgeContainer;
    @FXML private Label statusLabel;
    @FXML private FontIcon statusIcon;

    // Action buttons
    @FXML private Button playButton;
    @FXML private Button settingsButton;
    @FXML private Button backButton;
    @FXML private Button favoriteButton;
    @FXML private FontIcon favoriteIcon;

    // Content body elements
    @FXML private Label descriptionLabel;
    @FXML private Label playTimeLabel;
    @FXML private Label lastPlayedLabel;

    // Metadata sidebar elements
    @FXML private Label developerLabel;
    @FXML private Label publisherLabel;
    @FXML private Label releaseDateLabel;
    @FXML private FlowPane tagsContainer;

    // Context menu for settings
    private ContextMenu settingsContextMenu;

    private MainController mainController;
    private Game currentGame;
    private final GameLauncher gameLauncher = new GameLauncher();
    private final GameService gameService = GameService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup hero image to fill container while preserving aspect ratio
//        setupHeroImageCover();

        // Create settings context menu
        createSettingsContextMenu();
    }

    /**
     * Sets up the hero image with cover behavior (simulates CSS object-fit: cover).
     * The image fills the container completely while maintaining aspect ratio.
     */
//    private void setupHeroImageCover() {
//        // We manually handle aspect ratio, so disable preserveRatio
//        heroBackground.setPreserveRatio(false);
//
//        // Clip the hero header to hide overflow
//        javafx.scene.shape.Rectangle clipRect = new javafx.scene.shape.Rectangle();
//        clipRect.widthProperty().bind(heroHeader.widthProperty());
//        clipRect.heightProperty().bind(heroHeader.heightProperty());
//        heroHeader.setClip(clipRect);
//
//        // When image loads or container resizes, recalculate fit
//        heroBackground.imageProperty().addListener((obs, oldImg, newImg) -> {
//            if (newImg != null) {
//                Platform.runLater(this::updateHeroCover);
//            }
//        });
//
//        heroHeader.widthProperty().addListener((obs, oldVal, newVal) -> updateHeroCover());
//        heroHeader.heightProperty().addListener((obs, oldVal, newVal) -> updateHeroCover());
//    }
//
//    /**
//     * Updates hero image size to cover the container while maintaining aspect ratio.
//     */
//    private void updateHeroCover() {
//        Image img = heroBackground.getImage();
//        if (img == null || img.getWidth() <= 0 || img.getHeight() <= 0) return;
//
//        double containerWidth = heroHeader.getWidth();
//        double containerHeight = heroHeader.getHeight();
//        if (containerWidth <= 0 || containerHeight <= 0) return;
//
//        double imgAspect = img.getWidth() / img.getHeight();
//        double containerAspect = containerWidth / containerHeight;
//
//        double fitWidth, fitHeight;
//
//        // To "cover", scale the image so it fills both dimensions (some will overflow)
//        if (imgAspect > containerAspect) {
//            // Image is wider than container - fit to container height, width will overflow
//            fitHeight = containerHeight;
//            fitWidth = containerHeight * imgAspect;
//        } else {
//            // Image is taller than container - fit to container width, height will overflow
//            fitWidth = containerWidth;
//            fitHeight = containerWidth / imgAspect;
//        }
//
//        heroBackground.setFitWidth(fitWidth);
//        heroBackground.setFitHeight(fitHeight);
//
//        // Center the image (clip will hide overflow)
//        heroBackground.setTranslateX((containerWidth - fitWidth) / 2);
//        heroBackground.setTranslateY((containerHeight - fitHeight) / 3); // Bias towards showing top portion
//    }

    /**
     * Creates the settings context menu with Edit and Ignore options.
     */
    private void createSettingsContextMenu() {
        settingsContextMenu = new ContextMenu();
        settingsContextMenu.getStyleClass().add("settings-context-menu");

        // Edit Metadata menu item
        MenuItem editItem = new MenuItem("Edit Metadata");
        editItem.setGraphic(new FontIcon("fas-edit"));
        editItem.setOnAction(e -> openEditDialog());

        // Ignore Game menu item
        MenuItem ignoreItem = new MenuItem("Ignore Game");
        ignoreItem.setGraphic(new FontIcon("fas-eye-slash"));
        ignoreItem.getStyleClass().add("danger-menu-item");
        ignoreItem.setOnAction(e -> confirmIgnoreGame());

        settingsContextMenu.getItems().addAll(editItem, new SeparatorMenuItem(), ignoreItem);
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

        // Set platform badge
        platformBadgeLabel.setText(currentGame.getPlatform().getDisplayName().toUpperCase());
        platformBadgeLabel.getStyleClass().add("platform-" + currentGame.getPlatform().name().toLowerCase());

        // Set status badge
        updateStatusBadge();

        // Set description
        String description = currentGame.getDescription();
        if (description != null && !description.isEmpty()) {
            descriptionLabel.setText(description);
        } else {
            descriptionLabel.setText("No description available for this game.");
        }

        // Set metadata
        developerLabel.setText(currentGame.getDeveloper() != null ? currentGame.getDeveloper() : "Unknown");
        publisherLabel.setText(currentGame.getDeveloper() != null ? currentGame.getDeveloper() : "Unknown"); // Using developer as publisher fallback
        releaseDateLabel.setText(currentGame.getReleaseDate() != null ? currentGame.getReleaseDate() : "Unknown");

        // Set play time
        updatePlayTimeLabel();

        // Set last played
        updateLastPlayedLabel();

        // Set favorite button state
        updateFavoriteButton();

        // Populate tags
        populateTags();

        // Load images
        loadHeroImage();
        loadCoverImage();
    }

    private void updateStatusBadge() {
        if (currentGame.getStatus() == Game.Status.READY) {
            statusLabel.setText("Ready");
            statusBadgeContainer.getStyleClass().removeAll("status-missing");
            statusBadgeContainer.getStyleClass().add("status-ready");
            if (statusIcon != null) {
                statusIcon.setIconCode(FontAwesomeSolid.CHECK_CIRCLE);
            }
            updatePlayButton(false);
        } else if (currentGame.getStatus() == Game.Status.MISSING) {
            statusLabel.setText("Missing");
            statusBadgeContainer.getStyleClass().removeAll("status-ready");
            statusBadgeContainer.getStyleClass().add("status-missing");
            if (statusIcon != null) {
                statusIcon.setIconCode(MaterialDesignF.FOLDER_ALERT);
            }
            updatePlayButton(true);
        }
    }

    private void updatePlayButton(boolean isMissing) {
        HBox content = (HBox) playButton.getGraphic();
        if (content != null && content.getChildren().size() >= 2) {
            FontIcon icon = (FontIcon) content.getChildren().get(0);
            Label label = (Label) content.getChildren().get(1);

            if (isMissing) {
                icon.setIconLiteral("fas-folder-open");
                label.setText("LOCATE");
                playButton.getStyleClass().add("locate-mode");
            } else {
                icon.setIconLiteral("fas-play");
                label.setText("PLAY");
                playButton.getStyleClass().remove("locate-mode");
            }
        }
    }

    private void updatePlayTimeLabel() {
        long totalMinutes = currentGame.getTotalPlayTime();
        if (totalMinutes <= 0) {
            playTimeLabel.setText("0 hours");
        } else if (totalMinutes < 60) {
            playTimeLabel.setText(totalMinutes + " minutes");
        } else {
            double hours = totalMinutes / 60.0;
            playTimeLabel.setText(String.format("%.1f hours", hours));
        }
    }

    private void updateLastPlayedLabel() {
        LocalDateTime lastPlayed = currentGame.getLastPlayed();
        if (lastPlayed == null) {
            lastPlayedLabel.setText("Never");
        } else {
            LocalDateTime now = LocalDateTime.now();
            long hoursDiff = ChronoUnit.HOURS.between(lastPlayed, now);
            long daysDiff = ChronoUnit.DAYS.between(lastPlayed, now);

            if (hoursDiff < 1) {
                lastPlayedLabel.setText("Just now");
            } else if (hoursDiff < 24) {
                lastPlayedLabel.setText(hoursDiff + " hours ago");
            } else if (daysDiff < 7) {
                lastPlayedLabel.setText(daysDiff + " days ago");
            } else if (daysDiff < 30) {
                lastPlayedLabel.setText((daysDiff / 7) + " weeks ago");
            } else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
                lastPlayedLabel.setText(lastPlayed.format(formatter));
            }
        }
    }

    private void populateTags() {
        tagsContainer.getChildren().clear();

        // Add platform as a tag
        addTag(currentGame.getPlatform().getDisplayName());

        // Add some default genre tags based on common patterns
        // In a real app, these would come from the game metadata
        String title = currentGame.getTitle().toLowerCase();
        if (title.contains("rpg") || title.contains("fantasy")) {
            addTag("RPG");
        }
        if (title.contains("action") || title.contains("shooter")) {
            addTag("Action");
        }
        if (title.contains("indie")) {
            addTag("Indie");
        }

        // Always add at least one generic tag if empty
        if (tagsContainer.getChildren().isEmpty()) {
            addTag("Game");
        }
    }

    private void addTag(String tagName) {
        Label tag = new Label(tagName);
        tag.getStyleClass().add("game-tag");
        tagsContainer.getChildren().add(tag);
    }

    private void loadHeroImage() {
        String heroUrl = currentGame.getHeroImageUrl();
        if (heroUrl == null || heroUrl.isEmpty()) {
            heroUrl = currentGame.getCoverImageUrl();
        }

        if (heroUrl != null && !heroUrl.isEmpty()) {
            // Clean URL for CSS
            String cssUrl = heroUrl.replace(" ", "%20").replace("'", "\\'");

            // Apply to StackPane directly. 'cover' ensures it fits perfectly.
            heroHeader.setStyle(
                    "-fx-background-image: url('" + cssUrl + "'); " +
                            "-fx-background-size: cover; " +
                            "-fx-background-position: center center; " +
                            "-fx-background-repeat: no-repeat;"
            );
        } else {
            // Fallback gradient
            heroHeader.setStyle("-fx-background-color: linear-gradient(to bottom, #2d3748, #111827);");
        }
    }

    private void loadPlaceholderHeroImage() {
        String gameTitle = currentGame.getTitle() != null ? currentGame.getTitle() : "Game";
        String placeholderUrl = PlaceholderImageUtil.getHeroPlaceholder(gameTitle, 1920, 400);
        try {
            // FIX: preserveRatio = true
            Image image = new Image(placeholderUrl, 1920, 0, true, true, true);
            heroBackground.setImage(image);
        } catch (Exception e) {
            // Keep default background
        }
    }

    private void loadCoverImage() {
        if (currentGame.getCoverImageUrl() != null && !currentGame.getCoverImageUrl().isEmpty()) {
            try {
                Image image = new Image(currentGame.getCoverImageUrl(), 180, 240, false, true, true);
                image.errorProperty().addListener((obs, wasError, isError) -> {
                    if (isError) loadPlaceholderCoverImage();
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
        String placeholderUrl = PlaceholderImageUtil.getCoverPlaceholder(gameTitle, 360, 480);
        try {
            Image image = new Image(placeholderUrl, 180, 240, false, true, true);
            image.progressProperty().addListener((obs, oldProgress, newProgress) -> {
                if (newProgress.doubleValue() >= 1.0 && !image.isError()) {
                    coverImage.setImage(image);
                }
            });
            if (image.getProgress() >= 1.0 && !image.isError()) {
                coverImage.setImage(image);
            }
        } catch (Exception e) {
            // Keep default
        }
    }

    private void updateFavoriteButton() {
        if (currentGame.isFavorite()) {
            favoriteButton.getStyleClass().add("favorite-active");
            if (favoriteIcon != null) {
                favoriteIcon.setIconCode(FontAwesomeSolid.HEART);
                favoriteIcon.getStyleClass().add("favorite-active-icon");
            }
        } else {
            favoriteButton.getStyleClass().remove("favorite-active");
            if (favoriteIcon != null) {
                favoriteIcon.setIconCode(FontAwesomeSolid.HEART);
                favoriteIcon.getStyleClass().remove("favorite-active-icon");
            }
        }
    }

    // ==================== EVENT HANDLERS ====================

    @FXML
    private void onBackClick() {
        if (mainController != null) {
            mainController.backToLibrary();
        }
    }

    @FXML
    private void onPlayClick() {
        if (currentGame.getStatus() == Game.Status.MISSING) {
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

        launchTask.setOnSucceeded(e -> Platform.runLater(() -> {
            if (mainController != null) {
                mainController.showToast("Game Launched", "Starting " + currentGame.getTitle() + "...");
            }
            // Update last played time in UI
            currentGame.setLastPlayed(LocalDateTime.now());
            updateLastPlayedLabel();
        }));

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

    @FXML
    private void onSettingsClick() {
        // Show context menu below the settings button
        settingsContextMenu.show(settingsButton,
                javafx.geometry.Side.BOTTOM, 0, 4);
    }

    @FXML
    private void onFavoriteClick() {
        Task<Void> favoriteTask = new Task<>() {
            @Override
            protected Void call() {
                gameService.toggleFavorite(currentGame);
                return null;
            }
        };

        favoriteTask.setOnSucceeded(e -> Platform.runLater(() -> {
            updateFavoriteButton();
            String message = currentGame.isFavorite()
                    ? currentGame.getTitle() + " added to favorites"
                    : currentGame.getTitle() + " removed from favorites";
            if (mainController != null) {
                mainController.showToast("Favorites", message);
            }
        }));

        Thread thread = new Thread(favoriteTask);
        thread.setDaemon(true);
        thread.start();
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

        File selectedFile = fileChooser.showOpenDialog(rootScrollPane.getScene().getWindow());
        if (selectedFile != null) {
            currentGame.setExecutablePath(selectedFile.getAbsolutePath());
            currentGame.setInstallPath(selectedFile.getParent());
            currentGame.setStatus(Game.Status.READY);

            gameService.saveGame(currentGame);
            populateView();

            if (mainController != null) {
                mainController.showToast("Executable Located", "Game is now ready to play!");
            }
        }
    }

    /**
     * Opens the edit game dialog.
     */
    private void openEditDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nexus/views/EditGameDialog.fxml"));
            VBox dialogContent = loader.load();
            EditGameDialogController dialogController = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(NexusLauncherApp.getPrimaryStage());
            dialogStage.setTitle("Edit Game");

            Scene scene = new Scene(dialogContent);
            scene.getStylesheets().add(getClass().getResource("/com/nexus/styles/application.css").toExternalForm());

            dialogStage.setScene(scene);
            dialogController.setDialogStage(dialogStage);
            dialogController.setGame(currentGame);

            dialogController.setOnGameUpdated(updatedGame -> {
                populateView();
                if (mainController != null) {
                    mainController.showToast("Game Updated", updatedGame.getTitle() + " has been updated.");
                }
            });

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

    /**
     * Shows a confirmation dialog before deleting the game.
     */
    private void confirmDeleteGame() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Game");
        alert.setHeaderText("Delete " + currentGame.getTitle() + "?");
        alert.setContentText("This will remove the game from your library. This action cannot be undone.");

        // Style the dialog
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/com/nexus/styles/application.css").toExternalForm());
        dialogPane.getStyleClass().add("custom-dialog");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                deleteGame();
            }
        });
    }

    /**
     * Deletes the current game from the library.
     */
    private void deleteGame() {
        Task<Void> deleteTask = new Task<>() {
            @Override
            protected Void call() {
                gameService.deleteGame(currentGame);
                return null;
            }
        };

        deleteTask.setOnSucceeded(e -> Platform.runLater(() -> {
            if (mainController != null) {
                mainController.showToast("Game Deleted", currentGame.getTitle() + " has been removed.");
                mainController.backToLibraryAndRefresh();
            }
        }));

        deleteTask.setOnFailed(e -> Platform.runLater(() -> {
            if (mainController != null) {
                mainController.showToast("Error", "Failed to delete game.");
            }
        }));

        Thread thread = new Thread(deleteTask);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Shows a confirmation dialog before ignoring the game.
     */
    private void confirmIgnoreGame() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Ignore Game");
        alert.setHeaderText("Ignore " + currentGame.getTitle() + "?");
        alert.setContentText("Are you sure? This will hide the game from your library and future scans.\n\nYou can restore hidden games from Settings.");

        // Style the dialog
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/com/nexus/styles/application.css").toExternalForm());
        dialogPane.getStyleClass().add("custom-dialog");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                ignoreGame();
            }
        });
    }

    /**
     * Ignores the current game - removes from library and prevents future detection.
     */
    private void ignoreGame() {
        Task<Void> ignoreTask = new Task<>() {
            @Override
            protected Void call() {
                gameService.ignoreGame(currentGame);
                return null;
            }
        };

        ignoreTask.setOnSucceeded(e -> Platform.runLater(() -> {
            if (mainController != null) {
                mainController.showToast("Game Hidden", currentGame.getTitle() + " has been hidden from your library.");
                mainController.backToLibraryAndRefresh();
            }
        }));

        ignoreTask.setOnFailed(e -> Platform.runLater(() -> {
            if (mainController != null) {
                mainController.showToast("Error", "Failed to ignore game.");
            }
        }));

        Thread thread = new Thread(ignoreTask);
        thread.setDaemon(true);
        thread.start();
    }
}

