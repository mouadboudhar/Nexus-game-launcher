package com.nexus.controller;

import com.nexus.NexusLauncherApp;
import com.nexus.component.GameCard;
import com.nexus.service.GameLauncher;
import com.nexus.service.GameService;
import com.nexus.service.ScanTask;
import com.nexus.model.Game;
import com.nexus.util.HibernateUtil;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the Library view displaying game cards.
 */
public class LibraryController implements Initializable {

    @FXML private VBox rootContainer;
    @FXML private TextField searchField;
    @FXML private Button addGameButton;
    @FXML private Label gameCountLabel;
    @FXML private FlowPane gamesGrid;
    @FXML private VBox loadingOverlay;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label loadingLabel;

    private MainController mainController;
    private final GameService gameService = GameService.getInstance();
    private final GameLauncher gameLauncher = new GameLauncher();

    private ScanTask currentScanTask;
    private boolean isScanning = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup search listener
        setupSearchListener();

        // Initialize database in background, then load games
        initializeDatabaseAndLoad();
    }

    /**
     * Initializes the database and loads games after completion.
     */
    private void initializeDatabaseAndLoad() {
        Task<Void> initTask = new Task<>() {
            @Override
            protected Void call() {
                HibernateUtil.initialize();
                return null;
            }
        };

        initTask.setOnSucceeded(e -> {
            // First load existing games from DB
            loadGamesFromDatabase();

            // Then trigger scan after a short delay to let UI settle
            Platform.runLater(() -> {
                // Use a delayed scan to avoid race conditions
                javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
                delay.setOnFinished(event -> runInitialScan());
                delay.play();
            });
        });

        initTask.setOnFailed(e -> {
            System.err.println("[LibraryController] Database initialization failed: " + initTask.getException());
            // Still try to show empty state
            Platform.runLater(() -> {
                displayGames(new java.util.ArrayList<>());
                updateGameCount(0);
            });
        });

        Thread thread = new Thread(initTask);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Runs the initial background scan on startup.
     */
    private void runInitialScan() {
        if (isScanning) {
            return;
        }

        isScanning = true;
        currentScanTask = new ScanTask();

        // Show loading state
        Platform.runLater(() -> {
            showLoading(true, "Scanning for games...");
            gameCountLabel.setText("SCANNING...");
        });

        currentScanTask.setOnSucceeded(e -> {
            List<Game> games = currentScanTask.getValue();
            Platform.runLater(() -> {
                showLoading(false, null);
                displayGames(games);
                updateGameCount(games.size());
                isScanning = false;
                if (mainController != null) {
                    mainController.showToast("Scan Complete", "Found " + games.size() + " games");
                }
            });
        });

        currentScanTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                showLoading(false, null);
                isScanning = false;
                loadGamesFromDatabase(); // Fall back to database
                System.err.println("[LibraryController] Scan failed: " + currentScanTask.getException());
            });
        });

        Thread scanThread = new Thread(currentScanTask);
        scanThread.setDaemon(true);
        scanThread.start();
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    /**
     * Shows or hides the loading overlay.
     */
    private void showLoading(boolean show, String message) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisible(show);
            loadingOverlay.setManaged(show);
            if (message != null && loadingLabel != null) {
                loadingLabel.setText(message);
            }
        }
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterGames(newValue);
        });
    }

    /**
     * Loads games from the database.
     */
    private void loadGamesFromDatabase() {
        Task<List<Game>> loadTask = new Task<>() {
            @Override
            protected List<Game> call() {
                return gameService.getAllGames();
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<Game> games = loadTask.getValue();
            Platform.runLater(() -> {
                displayGames(games);
                updateGameCount(games.size());
            });
        });

        loadTask.setOnFailed(e -> {
            System.err.println("[LibraryController] Failed to load games: " + loadTask.getException());
        });

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Triggers a manual re-scan (called from Sync button).
     */
    public void triggerRescan() {
        runInitialScan();
    }

    public void refreshGames() {
        searchField.clear();
        loadGamesFromDatabase();
    }

    private void filterGames(String query) {
        Task<List<Game>> searchTask = new Task<>() {
            @Override
            protected List<Game> call() {
                return gameService.searchGames(query);
            }
        };

        searchTask.setOnSucceeded(e -> {
            List<Game> games = searchTask.getValue();
            Platform.runLater(() -> {
                displayGames(games);
                updateGameCount(games.size());
            });
        });

        Thread thread = new Thread(searchTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void displayGames(List<Game> games) {
        gamesGrid.getChildren().clear();

        for (Game game : games) {
            GameCard card = new GameCard(game);
            card.setOnCardClick(() -> openGameDetails(game));
            card.setOnPlayClick(() -> launchGame(game));
            gamesGrid.getChildren().add(card);
        }

        // Add the "Add Game" placeholder card
        gamesGrid.getChildren().add(createAddGamePlaceholder());
    }

    private StackPane createAddGamePlaceholder() {
        StackPane placeholder = new StackPane();
        placeholder.getStyleClass().add("game-card-placeholder");
        placeholder.setPrefWidth(180);
        placeholder.setPrefHeight(240);

        VBox content = new VBox(8);
        content.setAlignment(javafx.geometry.Pos.CENTER);

        // Use FontIcon for the add icon
        FontIcon addIcon = FontIcon.of(FontAwesomeSolid.PLUS, 36);
        addIcon.setOpacity(0.6);
        addIcon.getStyleClass().add("placeholder-icon");

        Label addText = new Label("Add Game");
        addText.getStyleClass().add("placeholder-text");

        content.getChildren().addAll(addIcon, addText);
        placeholder.getChildren().add(content);

        placeholder.setOnMouseClicked(e -> openAddGameDialog());

        return placeholder;
    }

    private void updateGameCount(int count) {
        gameCountLabel.setText(count + " GAMES");
    }

    private void openGameDetails(Game game) {
        if (mainController != null) {
            mainController.showGameDetails(game);
        }
    }

    /**
     * Launches a game using the GameLauncher service.
     */
    private void launchGame(Game game) {
        if (game.getStatus() == Game.Status.MISSING) {
            if (mainController != null) {
                mainController.showToast("Cannot Launch", "Game executable not found: " + game.getTitle());
            }
            return;
        }

        // Launch in background thread
        Task<Boolean> launchTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return gameLauncher.play(game);
            }
        };

        launchTask.setOnSucceeded(e -> {
            if (mainController != null) {
                Platform.runLater(() -> {
                    mainController.showToast("Game Launched", "Starting " + game.getTitle() + "...");
                });
            }
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

    @FXML
    private void openAddGameDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nexus/views/AddGameDialog.fxml"));
            VBox dialogContent = loader.load();
            AddGameDialogController dialogController = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(NexusLauncherApp.getPrimaryStage());
            dialogStage.setTitle("Add Game");

            Scene scene = new Scene(dialogContent);
            scene.getStylesheets().add(getClass().getResource("/com/nexus/styles/application.css").toExternalForm());

            dialogStage.setScene(scene);
            dialogController.setDialogStage(dialogStage);
            dialogController.setOnGameAdded(game -> {
                // Save to database
                gameService.saveGame(game);
                loadGamesFromDatabase();
            });

            dialogStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

