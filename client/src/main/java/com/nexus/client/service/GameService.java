package com.nexus.client.service;

import com.nexus.shared.model.Game;
import com.nexus.shared.repository.GameRepository;

import java.util.*;

/**
 * Service layer for game data operations.
 * Provides a clean API for controllers to interact with game data.
 */
public class GameService {

    private static GameService instance;
    private final GameRepository gameRepository;
    private final MetadataService metadataService;

    private GameService() {
        this.gameRepository = new GameRepository();
        this.metadataService = new PlaceholderMetadataService();
    }

    public static synchronized GameService getInstance() {
        if (instance == null) {
            instance = new GameService();
        }
        return instance;
    }

    /**
     * Gets all games from the database, deduplicated and with metadata.
     */
    public List<Game> getAllGames() {
        List<Game> games = gameRepository.findAll();

        // Deduplicate by uniqueId (keep first occurrence)
        Map<String, Game> uniqueGames = new LinkedHashMap<>();
        List<Long> duplicateIds = new ArrayList<>();

        for (Game game : games) {
            String key = game.getUniqueId();
            if (key == null || key.isEmpty()) {
                // Use title + platform as fallback key
                key = (game.getTitle() + "_" + game.getPlatform()).toLowerCase();
            }

            if (!uniqueGames.containsKey(key)) {
                uniqueGames.put(key, game);
                ensureMetadata(game);
            } else {
                // This is a duplicate - mark for deletion
                duplicateIds.add(game.getId());
            }
        }

        // Delete duplicates from database
        for (Long id : duplicateIds) {
            try {
                gameRepository.delete(id);
                System.out.println("[GameService] Removed duplicate game with id: " + id);
            } catch (Exception e) {
                // Ignore deletion errors
            }
        }

        return new ArrayList<>(uniqueGames.values());
    }

    /**
     * Gets favorite games.
     */
    public List<Game> getFavoriteGames() {
        List<Game> games = gameRepository.findByFavorite(true);
        for (Game game : games) {
            ensureMetadata(game);
        }
        return games;
    }

    /**
     * Searches games by title.
     */
    public List<Game> searchGames(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllGames();
        }
        List<Game> games = gameRepository.searchByTitle(query.trim());
        for (Game game : games) {
            ensureMetadata(game);
        }
        return games;
    }

    /**
     * Gets a game by ID.
     */
    public Optional<Game> getGameById(Long id) {
        Optional<Game> game = gameRepository.findById(id);
        game.ifPresent(this::ensureMetadata);
        return game;
    }

    /**
     * Gets a game by unique ID.
     */
    public Optional<Game> getGameByUniqueId(String uniqueId) {
        Optional<Game> game = gameRepository.findByUniqueId(uniqueId);
        game.ifPresent(this::ensureMetadata);
        return game;
    }

    /**
     * Saves or updates a game.
     */
    public Game saveGame(Game game) {
        return gameRepository.save(game);
    }

    /**
     * Toggles the favorite status of a game.
     */
    public void toggleFavorite(Game game) {
        game.setFavorite(!game.isFavorite());
        gameRepository.save(game);
    }

    /**
     * Deletes a game.
     */
    public void deleteGame(Game game) {
        if (game != null && game.getId() != null) {
            gameRepository.delete(game.getId());
        }
    }

    /**
     * Gets the total count of games.
     */
    public long getGameCount() {
        return gameRepository.count();
    }

    /**
     * Gets games by platform.
     */
    public List<Game> getGamesByPlatform(Game.Platform platform) {
        List<Game> games = gameRepository.findByPlatform(platform);
        for (Game game : games) {
            ensureMetadata(game);
        }
        return games;
    }

    /**
     * Ensures a game has metadata (cover, description, etc.).
     * Only applies if current values are empty/null.
     */
    private void ensureMetadata(Game game) {
        if (game == null) return;

        boolean needsUpdate = false;

        // Check if cover URL is missing or invalid
        String coverUrl = game.getCoverImageUrl();
        if (coverUrl == null || coverUrl.isEmpty() || coverUrl.startsWith("/assets/")) {
            metadataService.applyMetadata(game);
            needsUpdate = true;
        }

        // Check if description is missing
        if (game.getDescription() == null || game.getDescription().isEmpty() ||
            game.getDescription().startsWith("No description")) {
            metadataService.applyMetadata(game);
            needsUpdate = true;
        }

        // Save updated metadata
        if (needsUpdate && game.getId() != null) {
            try {
                gameRepository.save(game);
            } catch (Exception e) {
                // Ignore save errors for metadata updates
            }
        }
    }

    /**
     * Clears all games from the database (useful for rescan).
     */
    public void clearAllGames() {
        List<Game> games = gameRepository.findAll();
        for (Game game : games) {
            if (game.getPlatform() != Game.Platform.MANUAL) {
                gameRepository.delete(game.getId());
            }
        }
    }
}

