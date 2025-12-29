package com.nexus.client.component;

import com.nexus.shared.model.Game;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * Custom component representing a game card in the library grid.
 */
public class GameCard extends StackPane {

    private final Game game;
    private Runnable onCardClick;
    private Runnable onPlayClick;

    private ImageView coverImage;
    private VBox overlay;
    private Button playButton;

    private static final double CARD_WIDTH = 180;
    private static final double CARD_HEIGHT = 240;

    public GameCard(Game game) {
        this.game = game;
        initialize();
    }

    private void initialize() {
        setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        setMinSize(CARD_WIDTH, CARD_HEIGHT);
        setMaxSize(CARD_WIDTH, CARD_HEIGHT);
        getStyleClass().add("game-card");

        // Clip the card to rounded corners
        Rectangle clip = new Rectangle(CARD_WIDTH, CARD_HEIGHT);
        clip.setArcWidth(16);
        clip.setArcHeight(16);
        setClip(clip);

        // Build card layers
        createCoverImage();
        createGradientOverlay();
        createInfoSection();
        createHoverOverlay();

        // Apply styling based on game status
        if (game.getStatus() == Game.Status.MISSING) {
            getStyleClass().add("game-card-missing");
        }

        setupInteractions();
    }

    private void createCoverImage() {
        coverImage = new ImageView();
        coverImage.setFitWidth(CARD_WIDTH);
        coverImage.setFitHeight(CARD_HEIGHT);
        coverImage.setPreserveRatio(false);
        coverImage.getStyleClass().add("game-card-image");

        // Load image asynchronously
        String coverUrl = game.getCoverImageUrl();
        if (coverUrl != null && !coverUrl.isEmpty() && !coverUrl.startsWith("/assets/")) {
            try {
                // Create image with background loading
                Image image = new Image(coverUrl, CARD_WIDTH * 2, CARD_HEIGHT * 2, true, true, true);

                // Handle loading errors
                image.errorProperty().addListener((obs, wasError, isError) -> {
                    if (isError) {
                        setPlaceholderImage();
                    }
                });

                // Set image when loaded or immediately if cached
                if (!image.isError()) {
                    coverImage.setImage(image);
                } else {
                    setPlaceholderImage();
                }
            } catch (Exception e) {
                // Use placeholder if image fails to load
                setPlaceholderImage();
            }
        } else {
            setPlaceholderImage();
        }

        getChildren().add(coverImage);
    }

    private void setPlaceholderImage() {
        // Create a gradient placeholder with game initial
        setStyle("-fx-background-color: linear-gradient(to bottom, #4f46e5, #1f2937);");
    }

    private void createGradientOverlay() {
        // Dark gradient overlay for text readability
        Region gradientOverlay = new Region();
        gradientOverlay.getStyleClass().add("game-card-gradient");
        gradientOverlay.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        getChildren().add(gradientOverlay);
    }

    private void createInfoSection() {
        VBox infoBox = new VBox(4);
        infoBox.setAlignment(Pos.BOTTOM_LEFT);
        infoBox.setPadding(new Insets(16));
        infoBox.setPickOnBounds(false);

        // Game title
        Label titleLabel = new Label(game.getTitle());
        titleLabel.getStyleClass().add("game-card-title");
        titleLabel.setMaxWidth(CARD_WIDTH - 32);

        // Platform and status badges
        HBox badges = new HBox(8);
        badges.setAlignment(Pos.CENTER_LEFT);

        // Platform badge with icon
        HBox platformBadge = new HBox(4);
        platformBadge.setAlignment(Pos.CENTER_LEFT);
        platformBadge.getStyleClass().addAll("badge", "badge-" + game.getPlatform().name().toLowerCase());

        // Platform icon
        ImageView platformIcon = new ImageView();
        platformIcon.setFitWidth(12);
        platformIcon.setFitHeight(12);
        platformIcon.setPreserveRatio(true);

        String platformIconPath = switch (game.getPlatform()) {
            case STEAM -> "/assets/steam.png";
            case EPIC -> "/assets/epic.png";
            default -> "/assets/folder.png";
        };

        try {
            platformIcon.setImage(new Image(getClass().getResourceAsStream(platformIconPath)));
        } catch (Exception e) {
            // Fallback if icon not found
        }

        Label platformLabel = new Label(game.getPlatform().getDisplayName().toUpperCase());
        platformBadge.getChildren().addAll(platformIcon, platformLabel);

        // Status indicator with icon
        HBox statusBadge = new HBox(4);
        statusBadge.setAlignment(Pos.CENTER_LEFT);

        ImageView statusIcon = new ImageView();
        statusIcon.setFitWidth(10);
        statusIcon.setFitHeight(10);
        statusIcon.setPreserveRatio(true);

        Label statusLabel = new Label();
        if (game.getStatus() == Game.Status.READY) {
            try {
                statusIcon.setImage(new Image(getClass().getResourceAsStream("/assets/check.png")));
            } catch (Exception e) {}
            statusLabel.setText("Ready");
            statusBadge.getStyleClass().addAll("status-label", "status-ready");
        } else if (game.getStatus() == Game.Status.MISSING) {
            try {
                statusIcon.setImage(new Image(getClass().getResourceAsStream("/assets/folder.png")));
            } catch (Exception e) {}
            statusLabel.setText("Missing");
            statusBadge.getStyleClass().addAll("status-label", "status-missing");
        }

        statusBadge.getChildren().addAll(statusIcon, statusLabel);

        badges.getChildren().addAll(platformBadge, statusBadge);
        infoBox.getChildren().addAll(titleLabel, badges);

        StackPane.setAlignment(infoBox, Pos.BOTTOM_LEFT);
        getChildren().add(infoBox);
    }

    private void createHoverOverlay() {
        overlay = new VBox();
        overlay.setAlignment(Pos.CENTER);
        overlay.getStyleClass().add("game-card-overlay");
        overlay.setOpacity(0);
        overlay.setPickOnBounds(false);

        // Create button with icon
        HBox buttonContent = new HBox(6);
        buttonContent.setAlignment(Pos.CENTER);

        ImageView buttonIcon = new ImageView();
        buttonIcon.setFitWidth(16);
        buttonIcon.setFitHeight(16);
        buttonIcon.setPreserveRatio(true);

        Label buttonLabel = new Label();

        if (game.getStatus() == Game.Status.MISSING) {
            try {
                buttonIcon.setImage(new Image(getClass().getResourceAsStream("/assets/folder.png")));
            } catch (Exception e) {}
            buttonLabel.setText("LOCATE");
            playButton = new Button();
            playButton.getStyleClass().addAll("play-button", "locate-button");
        } else {
            try {
                buttonIcon.setImage(new Image(getClass().getResourceAsStream("/assets/play.png")));
            } catch (Exception e) {}
            buttonLabel.setText("PLAY");
            playButton = new Button();
            playButton.getStyleClass().add("play-button");
        }

        buttonContent.getChildren().addAll(buttonIcon, buttonLabel);
        playButton.setGraphic(buttonContent);

        playButton.setOnAction(e -> {
            e.consume();
            if (onPlayClick != null) {
                onPlayClick.run();
            }
        });

        overlay.getChildren().add(playButton);
        getChildren().add(overlay);
    }

    private void setupInteractions() {
        // Hover effects
        setOnMouseEntered(e -> {
            animateHoverIn();
        });

        setOnMouseExited(e -> {
            animateHoverOut();
        });

        // Click handler
        setOnMouseClicked(e -> {
            if (e.getTarget() != playButton && onCardClick != null) {
                onCardClick.run();
            }
        });
    }

    private void animateHoverIn() {
        // Scale up
        ScaleTransition scale = new ScaleTransition(Duration.millis(200), this);
        scale.setToX(1.02);
        scale.setToY(1.02);
        scale.play();

        // Show overlay
        FadeTransition fade = new FadeTransition(Duration.millis(200), overlay);
        fade.setToValue(1.0);
        fade.play();

        // Scale cover image
        ScaleTransition imgScale = new ScaleTransition(Duration.millis(300), coverImage);
        imgScale.setToX(1.1);
        imgScale.setToY(1.1);
        imgScale.play();

        getStyleClass().add("game-card-hover");
    }

    private void animateHoverOut() {
        // Scale back
        ScaleTransition scale = new ScaleTransition(Duration.millis(200), this);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.play();

        // Hide overlay
        FadeTransition fade = new FadeTransition(Duration.millis(200), overlay);
        fade.setToValue(0.0);
        fade.play();

        // Scale cover image back
        ScaleTransition imgScale = new ScaleTransition(Duration.millis(300), coverImage);
        imgScale.setToX(1.0);
        imgScale.setToY(1.0);
        imgScale.play();

        getStyleClass().remove("game-card-hover");
    }

    public void setOnCardClick(Runnable handler) {
        this.onCardClick = handler;
    }

    public void setOnPlayClick(Runnable handler) {
        this.onPlayClick = handler;
    }

    public Game getGame() {
        return game;
    }
}

