package com.nexus.controller;

import com.nexus.NexusLauncherApp;
import com.nexus.component.GameCard;
import com.nexus.service.GameLauncher;
import com.nexus.service.GameService;
import com.nexus.service.ScannerService;
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
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the Library view displaying game cards.
 */
public class LibraryController implements Initializable {

    @FXML private VBox rootContainer;
    @FXML private TextField searchField;
    @FXML private Button addGameButton;
    @FXML private Button scanButton;
    @FXML private FontIcon scanButtonIcon;
    @FXML private Label gameCountLabel;
    @FXML private FlowPane gamesGrid;

    // Non-blocking scan status UI
    @FXML private HBox scanStatusContainer;
    @FXML private ProgressIndicator scanProgressIndicator;
    @FXML private Label scanStatusLabel;

    private MainController mainController;
    private final GameService gameService = GameService.getInstance();
    private final GameLauncher gameLauncher = new GameLauncher();

    private Task<?> currentScanTask;
    private boolean isScanning = false;

    // Cache of currently displayed games to avoid unnecessary re-renders
    private List<Game> cachedGames = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup search listener
        setupSearchListener();

        // Initialize database and load existing games IMMEDIATELY
        initializeDatabaseAndLoad();
    }

    /**
     * Initializes the database and loads games instantly from DB.
     * Background scan runs AFTER UI is ready and non-blocking.
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
            // IMMEDIATELY load existing games from DB - user can interact right away
            loadGamesFromDatabase();

            // Run background scan AFTER UI is loaded (non-blocking, incremental)
            Platform.runLater(() -> {
                javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(
                        javafx.util.Duration.millis(1000));
                delay.setOnFinished(event -> runBackgroundIncrementalScan());
                delay.play();
            });
        });

        initTask.setOnFailed(e -> {
            System.err.println("[LibraryController] Database initialization failed: " + initTask.getException());
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
     * Runs an incremental background scan that only looks for changes.
     * This is NON-BLOCKING - the UI remains fully interactive.
     */
    private void runBackgroundIncrementalScan() {
        if (isScanning) {
            return;
        }

        isScanning = true;

        // Update UI to show scanning state
        Platform.runLater(() -> {
            showScanStatus(true, "Checking for new games...");
            updateScanButtonState(true);
        });

        Task<ScanResult> scanTask = new Task<>() {
            @Override
            protected ScanResult call() {
                ScannerService scannerService = new ScannerService();

                updateMessage("Scanning for changes...");

                // Get current games from DB
                List<Game> existingGames = gameService.getAllGames();
                int existingCount = existingGames.size();

                // Perform scan (this merges with DB internally)
                List<Game> scannedGames = scannerService.scanAll();

                int newGamesFound = scannedGames.size() - existingCount;

                return new ScanResult(scannedGames, newGamesFound);
            }
        };

        scanTask.messageProperty().addListener((obs, oldMsg, newMsg) ->
            Platform.runLater(() -> {
                if (scanStatusLabel != null) {
                    scanStatusLabel.setText(newMsg);
                }
            })
        );

        scanTask.setOnSucceeded(e -> {
            ScanResult result = scanTask.getValue();
            Platform.runLater(() -> {
                showScanStatus(false, null);
                updateScanButtonState(false);

                // Always refresh display after manual scan
                displayGames(result.games);
                updateGameCount(result.games.size());

                if (mainController != null) {
                    if (result.newGamesFound > 0) {
                        mainController.showToast("Scan Complete",
                                "Found " + result.newGamesFound + " new game(s)");
                    } else if (result.newGamesFound < 0) {
                        mainController.showToast("Scan Complete",
                                "Library updated (" + Math.abs(result.newGamesFound) + " game(s) removed)");
                    } else {
                        mainController.showToast("Scan Complete", "No changes found");
                    }
                }

                isScanning = false;
            });
        });

        scanTask.setOnFailed(e ->
            Platform.runLater(() -> {
                showScanStatus(false, null);
                updateScanButtonState(false);
                isScanning = false;
                System.err.println("[LibraryController] Background scan failed: " + scanTask.getException());
            })
        );

        scanTask.setOnCancelled(e ->
            Platform.runLater(() -> {
                showScanStatus(false, null);
                updateScanButtonState(false);
                isScanning = false;
                if (mainController != null) {
                    mainController.showToast("Scan Cancelled", "Scan was cancelled");
                }
            })
        );

        currentScanTask = scanTask;
        Thread scanThread = new Thread(scanTask);
        scanThread.setDaemon(true);
        scanThread.start();
    }

    /**
     * Handles scan button click - starts or cancels scan.
     */
    @FXML
    private void onScanButtonClick() {
        if (isScanning) {
            cancelScan();
        } else {
            runBackgroundIncrementalScan();
        }
    }

    /**
     * Cancels the current scan operation.
     */
    private void cancelScan() {
        if (currentScanTask != null && currentScanTask.isRunning()) {
            currentScanTask.cancel();
        }
    }

    /**
     * Updates the scan button appearance based on scanning state.
     */
    private void updateScanButtonState(boolean scanning) {
        if (scanButton != null) {
            if (scanning) {
                scanButton.setText("Cancel");
                scanButton.getStyleClass().remove("secondary-button");
                scanButton.getStyleClass().add("danger-button");
                if (scanButtonIcon != null) {
                    scanButtonIcon.setIconLiteral("fas-times");
                }
            } else {
                scanButton.setText("Scan");
                scanButton.getStyleClass().remove("danger-button");
                if (!scanButton.getStyleClass().contains("secondary-button")) {
                    scanButton.getStyleClass().add("secondary-button");
                }
                if (scanButtonIcon != null) {
                    scanButtonIcon.setIconLiteral("fas-sync-alt");
                }
            }
        }
    }

    /**
     * Simple result class for scan operations.
     */
    private static class ScanResult {
        final List<Game> games;
        final int newGamesFound;

        ScanResult(List<Game> games, int newGamesFound) {
            this.games = games;
            this.newGamesFound = newGamesFound;
        }
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    /**
     * Shows or hides the subtle scan status indicator.
     */
    private void showScanStatus(boolean show, String message) {
        if (scanStatusContainer != null) {
            scanStatusContainer.setVisible(show);
            scanStatusContainer.setManaged(show);
        }
        if (message != null && scanStatusLabel != null) {
            scanStatusLabel.setText(message);
        }
    }

    private void setupSearchListener() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterGames(newValue);
        });
    }

    /**
     * Loads games from the database (fast, instant).
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
     * Triggers a manual re-scan (called from Sync button or Scan view).
     */
    public void triggerRescan() {
        runBackgroundIncrementalScan();
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
        // Sort games alphabetically by title for consistent ordering
        List<Game> sortedGames = new ArrayList<>(games);
        sortedGames.sort((a, b) -> {
            String titleA = a.getTitle() != null ? a.getTitle().toLowerCase() : "";
            String titleB = b.getTitle() != null ? b.getTitle().toLowerCase() : "";
            return titleA.compareTo(titleB);
        });

        // Cache the sorted games
        this.cachedGames = sortedGames;

        gamesGrid.getChildren().clear();

        for (Game game : sortedGames) {
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
                gameService.saveGame(game);
                loadGamesFromDatabase();
            });

            dialogStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

