package com.nexus.component;

import com.nexus.model.Game;
import com.nexus.util.PlaceholderImageUtil;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import org.kordamp.ikonli.fontawesome5.FontAwesomeBrands;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignF;
import org.kordamp.ikonli.materialdesign2.MaterialDesignG;

import java.io.IOException;

/**
 * Custom FXML-based component representing a game card in the library grid.
 */
public class GameCard extends StackPane {

    private static final double CARD_WIDTH = 180;
    private static final double CARD_HEIGHT = 240;
    private static final double CORNER_RADIUS = 12;

    @FXML private ImageView coverImage;
    @FXML private Label titleLabel;
    @FXML private HBox platformBadge;
    @FXML private FontIcon platformIcon;
    @FXML private FontIcon statusCheckmark;

    private final Game game;
    private Runnable onCardClick;
    private Runnable onPlayClick;

    // Color adjustment for darkening the image
    private final ColorAdjust darkenEffect;

    public GameCard(Game game) {
        this.game = game;

        // Set fixed size constraints to prevent expansion
        setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        setMinSize(CARD_WIDTH, CARD_HEIGHT);
        setMaxSize(CARD_WIDTH, CARD_HEIGHT);

        // Create darkening effect for the cover image
        darkenEffect = new ColorAdjust();
        darkenEffect.setBrightness(-0.2); // Darken by 30%

        loadFXML();
        initialize();
    }

    private void loadFXML() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/nexus/views/GameCard.fxml"));
        loader.setRoot(this);
        loader.setController(this);

        try {
            loader.load();
        } catch (IOException e) {
            System.err.println("[GameCard] Failed to load FXML: " + e.getMessage());
            createFallbackLayout();
        }
    }

    private void initialize() {
        getStyleClass().add("game-card");

        // Bind cover image to card size
        coverImage.fitWidthProperty().bind(widthProperty());
        coverImage.fitHeightProperty().bind(heightProperty());

        // Apply darkening effect to make white text readable
        coverImage.setEffect(darkenEffect);

        // Apply clip for rounded corners
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        clip.setArcWidth(CORNER_RADIUS * 2);
        clip.setArcHeight(CORNER_RADIUS * 2);
        setClip(clip);

        // Set up game data
        setupGameData();

        // Set up interactions
        setupInteractions();

        // Apply missing style if needed
        if (game.getStatus() == Game.Status.MISSING) {
            getStyleClass().add("game-card-missing");
        }
    }

    private void setupGameData() {
        // Set title
        if (titleLabel != null) {
            titleLabel.setText(game.getTitle());
        }

        // Set platform icon
        if (platformIcon != null) {
            switch (game.getPlatform()) {
                case STEAM -> platformIcon.setIconCode(FontAwesomeBrands.STEAM);
                case EPIC -> platformIcon.setIconCode(MaterialDesignG.GAMEPAD_SQUARE);
                default -> platformIcon.setIconCode(MaterialDesignF.FOLDER);
            }
            platformIcon.setIconSize(18);
            platformIcon.setIconColor(Color.WHITE);
        }

        // Set status checkmark
        if (statusCheckmark != null) {
            boolean isReady = game.getStatus() == Game.Status.READY;
            statusCheckmark.setVisible(isReady);
            statusCheckmark.setManaged(isReady);
            if (isReady) {
                statusCheckmark.setIconCode(FontAwesomeSolid.CHECK_CIRCLE);
                statusCheckmark.setIconSize(18);
                statusCheckmark.setIconColor(Color.web("#22c55e"));
            }
        }

        // Load cover image
        loadCoverImage();
    }

    private void loadCoverImage() {
        if (coverImage == null) return;

        String coverUrl = game.getCoverImageUrl();
        if (coverUrl != null && !coverUrl.isEmpty() && !coverUrl.startsWith("/assets/")) {
            try {
                Image image = new Image(coverUrl, CARD_WIDTH * 2, CARD_HEIGHT * 2, true, true, true);

                image.errorProperty().addListener((obs, wasError, isError) -> {
                    if (isError) {
                        loadPlaceholderImage();
                    }
                });

                if (!image.isError()) {
                    coverImage.setImage(image);
                } else {
                    loadPlaceholderImage();
                }
            } catch (Exception e) {
                loadPlaceholderImage();
            }
        } else {
            loadPlaceholderImage();
        }
    }

    private void loadPlaceholderImage() {
        String gameTitle = game.getTitle() != null ? game.getTitle() : "Game";
        int width = (int) (CARD_WIDTH * 2);
        int height = (int) (CARD_HEIGHT * 2);
        String placeholderUrl = PlaceholderImageUtil.getCoverPlaceholder(gameTitle, width, height);

        try {
            Image placeholderImage = new Image(placeholderUrl, width, height, true, true, true);

            placeholderImage.progressProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() >= 1.0 && !placeholderImage.isError()) {
                    coverImage.setImage(placeholderImage);
                }
            });

            placeholderImage.errorProperty().addListener((obs, wasError, isError) -> {
                if (isError) {
                    showLocalFallback(gameTitle);
                }
            });

            if (placeholderImage.getProgress() >= 1.0 && !placeholderImage.isError()) {
                coverImage.setImage(placeholderImage);
            } else if (placeholderImage.isError()) {
                showLocalFallback(gameTitle);
            }
        } catch (Exception e) {
            showLocalFallback(gameTitle);
        }
    }

    private void showLocalFallback(String gameTitle) {
        setStyle("-fx-background-color: linear-gradient(to bottom, #4f46e5, #1f2937);");

        boolean hasLabel = getChildren().stream()
                .anyMatch(n -> n instanceof Label && "placeholder-title".equals(n.getId()));

        if (!hasLabel) {
            Label fallbackLabel = new Label(gameTitle);
            fallbackLabel.setId("placeholder-title");
            fallbackLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; " +
                    "-fx-wrap-text: true; -fx-text-alignment: center; -fx-padding: 16;");
            fallbackLabel.setMaxWidth(CARD_WIDTH - 20);
            fallbackLabel.setWrapText(true);
            StackPane.setAlignment(fallbackLabel, javafx.geometry.Pos.CENTER);
            getChildren().add(1, fallbackLabel);
        }
    }

    private void setupInteractions() {
        // Hover effects - just scale, no overlay
        setOnMouseEntered(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(200), this);
            scale.setToX(1.03);
            scale.setToY(1.03);
            scale.play();
            getStyleClass().add("game-card-hover");
        });

        setOnMouseExited(e -> {
            ScaleTransition scale = new ScaleTransition(Duration.millis(200), this);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.play();
            getStyleClass().remove("game-card-hover");
        });

        // Card click handler
        setOnMouseClicked(e -> {
            if (onCardClick != null && e.getClickCount() == 1) {
                onCardClick.run();
            }
        });
    }

    /**
     * Creates a fallback layout if FXML loading fails.
     */
    private void createFallbackLayout() {
        setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        setMinSize(CARD_WIDTH, CARD_HEIGHT);
        setMaxSize(CARD_WIDTH, CARD_HEIGHT);
        getStyleClass().add("game-card");

        // Create cover image with darkening effect
        coverImage = new ImageView();
        coverImage.setFitWidth(CARD_WIDTH);
        coverImage.setFitHeight(CARD_HEIGHT);
        coverImage.setPreserveRatio(false);
        coverImage.setEffect(darkenEffect);
        getChildren().add(coverImage);

        // Create info section with title and icons
        javafx.scene.layout.VBox infoSection = new javafx.scene.layout.VBox(6);
        infoSection.setAlignment(javafx.geometry.Pos.BOTTOM_LEFT);
        infoSection.setPadding(new javafx.geometry.Insets(0, 10, 10, 10));
        infoSection.setPickOnBounds(false);

        // Title
        titleLabel = new Label(game.getTitle());
        titleLabel.getStyleClass().add("game-card-title");
        titleLabel.setMaxWidth(CARD_WIDTH - 20);
        titleLabel.setWrapText(true);

        // Bottom row with platform icon and checkmark
        HBox bottomRow = new HBox();
        bottomRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Platform icon
        platformIcon = new FontIcon();
        platformIcon.setIconSize(18);
        platformIcon.setIconColor(Color.WHITE);
        platformIcon.getStyleClass().add("platform-icon");

        switch (game.getPlatform()) {
            case STEAM -> platformIcon.setIconCode(FontAwesomeBrands.STEAM);
            case EPIC -> platformIcon.setIconCode(MaterialDesignG.GAMEPAD_SQUARE);
            default -> platformIcon.setIconCode(MaterialDesignF.FOLDER);
        }

        platformBadge = new HBox();
        platformBadge.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        platformBadge.getChildren().add(platformIcon);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        bottomRow.getChildren().addAll(platformBadge, spacer);

        // Status checkmark
        if (game.getStatus() == Game.Status.READY) {
            statusCheckmark = FontIcon.of(FontAwesomeSolid.CHECK_CIRCLE, 18);
            statusCheckmark.setIconColor(Color.web("#22c55e"));
            statusCheckmark.getStyleClass().add("status-checkmark");
            bottomRow.getChildren().add(statusCheckmark);
        }

        infoSection.getChildren().addAll(titleLabel, bottomRow);
        StackPane.setAlignment(infoSection, javafx.geometry.Pos.BOTTOM_LEFT);
        getChildren().add(infoSection);
    }

    // Public API methods
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

