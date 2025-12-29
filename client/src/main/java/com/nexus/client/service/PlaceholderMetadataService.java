package com.nexus.client.service;

import com.nexus.shared.model.Game;
import com.nexus.shared.model.Game.Platform;

import java.util.HashMap;
import java.util.Map;

/**
 * Placeholder implementation of MetadataService.
 * Uses Steam CDN for Steam games and predefined URLs for known games.
 * Replace with actual API implementation (IGDB, Steam, etc.) later.
 */
public class PlaceholderMetadataService implements MetadataService {

    // Pre-defined metadata for known games (key is lowercase)
    private static final Map<String, GameMetadata> KNOWN_GAMES = new HashMap<>();

    static {
        // Popular Steam games with direct Steam CDN URLs
        addKnownGame("hollow knight", "367520",
            "Forge your own path in Hollow Knight! An epic action adventure through a vast ruined kingdom.",
            "Team Cherry");

        addKnownGame("celeste", "504230",
            "Help Madeline survive her inner demons on her journey to the top of Celeste Mountain.",
            "Maddy Makes Games");

        addKnownGame("hades", "1145360",
            "Defy the god of the dead as you hack and slash out of the Underworld.",
            "Supergiant Games");

        addKnownGame("stardew valley", "413150",
            "You've inherited your grandfather's old farm plot in Stardew Valley.",
            "ConcernedApe");

        addKnownGame("terraria", "105600",
            "Dig, fight, explore, build! The world is your canvas.",
            "Re-Logic");

        addKnownGame("factorio", "427520",
            "Factorio is a game about building and maintaining factories.",
            "Wube Software");

        addKnownGame("valheim", "892970",
            "A brutal exploration and survival game for 1-10 players.",
            "Iron Gate AB");

        addKnownGame("elden ring", "1245620",
            "Rise, Tarnished, and be guided by grace to brandish the power of the Elden Ring.",
            "FromSoftware");

        addKnownGame("the witcher 3", "292030",
            "You are Geralt of Rivia, mercenary monster slayer.",
            "CD Projekt Red");

        addKnownGame("witcher 3", "292030",
            "You are Geralt of Rivia, mercenary monster slayer.",
            "CD Projekt Red");

        addKnownGame("cyberpunk 2077", "1091500",
            "An open-world action-adventure RPG set in Night City.",
            "CD Projekt Red");

        addKnownGame("baldur's gate 3", "1086940",
            "Gather your party and return to the Forgotten Realms.",
            "Larian Studios");

        addKnownGame("counter-strike", "730",
            "Counter-Strike for a new generation.",
            "Valve");

        addKnownGame("cs2", "730",
            "Counter-Strike 2.",
            "Valve");

        addKnownGame("dota 2", "570",
            "Every day, millions of players enter battle as one of over a hundred Dota heroes.",
            "Valve");

        addKnownGame("portal 2", "620",
            "The sequel to the acclaimed Portal.",
            "Valve");

        addKnownGame("half-life", "70",
            "The classic first-person shooter.",
            "Valve");

        addKnownGame("left 4 dead 2", "550",
            "Cooperative first-person shooter zombie apocalypse.",
            "Valve");

        addKnownGame("team fortress 2", "440",
            "Nine distinct classes provide a broad range of tactical abilities.",
            "Valve");

        addKnownGame("garry's mod", "4000",
            "The sandbox game that has no objectives.",
            "Facepunch Studios");

        addKnownGame("rust", "252490",
            "The only aim in Rust is to survive.",
            "Facepunch Studios");

        addKnownGame("ark", "346110",
            "Stranded on a mysterious prehistoric island.",
            "Studio Wildcard");

        addKnownGame("dead by daylight", "381210",
            "A multiplayer horror game.",
            "Behaviour Interactive");

        addKnownGame("among us", "945360",
            "An online multiplayer social deduction game.",
            "Innersloth");

        addKnownGame("phasmophobia", "739630",
            "Paranormal activity ghost hunting game.",
            "Kinetic Games");

        addKnownGame("lethal company", "1966720",
            "A co-op horror about scavenging at abandoned moons.",
            "Zeekerss");

        addKnownGame("palworld", "1623730",
            "Fight, farm, build, work alongside mysterious creatures.",
            "Pocketpair");

        addKnownGame("civilization", "289070",
            "Build an empire to stand the test of time.",
            "Firaxis Games");

        addKnownGame("cities: skylines", "255710",
            "A modern take on the classic city simulation.",
            "Colossal Order");

        addKnownGame("rimworld", "294100",
            "A sci-fi colony sim.",
            "Ludeon Studios");

        addKnownGame("satisfactory", "526870",
            "Build massive factories in a first-person open-world.",
            "Coffee Stain Studios");

        addKnownGame("deep rock galactic", "548430",
            "Co-op FPS featuring badass space dwarves.",
            "Ghost Ship Games");

        addKnownGame("monster hunter", "582010",
            "Hunt massive monsters in an ever-changing ecosystem.",
            "Capcom");

        addKnownGame("dark souls", "570940",
            "Challenge yourself with this action RPG masterpiece.",
            "FromSoftware");

        addKnownGame("sekiro", "814380",
            "Carve your own clever path to vengeance.",
            "FromSoftware");

        addKnownGame("subnautica", "264710",
            "Descend into the depths of an alien underwater world.",
            "Unknown Worlds");

        addKnownGame("no man's sky", "275850",
            "Explore an infinite universe.",
            "Hello Games");

        addKnownGame("cuphead", "268910",
            "A classic run and gun action game.",
            "Studio MDHR");

        addKnownGame("hollow knight: silksong", "1030300",
            "The sequel to Hollow Knight.",
            "Team Cherry");

        addKnownGame("risk of rain", "632360",
            "Escape a chaotic alien planet.",
            "Hopoo Games");

        addKnownGame("vampire survivors", "1794680",
            "Mow down thousands of night creatures.",
            "poncle");

        addKnownGame("cult of the lamb", "1313140",
            "Start your own cult in a land of false prophets.",
            "Massive Monster");

        addKnownGame("dave the diver", "1868140",
            "A casual adventure RPG about a sushi restaurant.",
            "MINTROCKET");

        // Non-Steam games with custom URLs
        addKnownGameWithUrls("fortnite",
            "https://cdn1.epicgames.com/offer/fn/23BR_C4S1_EGS_Launcher_Blade_1200x1600_1200x1600-75c3f3a45e4017b838feaa6815c22498",
            "https://cdn2.unrealengine.com/social-image-chapter4-s3-3840x2160-d35912cc25ad.jpg",
            "Fortnite is the completely free multiplayer game.",
            "Epic Games");

        addKnownGameWithUrls("rocket league",
            "https://cdn1.epicgames.com/offer/9773aa1aa54f4f7b80e44bef04986cea/EGS_RocketLeague_PsyonixLLC_S2_1200x1600-41d92aa97918c0259fdad37a10b067fc",
            "https://cdn2.unrealengine.com/egs-rocketleague-psyonix-s1-2560x1440-6c749ca74c4a.jpg",
            "Rocket League is soccer with rocket-powered cars!",
            "Psyonix");

        addKnownGameWithUrls("fall guys",
            "https://cdn1.epicgames.com/offer/50118b7f954e450f8823df1614b24e80/EGS_FallGuys_Mediatonic_S2_1200x1600-c33e6e1f769093bdbec50d5dcc30ac1f",
            "https://cdn2.unrealengine.com/egs-fallguys-mediatonic-s1-2560x1440-2c8e94e2eeda.jpg",
            "Fall Guys is a massively multiplayer party royale.",
            "Mediatonic");

        addKnownGameWithUrls("minecraft",
            "https://www.minecraft.net/content/dam/games/minecraft/key-art/Games_Subnav_702x508_Box_Art-Latest.jpg",
            "https://www.minecraft.net/content/dam/games/minecraft/key-art/MC-Vanilla-keyart-702x508.jpg",
            "Minecraft is a game about placing blocks and going on adventures.",
            "Mojang Studios");

        addKnownGameWithUrls("league of legends",
            "https://ddragon.leagueoflegends.com/cdn/img/champion/splash/Lux_0.jpg",
            "https://ddragon.leagueoflegends.com/cdn/img/champion/splash/Jinx_0.jpg",
            "League of Legends is a team-based strategy game.",
            "Riot Games");

        addKnownGameWithUrls("valorant",
            "https://images.contentstack.io/v3/assets/bltb6530b271fddd0b1/blt81c7c04fb6b09d23/Val_Background.jpg",
            "https://images.contentstack.io/v3/assets/bltb6530b271fddd0b1/blt81c7c04fb6b09d23/Val_Background.jpg",
            "VALORANT is a free-to-play tactical shooter.",
            "Riot Games");

        addKnownGameWithUrls("genshin impact",
            "https://upload-static.hoyoverse.com/mijia-stone/2023/09/15/97b51b13c9d3e3ef0cc2f73618c4.png",
            "https://upload-static.hoyoverse.com/mijia-stone/2023/09/15/97b51b13c9d3e3ef0cc2f73618c4.png",
            "An open-world action RPG.",
            "HoYoverse");
    }

    private static void addKnownGame(String key, String steamAppId, String desc, String dev) {
        String cover = "https://steamcdn-a.akamaihd.net/steam/apps/" + steamAppId + "/library_600x900_2x.jpg";
        String hero = "https://steamcdn-a.akamaihd.net/steam/apps/" + steamAppId + "/header.jpg";
        KNOWN_GAMES.put(key.toLowerCase(), new GameMetadata(cover, hero, desc, dev, steamAppId));
    }

    private static void addKnownGameWithUrls(String key, String cover, String hero, String desc, String dev) {
        KNOWN_GAMES.put(key.toLowerCase(), new GameMetadata(cover, hero, desc, dev, null));
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
            // Apply known metadata
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
            // For Steam games, use Steam CDN with appId
            if (game.getPlatform() == Platform.STEAM && game.getAppId() != null && !game.getAppId().isEmpty()) {
                if (game.getCoverImageUrl() == null || game.getCoverImageUrl().isEmpty()) {
                    // Use the new Steam library assets CDN
                    game.setCoverImageUrl("https://steamcdn-a.akamaihd.net/steam/apps/" + game.getAppId() + "/library_600x900_2x.jpg");
                }
                if (game.getHeroImageUrl() == null || game.getHeroImageUrl().isEmpty()) {
                    game.setHeroImageUrl("https://steamcdn-a.akamaihd.net/steam/apps/" + game.getAppId() + "/header.jpg");
                }
            }

            // Set default text if still empty
            if (game.getDescription() == null || game.getDescription().isEmpty()) {
                game.setDescription("No description available for " + game.getTitle() + ".");
            }
            if (game.getDeveloper() == null || game.getDeveloper().isEmpty()) {
                game.setDeveloper("Unknown Developer");
            }
        }
    }

    @Override
    public String fetchCoverUrl(Game game) {
        if (game == null) return null;

        String title = game.getTitle().toLowerCase();
        for (Map.Entry<String, GameMetadata> entry : KNOWN_GAMES.entrySet()) {
            if (title.contains(entry.getKey()) || entry.getKey().contains(title)) {
                return entry.getValue().coverUrl;
            }
        }

        // For Steam games, construct URL from appId
        if (game.getPlatform() == Platform.STEAM && game.getAppId() != null) {
            return "https://steamcdn-a.akamaihd.net/steam/apps/" + game.getAppId() + "/library_600x900_2x.jpg";
        }

        return null;
    }

    @Override
    public String fetchDescription(Game game) {
        if (game == null) return null;

        String title = game.getTitle().toLowerCase();
        for (Map.Entry<String, GameMetadata> entry : KNOWN_GAMES.entrySet()) {
            if (title.contains(entry.getKey()) || entry.getKey().contains(title)) {
                return entry.getValue().description;
            }
        }
        return "No description available for " + game.getTitle() + ".";
    }

    @Override
    public String fetchHeroImageUrl(Game game) {
        if (game == null) return null;

        String title = game.getTitle().toLowerCase();
        for (Map.Entry<String, GameMetadata> entry : KNOWN_GAMES.entrySet()) {
            if (title.contains(entry.getKey()) || entry.getKey().contains(title)) {
                return entry.getValue().heroUrl;
            }
        }

        // For Steam games, construct URL from appId
        if (game.getPlatform() == Platform.STEAM && game.getAppId() != null) {
            return "https://steamcdn-a.akamaihd.net/steam/apps/" + game.getAppId() + "/header.jpg";
        }

        return null;
    }

    /**
     * Internal class to hold game metadata.
     */
    private static class GameMetadata {
        final String coverUrl;
        final String heroUrl;
        final String description;
        final String developer;
        final String steamAppId;

        GameMetadata(String coverUrl, String heroUrl, String description, String developer, String steamAppId) {
            this.coverUrl = coverUrl;
            this.heroUrl = heroUrl;
            this.description = description;
            this.developer = developer;
            this.steamAppId = steamAppId;
        }
    }
}

