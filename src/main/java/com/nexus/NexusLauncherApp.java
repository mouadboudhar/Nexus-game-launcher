package com.nexus;

import atlantafx.base.theme.PrimerDark;
import com.nexus.util.HibernateUtil;
import com.nexus.util.WindowsThemeUtil;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Main entry point for the Nexus Launcher JavaFX application.
 * Uses the default OS window decorations (title bar).
 */
public class NexusLauncherApp extends Application {

    private static Stage primaryStage;
    private static final double MIN_WIDTH = 1024;
    private static final double MIN_HEIGHT = 700;

    @Override
    public void start(Stage stage) throws IOException {
        // Set the AtlantaFX dark theme (must be done after toolkit initialization)
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        primaryStage = stage;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("views/MainView.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("styles/application.css").toExternalForm());

        stage.setTitle("Nexus Launcher");
        stage.setScene(scene);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);

        // Try to set the application icon
        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/assets/icon.png")));
        } catch (Exception e) {
            // Icon not found, continue without it
        }

        stage.show();

        // Enable dark title bar on Windows
        WindowsThemeUtil.enableDarkTitleBar(stage);
    }

    @Override
    public void stop() {
        // Ensure Hibernate is shut down when app closes
        HibernateUtil.shutdown();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
