package com.nexus.client.service;

import com.nexus.shared.model.Game;

/**
 * Interface for fetching game metadata (cover images, descriptions, etc.)
 * Implement this with different providers (IGDB, Steam API, etc.)
 */
public interface MetadataService {

    /**
     * Applies metadata to a game (cover image, description, developer, etc.)
     *
     * @param game The game to enrich with metadata
     */
    void applyMetadata(Game game);

    /**
     * Fetches a cover image URL for a game.
     *
     * @param game The game to fetch cover for
     * @return URL to the cover image, or null if not found
     */
    String fetchCoverUrl(Game game);

    /**
     * Fetches a description for a game.
     *
     * @param game The game to fetch description for
     * @return Description text, or null if not found
     */
    String fetchDescription(Game game);

    /**
     * Fetches a hero/banner image URL for a game.
     *
     * @param game The game to fetch hero image for
     * @return URL to the hero image, or null if not found
     */
    String fetchHeroImageUrl(Game game);
}

