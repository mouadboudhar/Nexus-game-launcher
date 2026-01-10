package com.nexus.controller;

import com.nexus.component.GameCard;
import com.nexus.service.GameLauncher;
import com.nexus.service.GameService;
import com.nexus.model.Game;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the Favorites view.
 */
public class FavoritesController implements Initializable {

    @FXML private VBox rootContainer;
    @FXML private Label gameCountLabel;
    @FXML private FlowPane favoritesGrid;
    @FXML private VBox emptyState;

    private MainController mainController;
    private final GameService gameService = GameService.getInstance();
    private final GameLauncher gameLauncher = new GameLauncher();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadFavorites();
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    private void loadFavorites() {
        Task<List<Game>> loadTask = new Task<>() {
            @Override
            protected List<Game> call() {
                return gameService.getFavoriteGames();
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<Game> favorites = loadTask.getValue();
            Platform.runLater(() -> {
                if (favorites.isEmpty()) {
                    showEmptyState();
                } else {
                    showFavorites(favorites);
                }
            });
        });

        loadTask.setOnFailed(e -> {
            Platform.runLater(this::showEmptyState);
        });

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    private void showEmptyState() {
        favoritesGrid.setVisible(false);
        favoritesGrid.setManaged(false);
        emptyState.setVisible(true);
        emptyState.setManaged(true);
        gameCountLabel.setText("0 GAMES");
    }

    private void showFavorites(List<Game> favorites) {
        emptyState.setVisible(false);
        emptyState.setManaged(false);
        favoritesGrid.setVisible(true);
        favoritesGrid.setManaged(true);

        favoritesGrid.getChildren().clear();

        for (Game game : favorites) {
            GameCard card = new GameCard(game);
            card.setOnCardClick(() -> openGameDetails(game));
            card.setOnPlayClick(() -> launchGame(game));
            favoritesGrid.getChildren().add(card);
        }

        gameCountLabel.setText(favorites.size() + " GAMES");
    }

    private void openGameDetails(Game game) {
        if (mainController != null) {
            mainController.showGameDetails(game);
        }
    }

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
            Platform.runLater(() -> {
                if (mainController != null) {
                    mainController.showToast("Game Launched", "Starting " + game.getTitle() + "...");
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

    public void refresh() {
        loadFavorites();
    }
}

