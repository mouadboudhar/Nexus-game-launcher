package com.nexus.client.service;

import com.nexus.model.Game;
import com.nexus.model.Game.Platform;

import java.util.HashMap;
import java.util.Map;

/**
 * Fallback metadata service with minimal hardcoded data.
 * Used when Steam/IGDB APIs are unavailable.
 *
 * Primary metadata is fetched dynamically by CombinedMetadataService.
 */
public class PlaceholderMetadataService implements MetadataService {

    private static final Map<String, GameMetadata> KNOWN_GAMES = new HashMap<>();

    static {
        // Only essential non-Steam games that need custom URLs

        // Riot Games
        addKnownGameWithUrls("league of legends",
            "https://cmsassets.rgpub.io/sanity/images/dsfx7636/news/7d30fd4bf86c9412ba5a7c38685aad14e7ec3cfa-1920x1080.jpg",
            "https://cmsassets.rgpub.io/sanity/images/dsfx7636/news/7d30fd4bf86c9412ba5a7c38685aad14e7ec3cfa-1920x1080.jpg",
            "League of Legends is a team-based strategy game.",
            "Riot Games");

        addKnownGameWithUrls("valorant",
            "https://cmsassets.rgpub.io/sanity/images/dsfx7636/news/4ad45e22ccbb5b9b29ec5c5a66055e3a3eb6c9fb-1920x1080.jpg",
            "https://cmsassets.rgpub.io/sanity/images/dsfx7636/news/4ad45e22ccbb5b9b29ec5c5a66055e3a3eb6c9fb-1920x1080.jpg",
            "VALORANT is a free-to-play 5v5 tactical shooter.",
            "Riot Games");

        // Epic exclusives
        addKnownGameWithUrls("fortnite",
            "https://cdn1.epicgames.com/offer/fn/23BR_C4S1_EGS_Launcher_Blade_1200x1600_1200x1600-75c3f3a45e4017b838feaa6815c22498",
            "https://cdn2.unrealengine.com/social-image-chapter4-s3-3840x2160-d35912cc25ad.jpg",
            "Fortnite is the completely free multiplayer game.",
            "Epic Games");

        // Minecraft (not on Steam)
        addKnownGameWithUrls("minecraft",
            "https://www.minecraft.net/content/dam/games/minecraft/key-art/Games_Subnav_702x508_Box_Art-Latest.jpg",
            "https://www.minecraft.net/content/dam/games/minecraft/key-art/MC-Vanilla-keyart-702x508.jpg",
            "Minecraft is a game about placing blocks and going on adventures.",
            "Mojang Studios");

        // HoYoverse games
        addKnownGameWithUrls("genshin impact",
            "https://webstatic.hoyoverse.com/upload/event/2020/11/06/98b6ed6b98ad5e7b5fbf26b0c4eb3bb0_1082432428905710452.jpg",
            "https://fastcdn.hoyoverse.com/content-v2/plat/114197/c3773f9f285b99c56b9f7e3e93cb79b0_3824539933557904091.jpg",
            "Step into Teyvat, a vast world teeming with life and flowing with elemental energy.",
            "HoYoverse");

        addKnownGameWithUrls("honkai: star rail",
            "https://webstatic.hoyoverse.com/upload/op-public/2023/04/13/0e03baad6628f5b5f1f5c93e764b19c6_8082814547481384145.jpg",
            "https://fastcdn.hoyoverse.com/content-v2/hkrpg/114273/21f7bf63c6f99a5fb0c9c5cd9ce7bb5e_2322066946206497986.jpg",
            "Honkai: Star Rail is a space fantasy RPG.",
            "HoYoverse");
    }

    private static void addKnownGameWithUrls(String key, String cover, String hero, String desc, String dev) {
        KNOWN_GAMES.put(key.toLowerCase(), new GameMetadata(cover, hero, desc, dev));
    }

    @Override
    public void applyMetadata(Game game) {
        if (game == null || game.getTitle() == null) return;

        String title = game.getTitle().toLowerCase().trim();

        // Try exact match first
        GameMetadata metadata = KNOWN_GAMES.get(title);

        // Try partial match if no exact match
        if (metadata == null) {
            for (Map.Entry<String, GameMetadata> entry : KNOWN_GAMES.entrySet()) {
                if (title.contains(entry.getKey()) || entry.getKey().contains(title)) {
                    metadata = entry.getValue();
                    break;
                }
            }
        }

        if (metadata != null) {
            if (game.getCoverImageUrl() == null || game.getCoverImageUrl().isEmpty()) {
                game.setCoverImageUrl(metadata.coverUrl);
            }
            if (game.getHeroImageUrl() == null || game.getHeroImageUrl().isEmpty()) {
                game.setHeroImageUrl(metadata.heroUrl);
            }
            if (game.getDescription() == null || game.getDescription().isEmpty()) {
                game.setDescription(metadata.description);
            }
            if (game.getDeveloper() == null || game.getDeveloper().isEmpty()) {
                game.setDeveloper(metadata.developer);
            }
        } else {
            // For Steam games without known metadata, use Steam CDN
            if (game.getPlatform() == Platform.STEAM && game.getAppId() != null && !game.getAppId().isEmpty()) {
                if (game.getCoverImageUrl() == null || game.getCoverImageUrl().isEmpty()) {
                    game.setCoverImageUrl("https://steamcdn-a.akamaihd.net/steam/apps/" + game.getAppId() + "/library_600x900_2x.jpg");
                }
                if (game.getHeroImageUrl() == null || game.getHeroImageUrl().isEmpty()) {
                    game.setHeroImageUrl("https://steamcdn-a.akamaihd.net/steam/apps/" + game.getAppId() + "/library_hero.jpg");
                }
            }

            // Set defaults
            if (game.getDescription() == null || game.getDescription().isEmpty()) {
                game.setDescription("No description available.");
            }
            if (game.getDeveloper() == null || game.getDeveloper().isEmpty()) {
                game.setDeveloper("Unknown Developer");
            }
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

    private static class GameMetadata {
        final String coverUrl;
        final String heroUrl;
        final String description;
        final String developer;

        GameMetadata(String coverUrl, String heroUrl, String description, String developer) {
            this.coverUrl = coverUrl;
            this.heroUrl = heroUrl;
            this.description = description;
            this.developer = developer;
        }
    }
}

