package com.nexus.client.service;

import com.nexus.shared.model.Game;
import com.nexus.shared.model.Game.Platform;
import com.nexus.shared.model.Game.Status;
import com.nexus.shared.repository.GameRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mock data service for providing sample game data.
 * This will be replaced with actual database/API calls in production.
 */
public class MockDataService {

    private static MockDataService instance;
    private final List<Game> games;
    private final GameRepository gameRepository;
    private Long nextId = 1L;

    private MockDataService() {
        games = new ArrayList<>();
        gameRepository = new GameRepository();
        initializeMockData();
    }

    public static MockDataService getInstance() {
        if (instance == null) {
            instance = new MockDataService();
        }
        return instance;
    }

    private void initializeMockData() {
        // Hollow Knight
        Game hollowKnight = new Game(nextId++, "Hollow Knight",
            "https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/367520/library_600x900.jpg",
            Platform.STEAM, Status.READY);
        hollowKnight.setDescription("Forge your own path in Hollow Knight! An epic action adventure through a vast ruined kingdom of insects and heroes. Explore twisting caverns, battle tainted creatures and befriend bizarre bugs.");
        hollowKnight.setDeveloper("Team Cherry");
        hollowKnight.setReleaseDate("February 24, 2017");
        hollowKnight.setHeroImageUrl("https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/367520/header.jpg");
        hollowKnight.setExecutablePath("C:\\Program Files\\Steam\\steamapps\\common\\Hollow Knight\\hollow_knight.exe");
        games.add(hollowKnight);

        // Fortnite
        Game fortnite = new Game(nextId++, "Fortnite",
            "https://images.igdb.com/igdb/image/upload/t_cover_big/coaxt6.webp",
            Platform.EPIC, Status.READY);
        fortnite.setDescription("Fortnite is the completely free online game where you and your friends fight to be the last one standing in Battle Royale, join forces to make your own Creative games, or catch a live show at Party Royale.");
        fortnite.setDeveloper("Epic Games");
        fortnite.setReleaseDate("July 25, 2017");
        fortnite.setHeroImageUrl("https://cdn2.unrealengine.com/social-image-chapter4-s3-3840x2160-d35912cc25ad.jpg");
        fortnite.setExecutablePath("C:\\Program Files\\Epic Games\\Fortnite\\FortniteGame\\Binaries\\Win64\\FortniteLauncher.exe");
        games.add(fortnite);

        // Minecraft
        Game minecraft = new Game(nextId++, "Minecraft",
            "https://images.igdb.com/igdb/image/upload/t_720p/ar4qpp.webp",
            Platform.MANUAL, Status.MISSING);
        minecraft.setDescription("Minecraft is a game about placing blocks and going on adventures. Explore randomly generated worlds and build amazing things from the simplest of homes to the grandest of castles.");
        minecraft.setDeveloper("Mojang Studios");
        minecraft.setReleaseDate("November 18, 2011");
        minecraft.setHeroImageUrl("https://www.minecraft.net/content/dam/games/minecraft/key-art/MC-Vanilla-keyart-702x508.jpg");
        games.add(minecraft);

        // Factorio
        Game factorio = new Game(nextId++, "Factorio",
            "https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/427520/library_600x900.jpg",
            Platform.STEAM, Status.READY);
        factorio.setDescription("Factorio is a game in which you build and maintain factories. You will be mining resources, researching technologies, building infrastructure, automating production and fighting enemies.");
        factorio.setDeveloper("Wube Software");
        factorio.setReleaseDate("August 14, 2020");
        factorio.setHeroImageUrl("https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/427520/header.jpg");
        factorio.setExecutablePath("C:\\Program Files\\Steam\\steamapps\\common\\Factorio\\bin\\x64\\factorio.exe");
        games.add(factorio);

        // Valheim
        Game valheim = new Game(nextId++, "Valheim",
            "https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/892970/library_600x900.jpg",
            Platform.STEAM, Status.READY);
        valheim.setDescription("A brutal exploration and survival game for 1-10 players, set in a procedurally-generated purgatory inspired by viking culture. Battle, build, and conquer your way to a saga worthy of Odin's blessing!");
        valheim.setDeveloper("Iron Gate AB");
        valheim.setReleaseDate("February 2, 2021");
        valheim.setHeroImageUrl("https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/892970/header.jpg");
        valheim.setExecutablePath("C:\\Program Files\\Steam\\steamapps\\common\\Valheim\\valheim.exe");
        valheim.setFavorite(true);
        games.add(valheim);

        // Celeste
        Game celeste = new Game(nextId++, "Celeste",
            "https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/504230/library_600x900.jpg",
            Platform.STEAM, Status.READY);
        celeste.setDescription("Help Madeline survive her inner demons on her journey to the top of Celeste Mountain. Brave hundreds of hand-crafted challenges, uncover devious secrets, and piece together the mystery of the mountain.");
        celeste.setDeveloper("Maddy Makes Games");
        celeste.setReleaseDate("January 25, 2018");
        celeste.setHeroImageUrl("https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/504230/header.jpg");
        celeste.setExecutablePath("C:\\Program Files\\Steam\\steamapps\\common\\Celeste\\Celeste.exe");
        celeste.setFavorite(true);
        games.add(celeste);

        // Hades
        Game hades = new Game(nextId++, "Hades",
            "https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/1145360/library_600x900.jpg",
            Platform.STEAM, Status.READY);
        hades.setDescription("Defy the god of the dead as you hack and slash out of the Underworld in this rogue-like dungeon crawler from the creators of Bastion, Transistor, and Pyre.");
        hades.setDeveloper("Supergiant Games");
        hades.setReleaseDate("September 17, 2020");
        hades.setHeroImageUrl("https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/1145360/header.jpg");
        hades.setExecutablePath("C:\\Program Files\\Steam\\steamapps\\common\\Hades\\x64\\Hades.exe");
        games.add(hades);

        // The Witcher 3
        Game witcher3 = new Game(nextId++, "The Witcher 3: Wild Hunt",
            "https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/292030/library_600x900.jpg",
            Platform.STEAM, Status.READY);
        witcher3.setDescription("You are Geralt of Rivia, mercenary monster slayer. Before you stands a war-torn, monster-infested continent you can explore at will. Your current contract? Tracking down Ciri — the Child of Prophecy.");
        witcher3.setDeveloper("CD Projekt Red");
        witcher3.setReleaseDate("May 18, 2015");
        witcher3.setHeroImageUrl("https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/292030/header.jpg");
        witcher3.setExecutablePath("C:\\Program Files\\Steam\\steamapps\\common\\The Witcher 3\\bin\\x64\\witcher3.exe");
        witcher3.setFavorite(true);
        games.add(witcher3);

        // Elden Ring
        Game eldenRing = new Game(nextId++, "Elden Ring",
            "https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/1245620/library_600x900.jpg",
            Platform.STEAM, Status.READY);
        eldenRing.setDescription("THE NEW FANTASY ACTION RPG. Rise, Tarnished, and be guided by grace to brandish the power of the Elden Ring and become an Elden Lord in the Lands Between.");
        eldenRing.setDeveloper("FromSoftware");
        eldenRing.setReleaseDate("February 25, 2022");
        eldenRing.setHeroImageUrl("https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/1245620/header.jpg");
        eldenRing.setExecutablePath("C:\\Program Files\\Steam\\steamapps\\common\\ELDEN RING\\Game\\eldenring.exe");
        games.add(eldenRing);

        // Stardew Valley
        Game stardew = new Game(nextId++, "Stardew Valley",
            "https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/413150/library_600x900.jpg",
            Platform.STEAM, Status.READY);
        stardew.setDescription("You've inherited your grandfather's old farm plot in Stardew Valley. Armed with hand-me-down tools and a few coins, you set out to begin your new life.");
        stardew.setDeveloper("ConcernedApe");
        stardew.setReleaseDate("February 26, 2016");
        stardew.setHeroImageUrl("https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/413150/header.jpg");
        stardew.setExecutablePath("C:\\Program Files\\Steam\\steamapps\\common\\Stardew Valley\\Stardew Valley.exe");
        games.add(stardew);

        // Rocket League (Epic)
        Game rocketLeague = new Game(nextId++, "Rocket League",
            "https://images.igdb.com/igdb/image/upload/t_cover_big/co5w0w.webp",
            Platform.EPIC, Status.READY);
        rocketLeague.setDescription("Rocket League is a high-powered hybrid of arcade soccer and vehicular mayhem! Customize your car, hit the field, and compete in one of the most critically acclaimed sports games of all time!");
        rocketLeague.setDeveloper("Psyonix");
        rocketLeague.setReleaseDate("July 7, 2015");
        rocketLeague.setHeroImageUrl("https://cdn2.unrealengine.com/egs-rocketleague-psyonix-s1-2560x1440-6c749ca74c4a.jpg");
        rocketLeague.setExecutablePath("C:\\Program Files\\Epic Games\\RocketLeague\\Binaries\\Win64\\RocketLeague.exe");
        games.add(rocketLeague);

        // Fall Guys (Epic)
        Game fallGuys = new Game(nextId++, "Fall Guys",
            "https://images.igdb.com/igdb/image/upload/t_cover_big/co4jni.webp",
            Platform.EPIC, Status.READY);
        fallGuys.setDescription("Fall Guys is a free, cross-platform massively multiplayer party royale game. Compete in a series of escalating rounds, overcome absurd obstacles, and stumble towards the crown!");
        fallGuys.setDeveloper("Mediatonic");
        fallGuys.setReleaseDate("August 4, 2020");
        fallGuys.setHeroImageUrl("https://cdn2.unrealengine.com/egs-fallguys-mediatonic-s1-2560x1440-2c8e94e2eeda.jpg");
        fallGuys.setExecutablePath("C:\\Program Files\\Epic Games\\FallGuys\\FallGuys_client.exe");
        games.add(fallGuys);
    }

    public List<Game> getAllGames() {
        return new ArrayList<>(games);
    }

    public List<Game> getFavoriteGames() {
        return games.stream()
            .filter(Game::isFavorite)
            .collect(Collectors.toList());
    }

    public List<Game> searchGames(String query) {
        String lowerQuery = query.toLowerCase();
        return games.stream()
            .filter(game -> game.getTitle().toLowerCase().contains(lowerQuery))
            .collect(Collectors.toList());
    }

    public Game getGameById(Long id) {
        return games.stream()
            .filter(game -> game.getId().equals(id))
            .findFirst()
            .orElse(null);
    }

    public void addGame(Game game) {
        game.setId(nextId++);
        games.add(game);
    }

    public void toggleFavorite(Long gameId) {
        Game game = getGameById(gameId);
        if (game != null) {
            game.setFavorite(!game.isFavorite());
        }
    }

    public int getGameCount() {
        return games.size();
    }

    public GameRepository getGameRepository() {
        return gameRepository;
    }
}
