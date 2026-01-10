package com.nexus.component;

import com.nexus.util.PlaceholderImageUtil;
import com.nexus.model.Game;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.kordamp.ikonli.fontawesome5.FontAwesomeBrands;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignG;

/**
 * Custom component representing a game card in the library grid.
 */
public class GameCard extends StackPane {

    private final Game game;
    private Runnable onCardClick;
    private Runnable onPlayClick;

    private ImageView coverImage;
    private VBox overlay;

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
        // Use placehold.co API for placeholder images with game title
        String gameTitle = game.getTitle() != null ? game.getTitle() : "Game";
        int width = (int) (CARD_WIDTH * 2);
        int height = (int) (CARD_HEIGHT * 2);
        String placeholderUrl = PlaceholderImageUtil.getCoverPlaceholder(gameTitle, width, height);

        try {
            Image placeholderImage = new Image(placeholderUrl, CARD_WIDTH * 2, CARD_HEIGHT * 2, true, true, true);
            placeholderImage.errorProperty().addListener((obs, wasError, isError) -> {
                if (isError) {
                    // Fallback to styled background with game title
                    showLocalPlaceholder(gameTitle);
                }
            });

            // Also check progress - when loaded, set it
            placeholderImage.progressProperty().addListener((obs, oldProgress, newProgress) -> {
                if (newProgress.doubleValue() >= 1.0 && !placeholderImage.isError()) {
                    coverImage.setImage(placeholderImage);
                }
            });

            if (placeholderImage.getProgress() >= 1.0 && !placeholderImage.isError()) {
                coverImage.setImage(placeholderImage);
            } else if (placeholderImage.isError()) {
                showLocalPlaceholder(gameTitle);
            }
        } catch (Exception e) {
            // Fallback to styled background
            showLocalPlaceholder(gameTitle);
        }
    }

    private void showLocalPlaceholder(String gameTitle) {
        // Set gradient background
        setStyle("-fx-background-color: linear-gradient(to bottom, #4f46e5, #1f2937);");

        // Add a label with the game title if not already present
        boolean hasLabel = getChildren().stream().anyMatch(n -> n instanceof Label && "placeholder-title".equals(n.getId()));
        if (!hasLabel) {
            Label titleLabel = new Label(gameTitle);
            titleLabel.setId("placeholder-title");
            titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-wrap-text: true; -fx-text-alignment: center; -fx-padding: 16;");
            titleLabel.setMaxWidth(CARD_WIDTH - 20);
            titleLabel.setWrapText(true);
            StackPane.setAlignment(titleLabel, Pos.CENTER);
            getChildren().add(1, titleLabel); // Add after coverImage but before other overlays
        }
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

        // Platform icon using FontIcon
        FontIcon platformIcon = switch (game.getPlatform()) {
            case STEAM -> FontIcon.of(FontAwesomeBrands.STEAM, 12);
            case EPIC -> FontIcon.of(MaterialDesignG.GAMEPAD_SQUARE, 12);
            default -> FontIcon.of(MaterialDesignF.FOLDER, 12);
        };
        platformIcon.getStyleClass().add("badge-icon");

        Label platformLabel = new Label(game.getPlatform().getDisplayName().toUpperCase());
        platformBadge.getChildren().addAll(platformIcon, platformLabel);

        // Status indicator with icon
        HBox statusBadge = new HBox(4);
        statusBadge.setAlignment(Pos.CENTER_LEFT);

        FontIcon statusIcon;
        Label statusLabel = new Label();
        if (game.getStatus() == Game.Status.READY) {
            statusIcon = FontIcon.of(FontAwesomeSolid.CHECK_CIRCLE, 10);
            statusLabel.setText("Ready");
            statusBadge.getStyleClass().addAll("status-label", "status-ready");
        } else {
            statusIcon = FontIcon.of(MaterialDesignF.FOLDER_ALERT, 10);
            statusLabel.setText("Missing");
            statusBadge.getStyleClass().addAll("status-label", "status-missing");
        }
        statusIcon.getStyleClass().add("status-icon");

        statusBadge.getChildren().addAll(statusIcon, statusLabel);

        badges.getChildren().addAll(platformBadge, statusBadge);
        infoBox.getChildren().addAll(titleLabel, badges);

        StackPane.setAlignment(infoBox, Pos.BOTTOM_LEFT);
        getChildren().add(infoBox);
    }

    private void createHoverOverlay() {
        // Simple overlay - single StackPane with background and centered button
        overlay = new VBox();
        overlay.setAlignment(Pos.CENTER);
        overlay.setMinSize(CARD_WIDTH, CARD_HEIGHT);
        overlay.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        overlay.setMaxSize(CARD_WIDTH, CARD_HEIGHT);
        overlay.setStyle("-fx-background-color: rgba(17, 24, 39, 0.85);");

        // Start completely invisible and non-interactive
        overlay.setVisible(false);
        overlay.setOpacity(0);

        // Create the play button
        Button playBtn = createPlayButton();
        overlay.getChildren().add(playBtn);

        getChildren().add(overlay);
    }

    private Button createPlayButton() {
        HBox buttonContent = new HBox(8);
        buttonContent.setAlignment(Pos.CENTER);

        FontIcon buttonIcon;
        Label buttonLabel = new Label();
        buttonLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        Button btn;
        if (game.getStatus() == Game.Status.MISSING) {
            buttonIcon = FontIcon.of(MaterialDesignF.FOLDER_SEARCH, 18);
            buttonLabel.setText("LOCATE");
            btn = new Button();
            btn.getStyleClass().addAll("play-button", "locate-button");
        } else {
            buttonIcon = FontIcon.of(FontAwesomeSolid.PLAY, 18);
            buttonLabel.setText("PLAY");
            btn = new Button();
            btn.getStyleClass().add("play-button");
        }
        buttonIcon.setIconColor(javafx.scene.paint.Color.WHITE);
        buttonIcon.getStyleClass().add("play-button-icon");

        buttonContent.getChildren().addAll(buttonIcon, buttonLabel);
        btn.setGraphic(buttonContent);

        btn.setOnAction(e -> {
            e.consume();
            if (onPlayClick != null) {
                onPlayClick.run();
            }
        });


        return btn;
    }

    private void animateHoverIn() {
        // Scale up
        ScaleTransition scale = new ScaleTransition(Duration.millis(200), this);
        scale.setToX(1.02);
        scale.setToY(1.02);
        scale.play();

        // Show overlay - make visible first, then fade in
        overlay.setVisible(true);
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

        // Hide overlay - fade out then set invisible
        FadeTransition fade = new FadeTransition(Duration.millis(200), overlay);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> overlay.setVisible(false));
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

    private void setupInteractions() {
        // Hover effects
        setOnMouseEntered(e -> animateHoverIn());
        setOnMouseExited(e -> animateHoverOut());

        // Card click handler
        setOnMouseClicked(e -> {
            if (onCardClick != null && e.getClickCount() == 1) {
                onCardClick.run();
            }
        });
    }
}

