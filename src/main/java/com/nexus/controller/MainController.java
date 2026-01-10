package com.nexus.controller;

import com.nexus.model.Game;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Main controller handling the shell layout and navigation.
 */
public class MainController implements Initializable {

    @FXML private BorderPane rootPane;
    @FXML private VBox sidebar;
    @FXML private Button navLibrary;
    @FXML private Button navScan;
    @FXML private Button navFavorites;
    @FXML private Button navSettings;

    private Button currentActiveNav;
    private Node libraryView;
    private LibraryController libraryController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentActiveNav = navLibrary;
        loadLibraryView();
    }

    private void loadLibraryView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nexus/views/LibraryView.fxml"));
            libraryView = loader.load();
            libraryController = loader.getController();
            libraryController.setMainController(this);
            rootPane.setCenter(libraryView);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load Library view");
        }
    }

    @FXML
    private void onNavLibrary() {
        setActiveNav(navLibrary);
        if (libraryView != null) {
            rootPane.setCenter(libraryView);
        } else {
            loadLibraryView();
        }
    }

    @FXML
    private void onNavScan() {
        setActiveNav(navScan);
        loadView("/com/nexus/views/ScanView.fxml");
    }

    @FXML
    private void onNavFavorites() {
        setActiveNav(navFavorites);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nexus/views/FavoritesView.fxml"));
            Node view = loader.load();
            FavoritesController controller = loader.getController();
            controller.setMainController(this);
            rootPane.setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load Favorites view");
        }
    }

    @FXML
    private void onNavSettings() {
        setActiveNav(navSettings);
        loadView("/com/nexus/views/SettingsView.fxml");
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();
            rootPane.setCenter(view);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load view: " + fxmlPath);
        }
    }

    private void setActiveNav(Button newActive) {
        if (currentActiveNav != null) {
            currentActiveNav.getStyleClass().remove("nav-button-active");
        }
        newActive.getStyleClass().add("nav-button-active");
        currentActiveNav = newActive;
    }

    /**
     * Navigate to game details view.
     */
    public void showGameDetails(Game game) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nexus/views/GameDetailsView.fxml"));
            Node view = loader.load();
            GameDetailsController controller = loader.getController();
            controller.setMainController(this);
            controller.setGame(game);
            rootPane.setCenter(view);

            // Clear active nav state when viewing details
            if (currentActiveNav != null) {
                currentActiveNav.getStyleClass().remove("nav-button-active");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to load Game Details view");
        }
    }

    /**
     * Return to library view from game details.
     */
    public void backToLibrary() {
        setActiveNav(navLibrary);
        if (libraryView != null && libraryController != null) {
            libraryController.refreshGames();
            rootPane.setCenter(libraryView);
        } else {
            loadLibraryView();
        }
    }

    /**
     * Show a toast notification.
     */
    public void showToast(String title, String message) {
        // Toast notification will be implemented with a custom overlay
        System.out.println("[TOAST] " + title + ": " + message);
    }

    private void showError(String message) {
        System.err.println("[ERROR] " + message);
    }
}
