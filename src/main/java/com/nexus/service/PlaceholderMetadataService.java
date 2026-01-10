package com.nexus.service;

import com.nexus.model.Game;
import com.nexus.model.Game.Platform;

/**
 * Fallback metadata service that provides default values when
 * Steam/IGDB APIs are unavailable.
 *
 * For Steam games with appId, generates Steam CDN URLs.
 * For other games, sets placeholder defaults.
 *
 * Note: PlaceholderImageUtil is used by UI components to generate
 * placeholder images via placehold.co API when cover URLs are missing.
 */
public class PlaceholderMetadataService implements MetadataService {

    @Override
    public void applyMetadata(Game game) {
        if (game == null || game.getTitle() == null) return;

        // For Steam games with appId, use Steam CDN URLs
        if (game.getPlatform() == Platform.STEAM && game.getAppId() != null && !game.getAppId().isEmpty()) {
            if (game.getCoverImageUrl() == null || game.getCoverImageUrl().isEmpty()) {
                game.setCoverImageUrl("https://steamcdn-a.akamaihd.net/steam/apps/" + game.getAppId() + "/library_600x900_2x.jpg");
            }
            if (game.getHeroImageUrl() == null || game.getHeroImageUrl().isEmpty()) {
                game.setHeroImageUrl("https://steamcdn-a.akamaihd.net/steam/apps/" + game.getAppId() + "/library_hero.jpg");
            }
        }

        // Set default values for missing metadata
        if (game.getDescription() == null || game.getDescription().isEmpty()) {
            game.setDescription("No description available.");
        }
        if (game.getDeveloper() == null || game.getDeveloper().isEmpty()) {
            game.setDeveloper("Unknown Developer");
        }
    }

    @Override
    public String fetchCoverUrl(Game game) {
        if (game == null) return null;
        applyMetadata(game);
        return game.getCoverImageUrl();
    }

    @Override
    public String fetchDescription(Game game) {
        if (game == null) return null;
        applyMetadata(game);
        return game.getDescription();
    }

    @Override
    public String fetchHeroImageUrl(Game game) {
        if (game == null) return null;
        applyMetadata(game);
        return game.getHeroImageUrl();
    }
}

