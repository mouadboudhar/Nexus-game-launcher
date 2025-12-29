package com.nexus.client;

import com.nexus.shared.util.HibernateUtil;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.ParallelTransition;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;

/**
 * Main entry point for the Nexus Launcher JavaFX application.
 */
public class NexusLauncherApp extends Application {

    private static Stage primaryStage;
    private static boolean isMaximized = false;
    private static double prevX, prevY, prevWidth, prevHeight;

    // Resize/drag state
    private static final int RESIZE_MARGIN = 8;
    private static final double MIN_WIDTH = 1024;
    private static final double MIN_HEIGHT = 700;
    private double dragOffsetX, dragOffsetY;
    private double startX, startY, startWidth, startHeight, startStageX, startStageY;
    private String resizeDirection = "";
    private boolean isDragging = false;
    private boolean isResizing = false;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;

        FXMLLoader loader = new FXMLLoader(getClass().getResource("views/MainView.fxml"));
        Parent root = loader.load();

        // Remove the OS Title Bar with transparent style for rounded corners
        stage.initStyle(StageStyle.TRANSPARENT);

        Scene scene = new Scene(root, 1280, 800);
        scene.setFill(Color.TRANSPARENT);
        scene.getStylesheets().add(getClass().getResource("styles/application.css").toExternalForm());

        stage.setTitle("Nexus Launcher");
        stage.setScene(scene);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);

        // Setup combined mouse handlers for dragging and resizing
        setupMouseHandlers(stage, scene, root);

        // Try to set the application icon
        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/assets/icon.png")));
        } catch (Exception e) {
            // Icon not found, continue without it
        }

        stage.show();
    }

    private void setupMouseHandlers(Stage stage, Scene scene, Parent root) {
        Region region = (Region) root;

        // Use event filters on scene to capture events before children consume them
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, event -> {
            if (isMaximized) {
                scene.setCursor(Cursor.DEFAULT);
                return;
            }
            updateCursor(scene, event, stage);
        });

        // Mouse pressed - determine action (resize or drag)
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (isMaximized) return;

            double x = event.getSceneX();
            double y = event.getSceneY();
            double width = stage.getWidth();
            double height = stage.getHeight();

            // Store initial state
            startX = event.getScreenX();
            startY = event.getScreenY();
            startWidth = width;
            startHeight = height;
            startStageX = stage.getX();
            startStageY = stage.getY();

            // Determine resize direction
            resizeDirection = getResizeDirection(x, y, width, height);

            if (!resizeDirection.isEmpty()) {
                // We're resizing - consume the event so children don't get it
                isResizing = true;
                event.consume();
            } else if (y < 40) {
                // In title bar area, enable dragging
                isDragging = true;
                dragOffsetX = x;
                dragOffsetY = y;
            }
        });

        // Mouse dragged - perform resize or drag
        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (isMaximized) return;

            if (isResizing && !resizeDirection.isEmpty()) {
                // Resizing the window
                performResize(stage, event);
                event.consume();
            } else if (isDragging) {
                // Dragging the window
                stage.setX(event.getScreenX() - dragOffsetX);
                stage.setY(event.getScreenY() - dragOffsetY);
            }
        });

        // Mouse released - reset state
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> {
            if (isResizing) {
                event.consume();
            }
            isDragging = false;
            isResizing = false;
            resizeDirection = "";
        });
    }

    private void updateCursor(Scene scene, MouseEvent event, Stage stage) {
        double x = event.getSceneX();
        double y = event.getSceneY();
        double width = stage.getWidth();
        double height = stage.getHeight();

        String direction = getResizeDirection(x, y, width, height);

        switch (direction) {
            case "NW" -> scene.setCursor(Cursor.NW_RESIZE);
            case "NE" -> scene.setCursor(Cursor.NE_RESIZE);
            case "SW" -> scene.setCursor(Cursor.SW_RESIZE);
            case "SE" -> scene.setCursor(Cursor.SE_RESIZE);
            case "N" -> scene.setCursor(Cursor.N_RESIZE);
            case "S" -> scene.setCursor(Cursor.S_RESIZE);
            case "E" -> scene.setCursor(Cursor.E_RESIZE);
            case "W" -> scene.setCursor(Cursor.W_RESIZE);
            default -> scene.setCursor(Cursor.DEFAULT);
        }
    }

    private String getResizeDirection(double x, double y, double width, double height) {
        boolean left = x < RESIZE_MARGIN;
        boolean right = x > width - RESIZE_MARGIN;
        boolean top = y < RESIZE_MARGIN;
        boolean bottom = y > height - RESIZE_MARGIN;

        if (left && top) return "NW";
        if (right && top) return "NE";
        if (left && bottom) return "SW";
        if (right && bottom) return "SE";
        if (left) return "W";
        if (right) return "E";
        if (top) return "N";
        if (bottom) return "S";
        return "";
    }

    private void performResize(Stage stage, MouseEvent event) {
        double dx = event.getScreenX() - startX;
        double dy = event.getScreenY() - startY;

        double newWidth = startWidth;
        double newHeight = startHeight;
        double newX = startStageX;
        double newY = startStageY;

        switch (resizeDirection) {
            case "E" -> newWidth = Math.max(MIN_WIDTH, startWidth + dx);
            case "W" -> {
                newWidth = Math.max(MIN_WIDTH, startWidth - dx);
                if (newWidth > MIN_WIDTH) newX = startStageX + dx;
            }
            case "S" -> newHeight = Math.max(MIN_HEIGHT, startHeight + dy);
            case "N" -> {
                newHeight = Math.max(MIN_HEIGHT, startHeight - dy);
                if (newHeight > MIN_HEIGHT) newY = startStageY + dy;
            }
            case "SE" -> {
                newWidth = Math.max(MIN_WIDTH, startWidth + dx);
                newHeight = Math.max(MIN_HEIGHT, startHeight + dy);
            }
            case "SW" -> {
                newWidth = Math.max(MIN_WIDTH, startWidth - dx);
                newHeight = Math.max(MIN_HEIGHT, startHeight + dy);
                if (newWidth > MIN_WIDTH) newX = startStageX + dx;
            }
            case "NE" -> {
                newWidth = Math.max(MIN_WIDTH, startWidth + dx);
                newHeight = Math.max(MIN_HEIGHT, startHeight - dy);
                if (newHeight > MIN_HEIGHT) newY = startStageY + dy;
            }
            case "NW" -> {
                newWidth = Math.max(MIN_WIDTH, startWidth - dx);
                newHeight = Math.max(MIN_HEIGHT, startHeight - dy);
                if (newWidth > MIN_WIDTH) newX = startStageX + dx;
                if (newHeight > MIN_HEIGHT) newY = startStageY + dy;
            }
        }

        stage.setX(newX);
        stage.setY(newY);
        stage.setWidth(newWidth);
        stage.setHeight(newHeight);
    }

    /**
     * Minimize with smooth animation
     */
    public static void minimizeWithAnimation() {
        FadeTransition fade = new FadeTransition(Duration.millis(150), primaryStage.getScene().getRoot());
        fade.setFromValue(1.0);
        fade.setToValue(0.7);

        ScaleTransition scale = new ScaleTransition(Duration.millis(150), primaryStage.getScene().getRoot());
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(0.95);
        scale.setToY(0.95);

        ParallelTransition parallel = new ParallelTransition(fade, scale);
        parallel.setOnFinished(e -> {
            primaryStage.setIconified(true);
            // Reset for when restored
            primaryStage.getScene().getRoot().setOpacity(1.0);
            primaryStage.getScene().getRoot().setScaleX(1.0);
            primaryStage.getScene().getRoot().setScaleY(1.0);
        });
        parallel.play();
    }

    /**
     * Toggle maximize with smooth animation (keeps taskbar visible)
     */
    public static void toggleMaximizeWithAnimation() {
        FadeTransition fade = new FadeTransition(Duration.millis(100), primaryStage.getScene().getRoot());
        fade.setFromValue(1.0);
        fade.setToValue(0.9);
        fade.setAutoReverse(true);
        fade.setCycleCount(2);

        fade.setOnFinished(e -> {
            if (isMaximized) {
                // Restore to previous size
                primaryStage.setX(prevX);
                primaryStage.setY(prevY);
                primaryStage.setWidth(prevWidth);
                primaryStage.setHeight(prevHeight);
                isMaximized = false;
            } else {
                // Save current bounds
                prevX = primaryStage.getX();
                prevY = primaryStage.getY();
                prevWidth = primaryStage.getWidth();
                prevHeight = primaryStage.getHeight();

                // Get the visual bounds of the screen (excludes taskbar)
                Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
                primaryStage.setX(screenBounds.getMinX());
                primaryStage.setY(screenBounds.getMinY());
                primaryStage.setWidth(screenBounds.getWidth());
                primaryStage.setHeight(screenBounds.getHeight());
                isMaximized = true;
            }
        });
        fade.play();
    }

    /**
     * Close with smooth animation
     */
    public static void closeWithAnimation() {
        FadeTransition fade = new FadeTransition(Duration.millis(200), primaryStage.getScene().getRoot());
        fade.setFromValue(1.0);
        fade.setToValue(0.0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(200), primaryStage.getScene().getRoot());
        scale.setFromX(1.0);
        scale.setFromY(1.0);
        scale.setToX(0.9);
        scale.setToY(0.9);

        ParallelTransition parallel = new ParallelTransition(fade, scale);
        parallel.setOnFinished(e -> {
            // Shutdown Hibernate before exiting
            HibernateUtil.shutdown();
            javafx.application.Platform.exit();
            System.exit(0);
        });
        parallel.play();
    }

    @Override
    public void stop() {
        // Ensure Hibernate is shut down when app closes
        HibernateUtil.shutdown();
    }

    public static boolean isWindowMaximized() {
        return isMaximized;
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
