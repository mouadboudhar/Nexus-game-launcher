package com.nexus.service;

import com.nexus.model.Game;
import com.nexus.model.Game.Platform;

import java.util.HashMap;
import java.util.Map;

/**
 * Fallback metadata service with hardcoded data for popular games.
 */
public class PlaceholderMetadataService implements MetadataService {

    private static final Map<String, GameMeta> KNOWN_GAMES = new HashMap<>();

    static {
        // Riot Games
        KNOWN_GAMES.put("league of legends", new GameMeta(
            "https://images.igdb.com/igdb/image/upload/t_cover_big/co1rct.jpg",
            "https://images.igdb.com/igdb/image/upload/t_1080p/co1rct.jpg",
            "League of Legends is a team-based strategy game where two teams of five powerful champions face off to destroy the other's base.",
            "Riot Games"
        ));
        KNOWN_GAMES.put("valorant", new GameMeta(
            "https://images.igdb.com/igdb/image/upload/t_cover_big/co2mvt.jpg",
            "https://images.igdb.com/igdb/image/upload/t_1080p/co2mvt.jpg",
            "VALORANT is a 5v5 character-based tactical shooter where precise gunplay meets unique agent abilities.",
            "Riot Games"
        ));
        KNOWN_GAMES.put("legends of runeterra", new GameMeta(
            "https://images.igdb.com/igdb/image/upload/t_cover_big/co1ycb.jpg",
            "https://images.igdb.com/igdb/image/upload/t_1080p/co1ycb.jpg",
            "Legends of Runeterra is a digital collectible card game set in the League of Legends universe.",
            "Riot Games"
        ));

        // Blizzard / Battle.net
        KNOWN_GAMES.put("world of warcraft", new GameMeta(
            "https://images.igdb.com/igdb/image/upload/t_cover_big/co1rgg.jpg",
            "https://images.igdb.com/igdb/image/upload/t_1080p/co1rgg.jpg",
            "World of Warcraft is a massively multiplayer online role-playing game set in the Warcraft universe.",
            "Blizzard Entertainment"
        ));
        KNOWN_GAMES.put("diablo iv", new GameMeta(
            "https://images.igdb.com/igdb/image/upload/t_cover_big/co5w3k.jpg",
            "https://images.igdb.com/igdb/image/upload/t_1080p/co5w3k.jpg",
            "Diablo IV is the ultimate action RPG experience with endless evil to slaughter.",
            "Blizzard Entertainment"
        ));
        KNOWN_GAMES.put("overwatch 2", new GameMeta(
            "https://images.igdb.com/igdb/image/upload/t_cover_big/co5tku.jpg",
            "https://images.igdb.com/igdb/image/upload/t_1080p/co5tku.jpg",
            "Overwatch 2 is a free-to-play team-based action game set in an optimistic future.",
            "Blizzard Entertainment"
        ));
        KNOWN_GAMES.put("hearthstone", new GameMeta(
            "https://images.igdb.com/igdb/image/upload/t_cover_big/co1r76.jpg",
            "https://images.igdb.com/igdb/image/upload/t_1080p/co1r76.jpg",
            "Hearthstone is a free-to-play digital collectible card game from Blizzard Entertainment.",
            "Blizzard Entertainment"
        ));

        // Minecraft / Mojang
        KNOWN_GAMES.put("minecraft", new GameMeta(
            "https://images.igdb.com/igdb/image/upload/t_cover_big/co49x5.jpg",
            "https://images.igdb.com/igdb/image/upload/t_1080p/co49x5.jpg",
            "Minecraft is a game about placing blocks and going on adventures.",
            "Mojang Studios"
        ));

        // miHoYo / HoYoverse
        KNOWN_GAMES.put("genshin impact", new GameMeta(
            "https://images.igdb.com/igdb/image/upload/t_cover_big/co3p8f.jpg",
            "https://images.igdb.com/igdb/image/upload/t_1080p/co3p8f.jpg",
            "Genshin Impact is an open-world action RPG where you embark on a journey across Teyvat.",
            "miHoYo"
        ));
        KNOWN_GAMES.put("honkai: star rail", new GameMeta(
            "https://images.igdb.com/igdb/image/upload/t_cover_big/co5vrt.jpg",
            "https://images.igdb.com/igdb/image/upload/t_1080p/co5vrt.jpg",
            "Honkai: Star Rail is a space fantasy RPG from HoYoverse.",
            "miHoYo"
        ));

        // Other popular games
        KNOWN_GAMES.put("fortnite", new GameMeta(
            "https://images.igdb.com/igdb/image/upload/t_cover_big/co3wk8.jpg",
            "https://images.igdb.com/igdb/image/upload/t_1080p/co3wk8.jpg",
            "Fortnite is a free-to-play battle royale game with building mechanics.",
            "Epic Games"
        ));
        KNOWN_GAMES.put("roblox", new GameMeta(
            "https://images.igdb.com/igdb/image/upload/t_cover_big/co3obe.jpg",
            "https://images.igdb.com/igdb/image/upload/t_1080p/co3obe.jpg",
            "Roblox is an online platform where millions of people come together to create and share experiences.",
            "Roblox Corporation"
        ));
    }

    private static class GameMeta {
        final String coverUrl, heroUrl, description, developer;

        GameMeta(String coverUrl, String heroUrl, String description, String developer) {
            this.coverUrl = coverUrl;
            this.heroUrl = heroUrl;
            this.description = description;
            this.developer = developer;
        }
    }

    @Override
    public void applyMetadata(Game game) {
        if (game == null || game.getTitle() == null) return;

        // Steam games - use Steam CDN URLs
        if (game.getPlatform() == Platform.STEAM && game.getAppId() != null && !game.getAppId().isEmpty()) {
            if (game.getCoverImageUrl() == null || game.getCoverImageUrl().isEmpty()) {
                game.setCoverImageUrl("https://steamcdn-a.akamaihd.net/steam/apps/" + game.getAppId() + "/library_600x900_2x.jpg");
            }
            if (game.getHeroImageUrl() == null || game.getHeroImageUrl().isEmpty()) {
                game.setHeroImageUrl("https://steamcdn-a.akamaihd.net/steam/apps/" + game.getAppId() + "/library_hero.jpg");
            }
        }

        // Check known games
        String titleLower = game.getTitle().toLowerCase();
        GameMeta meta = KNOWN_GAMES.get(titleLower);

        if (meta != null) {
            if (game.getCoverImageUrl() == null || game.getCoverImageUrl().isEmpty()) {
                game.setCoverImageUrl(meta.coverUrl);
            }
            if (game.getHeroImageUrl() == null || game.getHeroImageUrl().isEmpty()) {
                game.setHeroImageUrl(meta.heroUrl);
            }
            if (game.getDescription() == null || game.getDescription().isEmpty()) {
                game.setDescription(meta.description);
            }
            if (game.getDeveloper() == null || game.getDeveloper().isEmpty()) {
                game.setDeveloper(meta.developer);
            }
            return;
        }

        // Default fallbacks
        if (game.getDescription() == null || game.getDescription().isEmpty()) {
            game.setDescription("No description available.");
        }
        if (game.getDeveloper() == null || game.getDeveloper().isEmpty()) {
            game.setDeveloper("Unknown Developer");
        }
    }

    @Override
    public String fetchCoverUrl(Game game) {
        applyMetadata(game);
        return game.getCoverImageUrl();
    }

    @Override
    public String fetchDescription(Game game) {
        applyMetadata(game);
        return game.getDescription();
    }

    @Override
    public String fetchHeroImageUrl(Game game) {
        applyMetadata(game);
        return game.getHeroImageUrl();
    }
}

