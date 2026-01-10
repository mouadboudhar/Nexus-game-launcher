package com.nexus.service;

import com.nexus.model.Game;
import com.nexus.model.Game.Platform;
import com.nexus.model.Game.Status;
import com.nexus.repository.GameRepository;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service responsible for scanning installed games from multiple sources:
 * - Steam (via registry + libraryfolders.vdf)
 * - Epic Games (via manifests)
 * - Riot Games (via RiotClientInstalls.json)
 * - Battle.net (via product.db)
 * - EA App / Origin (via local content)
 * - Known standalone games (whitelist-based)
 */
public class ScannerService {

    private final GameRepository gameRepository;
    private final MetadataService metadataService;

    // ==================== KNOWN STANDALONE GAMES WHITELIST ====================
    // Maps registry DisplayName patterns to canonical game names
    private static final Map<String, String> KNOWN_STANDALONE_GAMES = new LinkedHashMap<>();
    static {
        // Minecraft / Mojang
        KNOWN_STANDALONE_GAMES.put("minecraft launcher", "Minecraft");
        KNOWN_STANDALONE_GAMES.put("minecraft dungeons", "Minecraft Dungeons");
        KNOWN_STANDALONE_GAMES.put("minecraft legends", "Minecraft Legends");

        // Roblox
        KNOWN_STANDALONE_GAMES.put("roblox", "Roblox");

        // miHoYo / HoYoverse
        KNOWN_STANDALONE_GAMES.put("genshin impact", "Genshin Impact");
        KNOWN_STANDALONE_GAMES.put("honkai: star rail", "Honkai: Star Rail");
        KNOWN_STANDALONE_GAMES.put("honkai impact", "Honkai Impact 3rd");
        KNOWN_STANDALONE_GAMES.put("zenless zone zero", "Zenless Zone Zero");

        // Other popular standalone games
        KNOWN_STANDALONE_GAMES.put("osu!", "osu!");
        KNOWN_STANDALONE_GAMES.put("warframe", "Warframe");
        KNOWN_STANDALONE_GAMES.put("path of exile", "Path of Exile");
        KNOWN_STANDALONE_GAMES.put("escape from tarkov", "Escape From Tarkov");
        KNOWN_STANDALONE_GAMES.put("escapefromtarkov", "Escape From Tarkov");
        KNOWN_STANDALONE_GAMES.put("star citizen", "Star Citizen");
        KNOWN_STANDALONE_GAMES.put("guild wars 2", "Guild Wars 2");
        KNOWN_STANDALONE_GAMES.put("final fantasy xiv", "Final Fantasy XIV");
        KNOWN_STANDALONE_GAMES.put("ffxiv", "Final Fantasy XIV");
        KNOWN_STANDALONE_GAMES.put("phantasy star online 2", "Phantasy Star Online 2");
        KNOWN_STANDALONE_GAMES.put("pso2", "Phantasy Star Online 2");
        KNOWN_STANDALONE_GAMES.put("maplestory", "MapleStory");
        KNOWN_STANDALONE_GAMES.put("lost ark", "Lost Ark");
        KNOWN_STANDALONE_GAMES.put("black desert", "Black Desert Online");
        KNOWN_STANDALONE_GAMES.put("albion online", "Albion Online");
        KNOWN_STANDALONE_GAMES.put("runescape", "RuneScape");
        KNOWN_STANDALONE_GAMES.put("old school runescape", "Old School RuneScape");
        KNOWN_STANDALONE_GAMES.put("world of tanks", "World of Tanks");
        KNOWN_STANDALONE_GAMES.put("world of warships", "World of Warships");
        KNOWN_STANDALONE_GAMES.put("war thunder", "War Thunder");
        KNOWN_STANDALONE_GAMES.put("crossfire", "CrossFire");
        KNOWN_STANDALONE_GAMES.put("growtopia", "Growtopia");
        KNOWN_STANDALONE_GAMES.put("rec room", "Rec Room");
        KNOWN_STANDALONE_GAMES.put("vrchat", "VRChat");
        KNOWN_STANDALONE_GAMES.put("tower of fantasy", "Tower of Fantasy");
        KNOWN_STANDALONE_GAMES.put("wuthering waves", "Wuthering Waves");
        KNOWN_STANDALONE_GAMES.put("the finals", "THE FINALS");
        KNOWN_STANDALONE_GAMES.put("multiversus", "MultiVersus");
        KNOWN_STANDALONE_GAMES.put("brawlhalla", "Brawlhalla");
        KNOWN_STANDALONE_GAMES.put("trackmania", "Trackmania");
        KNOWN_STANDALONE_GAMES.put("enlisted", "Enlisted");
        KNOWN_STANDALONE_GAMES.put("smite", "SMITE");
        KNOWN_STANDALONE_GAMES.put("paladins", "Paladins");
        KNOWN_STANDALONE_GAMES.put("realm royale", "Realm Royale");
        KNOWN_STANDALONE_GAMES.put("dauntless", "Dauntless");
        KNOWN_STANDALONE_GAMES.put("neverwinter", "Neverwinter");
        KNOWN_STANDALONE_GAMES.put("tera", "TERA");
        KNOWN_STANDALONE_GAMES.put("blade & soul", "Blade & Soul");
        KNOWN_STANDALONE_GAMES.put("aion", "Aion");
        KNOWN_STANDALONE_GAMES.put("lineage", "Lineage");
        KNOWN_STANDALONE_GAMES.put("elden ring", "Elden Ring");
        KNOWN_STANDALONE_GAMES.put("armored core vi", "Armored Core VI");
        KNOWN_STANDALONE_GAMES.put("dark souls", "Dark Souls");
        KNOWN_STANDALONE_GAMES.put("sekiro", "Sekiro: Shadows Die Twice");
        KNOWN_STANDALONE_GAMES.put("cyberpunk 2077", "Cyberpunk 2077");
        KNOWN_STANDALONE_GAMES.put("the witcher 3", "The Witcher 3");
        KNOWN_STANDALONE_GAMES.put("hogwarts legacy", "Hogwarts Legacy");
        KNOWN_STANDALONE_GAMES.put("baldur's gate 3", "Baldur's Gate 3");
        KNOWN_STANDALONE_GAMES.put("lethal company", "Lethal Company");
        KNOWN_STANDALONE_GAMES.put("palworld", "Palworld");
        KNOWN_STANDALONE_GAMES.put("satisfactory", "Satisfactory");
        KNOWN_STANDALONE_GAMES.put("factorio", "Factorio");
    }

    public ScannerService() {
        this.gameRepository = new GameRepository();
        this.metadataService = new CombinedMetadataService();
    }

    public ScannerService(GameRepository gameRepository, MetadataService metadataService) {
        this.gameRepository = gameRepository;
        this.metadataService = metadataService;
    }

    public List<Game> fullRescan() {
        System.out.println("[ScannerService] Performing full rescan...");
        try {
            gameRepository.deleteAll();
        } catch (Exception e) {
            System.err.println("[ScannerService] Error clearing database: " + e.getMessage());
        }
        return scanAll();
    }

    public List<Game> scanAll() {
        System.out.println("[ScannerService] Starting full scan...");

        Map<String, Game> gameByUniqueId = new LinkedHashMap<>();
        Set<String> seenNormalizedTitles = new HashSet<>();

        // 1. Scan Steam
        addGames(gameByUniqueId, seenNormalizedTitles, scanSteam(), "Steam");

        // 2. Scan Epic Games
        addGames(gameByUniqueId, seenNormalizedTitles, scanEpic(), "Epic");

        // 3. Scan Riot Games (League, VALORANT, etc.)
        addGames(gameByUniqueId, seenNormalizedTitles, scanRiotGames(), "Riot");

        // 4. Scan Battle.net (WoW, Diablo, Overwatch, etc.)
        addGames(gameByUniqueId, seenNormalizedTitles, scanBattleNet(), "Battle.net");

        // 5. Scan EA App / Origin
        addGames(gameByUniqueId, seenNormalizedTitles, scanEAApp(), "EA");

        // 6. Scan known standalone games from registry
        addGames(gameByUniqueId, seenNormalizedTitles, scanKnownStandalones(), "Standalone");

        List<Game> allGames = new ArrayList<>(gameByUniqueId.values());
        List<Game> mergedGames = mergeWithDatabase(allGames);
        System.out.println("[ScannerService] Scan complete. Total games: " + mergedGames.size());

        return mergedGames;
    }

    private void addGames(Map<String, Game> gameMap, Set<String> seenTitles, List<Game> games, String source) {
        System.out.println("[ScannerService] Found " + games.size() + " " + source + " games");
        for (Game g : games) {
            String normalizedTitle = normalizeTitle(g.getTitle());
            if (!seenTitles.contains(normalizedTitle) && g.getUniqueId() != null) {
                seenTitles.add(normalizedTitle);
                gameMap.put(g.getUniqueId(), g);
            }
        }
    }

    private String normalizeTitle(String title) {
        if (title == null) return "";
        return title.toLowerCase().replaceAll("[^a-z0-9]", "").trim();
    }

    // ==================== STEAM SCANNER ====================

    public List<Game> scanSteam() {
        Map<String, Game> gamesByAppId = new LinkedHashMap<>();
        String steamPath = getSteamPath();

        if (steamPath == null || steamPath.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> libraryPaths = getSteamLibraryPaths(steamPath);

        for (String libraryPath : libraryPaths) {
            Path steamappsPath = Paths.get(libraryPath, "steamapps");
            if (!Files.exists(steamappsPath)) continue;

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(steamappsPath, "appmanifest_*.acf")) {
                for (Path acfFile : stream) {
                    Game game = parseSteamAcf(acfFile);
                    if (game != null && game.getAppId() != null && !gamesByAppId.containsKey(game.getAppId())) {
                        gamesByAppId.put(game.getAppId(), game);
                    }
                }
            } catch (IOException e) {
                System.err.println("[ScannerService] Error reading Steam library: " + e.getMessage());
            }
        }

        return new ArrayList<>(gamesByAppId.values());
    }

    private String getSteamPath() {
        try {
            ProcessBuilder pb = new ProcessBuilder("reg", "query",
                "HKEY_CURRENT_USER\\Software\\Valve\\Steam", "/v", "SteamPath");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("SteamPath")) {
                        String[] parts = line.split("REG_SZ");
                        if (parts.length > 1) return parts[1].trim();
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            System.err.println("[ScannerService] Error reading Steam registry: " + e.getMessage());
        }
        return null;
    }

    private List<String> getSteamLibraryPaths(String steamPath) {
        List<String> paths = new ArrayList<>();
        paths.add(steamPath);

        Path libraryFoldersPath = Paths.get(steamPath, "steamapps", "libraryfolders.vdf");
        if (Files.exists(libraryFoldersPath)) {
            try {
                String content = Files.readString(libraryFoldersPath);
                Pattern pathPattern = Pattern.compile("\"path\"\\s*\"([^\"]+)\"");
                Matcher matcher = pathPattern.matcher(content);
                while (matcher.find()) {
                    String path = matcher.group(1).replace("\\\\", "\\");
                    if (!paths.contains(path)) paths.add(path);
                }
            } catch (IOException e) {
                System.err.println("[ScannerService] Error reading libraryfolders.vdf");
            }
        }

        return paths;
    }

    private Game parseSteamAcf(Path acfFile) {
        try {
            String content = Files.readString(acfFile, StandardCharsets.UTF_8);

            String appId = extractVdfValue(content, "appid");
            String name = extractVdfValue(content, "name");
            String installDir = extractVdfValue(content, "installdir");

            if (appId == null || name == null || name.isEmpty() || installDir == null) return null;

            // Skip non-games
            String nameLower = name.toLowerCase();
            if (nameLower.contains("proton") || nameLower.contains("steamworks") ||
                nameLower.contains("redistributable") || nameLower.contains("sdk") ||
                nameLower.contains("directx") || nameLower.contains("vcredist") ||
                nameLower.contains("runtime") || nameLower.contains("tool") ||
                nameLower.contains("soundtrack") || nameLower.contains("dedicated server")) {
                return null;
            }

            Game game = new Game();
            game.setTitle(name);
            game.setAppId(appId);
            game.setPlatform(Platform.STEAM);
            game.setUniqueId(Game.generateUniqueId(Platform.STEAM, appId));

            Path steamappsPath = acfFile.getParent();
            Path gamePath = steamappsPath.resolve("common").resolve(installDir);
            game.setInstallPath(gamePath.toString());
            game.setStatus(Files.exists(gamePath) ? Status.READY : Status.MISSING);

            metadataService.applyMetadata(game);
            return game;
        } catch (IOException e) {
            return null;
        }
    }

    private String extractVdfValue(String content, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        return matcher.find() ? matcher.group(1) : null;
    }

    // ==================== EPIC GAMES SCANNER ====================

    public List<Game> scanEpic() {
        List<Game> games = new ArrayList<>();

        String programData = System.getenv("ProgramData");
        if (programData == null) programData = "C:\\ProgramData";

        Path manifestsPath = Paths.get(programData, "Epic", "EpicGamesLauncher", "Data", "Manifests");
        if (!Files.exists(manifestsPath)) return games;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(manifestsPath, "*.item")) {
            for (Path itemFile : stream) {
                Game game = parseEpicManifest(itemFile);
                if (game != null) games.add(game);
            }
        } catch (IOException e) {
            System.err.println("[ScannerService] Error reading Epic manifests: " + e.getMessage());
        }

        return games;
    }

    private Game parseEpicManifest(Path itemFile) {
        try {
            String content = Files.readString(itemFile, StandardCharsets.UTF_8);

            String displayName = extractJsonValue(content, "DisplayName");
            String installLocation = extractJsonValue(content, "InstallLocation");
            String launchExecutable = extractJsonValue(content, "LaunchExecutable");
            String appName = extractJsonValue(content, "AppName");

            if (displayName == null || displayName.isEmpty()) return null;

            // Skip launchers and tools
            String nameLower = displayName.toLowerCase();
            if (nameLower.contains("launcher") || nameLower.contains("plugin") ||
                nameLower.contains("unreal engine") || nameLower.contains("editor")) {
                return null;
            }

            Game game = new Game();
            game.setTitle(displayName);
            game.setAppId(appName);
            game.setPlatform(Platform.EPIC);
            game.setUniqueId(Game.generateUniqueId(Platform.EPIC, appName != null ? appName : displayName));
            game.setInstallPath(installLocation);

            if (installLocation != null && launchExecutable != null) {
                Path exePath = Paths.get(installLocation, launchExecutable);
                game.setExecutablePath(exePath.toString());
                game.setStatus(Files.exists(exePath) ? Status.READY : Status.MISSING);
            } else {
                game.setStatus(Status.MISSING);
            }

            metadataService.applyMetadata(game);
            return game;
        } catch (IOException e) {
            return null;
        }
    }

    private String extractJsonValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1).replace("\\\\", "\\").replace("\\/", "/");
        }
        return null;
    }

    // ==================== RIOT GAMES SCANNER ====================

    public List<Game> scanRiotGames() {
        List<Game> games = new ArrayList<>();

        // Find Riot Client installs file
        String programData = System.getenv("ProgramData");
        if (programData == null) programData = "C:\\ProgramData";

        Path riotInstallsPath = Paths.get(programData, "Riot Games", "RiotClientInstalls.json");

        if (Files.exists(riotInstallsPath)) {
            try {
                String content = Files.readString(riotInstallsPath, StandardCharsets.UTF_8);
                games.addAll(parseRiotInstalls(content));
            } catch (IOException e) {
                System.err.println("[ScannerService] Error reading Riot installs: " + e.getMessage());
            }
        }

        // Also check common Riot Games installation paths
        String[] riotPaths = {
            "C:\\Riot Games",
            "D:\\Riot Games",
            System.getenv("ProgramFiles") + "\\Riot Games"
        };

        for (String riotPath : riotPaths) {
            if (riotPath == null) continue;
            Path path = Paths.get(riotPath);
            if (Files.exists(path)) {
                games.addAll(scanRiotDirectory(path));
            }
        }

        return games;
    }

    private List<Game> parseRiotInstalls(String json) {
        List<Game> games = new ArrayList<>();

        // Parse associated_client entries which contain installed games
        // Format: "associated_client": { "path": "product_id", ... }
        Map<String, String> riotProducts = Map.of(
            "league_of_legends", "League of Legends",
            "valorant", "VALORANT",
            "lor", "Legends of Runeterra",
            "bacon", "Legends of Runeterra",  // Internal code name
            "tft", "Teamfight Tactics"
        );

        for (Map.Entry<String, String> product : riotProducts.entrySet()) {
            String productId = product.getKey();
            String gameName = product.getValue();

            // Look for product paths in the JSON
            Pattern pattern = Pattern.compile("\"([^\"]+" + productId + "[^\"]*?)\"\\s*:\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(json);

            while (matcher.find()) {
                String value = matcher.group(2).replace("\\\\", "\\");
                if (value.contains("Riot") || value.contains("Games") || Files.exists(Paths.get(value))) {
                    Game game = createRiotGame(gameName, value);
                    if (game != null && game.getStatus() == Status.READY) {
                        games.add(game);
                        break; // Only add once per game
                    }
                }
            }
        }

        return games;
    }

    private List<Game> scanRiotDirectory(Path riotPath) {
        List<Game> games = new ArrayList<>();

        // Map of folder names to game names
        Map<String, String> riotFolders = Map.of(
            "League of Legends", "League of Legends",
            "VALORANT", "VALORANT",
            "LoR", "Legends of Runeterra",
            "Legends of Runeterra", "Legends of Runeterra"
        );

        try {
            for (Map.Entry<String, String> entry : riotFolders.entrySet()) {
                Path gamePath = riotPath.resolve(entry.getKey());
                if (Files.exists(gamePath)) {
                    Game game = createRiotGame(entry.getValue(), gamePath.toString());
                    if (game != null) games.add(game);
                }
            }
        } catch (Exception e) {
            System.err.println("[ScannerService] Error scanning Riot directory: " + e.getMessage());
        }

        return games;
    }

    private Game createRiotGame(String title, String installPath) {
        Game game = new Game();
        game.setTitle(title);
        game.setPlatform(Platform.SYSTEM);
        game.setUniqueId(Game.generateUniqueId(Platform.SYSTEM, "riot_" + title.toLowerCase().replace(" ", "_")));
        game.setInstallPath(installPath);

        // Find executable based on game
        String exeName = switch (title) {
            case "League of Legends" -> "LeagueClient.exe";
            case "VALORANT" -> "VALORANT.exe";
            case "Legends of Runeterra" -> "LoR.exe";
            default -> null;
        };

        if (exeName != null) {
            try (var stream = Files.walk(Paths.get(installPath), 4)) {
                Optional<Path> exe = stream
                    .filter(p -> p.getFileName().toString().equalsIgnoreCase(exeName))
                    .findFirst();
                if (exe.isPresent()) {
                    game.setExecutablePath(exe.get().toString());
                    game.setStatus(Status.READY);
                } else {
                    game.setStatus(Status.MISSING);
                }
            } catch (IOException e) {
                game.setStatus(Status.MISSING);
            }
        } else {
            game.setStatus(Status.MISSING);
        }

        metadataService.applyMetadata(game);
        return game;
    }

    // ==================== BATTLE.NET SCANNER ====================

    public List<Game> scanBattleNet() {
        List<Game> games = new ArrayList<>();

        // Battle.net stores install info in product.db (protobuf) and config files
        String programData = System.getenv("ProgramData");
        if (programData == null) programData = "C:\\ProgramData";

        // Check Battle.net agent config
        Path agentPath = Paths.get(programData, "Battle.net", "Agent");
        Path configPath = Paths.get(System.getenv("APPDATA"), "Battle.net", "Battle.net.config");

        // Known Battle.net games with their product codes
        Map<String, String> bnetProducts = new LinkedHashMap<>();
        bnetProducts.put("wow", "World of Warcraft");
        bnetProducts.put("wow_classic", "World of Warcraft Classic");
        bnetProducts.put("d3", "Diablo III");
        bnetProducts.put("d4", "Diablo IV");
        bnetProducts.put("ow", "Overwatch");
        bnetProducts.put("pro", "Overwatch 2");
        bnetProducts.put("heroes", "Heroes of the Storm");
        bnetProducts.put("hs", "Hearthstone");
        bnetProducts.put("s2", "StarCraft II");
        bnetProducts.put("s1", "StarCraft Remastered");
        bnetProducts.put("w3", "Warcraft III: Reforged");
        bnetProducts.put("viper", "Call of Duty: Black Ops Cold War");
        bnetProducts.put("fore", "Call of Duty: Vanguard");
        bnetProducts.put("zeus", "Call of Duty: Modern Warfare II");
        bnetProducts.put("fenris", "Diablo IV");
        bnetProducts.put("anbs", "Diablo Immortal");
        bnetProducts.put("rtro", "Blizzard Arcade Collection");
        bnetProducts.put("wlby", "Crash Bandicoot 4");

        // Check common Battle.net install locations
        String[] bnetPaths = {
            "C:\\Program Files (x86)\\Battle.net",
            "C:\\Program Files\\Battle.net",
            "C:\\Program Files (x86)\\Blizzard Entertainment",
            "C:\\Program Files\\Blizzard Entertainment",
            "D:\\Games\\Battle.net",
            "D:\\Battle.net"
        };

        for (String basePath : bnetPaths) {
            Path base = Paths.get(basePath);
            if (!Files.exists(base)) continue;

            try (var dirs = Files.list(base)) {
                dirs.filter(Files::isDirectory).forEach(dir -> {
                    String folderName = dir.getFileName().toString().toLowerCase();

                    for (Map.Entry<String, String> product : bnetProducts.entrySet()) {
                        String gameName = product.getValue();
                        if (folderName.contains(gameName.toLowerCase().replace(" ", "").replace(":", "")) ||
                            folderName.contains(product.getKey())) {
                            Game game = createBattleNetGame(gameName, dir.toString());
                            if (game != null) games.add(game);
                        }
                    }
                });
            } catch (IOException e) {
                // Continue
            }
        }

        // Also check Battle.net config for installed games
        if (Files.exists(configPath)) {
            try {
                String config = Files.readString(configPath, StandardCharsets.UTF_8);
                for (Map.Entry<String, String> product : bnetProducts.entrySet()) {
                    if (config.contains("\"" + product.getKey() + "\"")) {
                        // Game is likely installed, try to find it
                        Pattern pathPattern = Pattern.compile("\"" + product.getKey() + "\"[^}]*?\"InstallPath\"\\s*:\\s*\"([^\"]+)\"");
                        Matcher matcher = pathPattern.matcher(config);
                        if (matcher.find()) {
                            String installPath = matcher.group(1).replace("\\\\", "\\");
                            Game game = createBattleNetGame(product.getValue(), installPath);
                            if (game != null) games.add(game);
                        }
                    }
                }
            } catch (IOException e) {
                // Continue
            }
        }

        return games;
    }

    private Game createBattleNetGame(String title, String installPath) {
        Game game = new Game();
        game.setTitle(title);
        game.setPlatform(Platform.SYSTEM);
        game.setUniqueId(Game.generateUniqueId(Platform.SYSTEM, "bnet_" + title.toLowerCase().replace(" ", "_")));
        game.setInstallPath(installPath);

        // Check if path exists
        if (!Files.exists(Paths.get(installPath))) {
            return null;
        }

        // Try to find an executable
        try (var stream = Files.walk(Paths.get(installPath), 2)) {
            Optional<Path> exe = stream
                .filter(p -> p.toString().toLowerCase().endsWith(".exe"))
                .filter(p -> !p.getFileName().toString().toLowerCase().contains("unins"))
                .filter(p -> !p.getFileName().toString().toLowerCase().contains("crash"))
                .filter(p -> !p.getFileName().toString().toLowerCase().contains("update"))
                .findFirst();
            if (exe.isPresent()) {
                game.setExecutablePath(exe.get().toString());
                game.setStatus(Status.READY);
            } else {
                game.setStatus(Status.MISSING);
            }
        } catch (IOException e) {
            game.setStatus(Status.MISSING);
        }

        metadataService.applyMetadata(game);
        return game;
    }

    // ==================== EA APP / ORIGIN SCANNER ====================

    public List<Game> scanEAApp() {
        List<Game> games = new ArrayList<>();

        String programData = System.getenv("ProgramData");
        String localAppData = System.getenv("LOCALAPPDATA");

        // EA App stores data in ProgramData/EA Desktop
        Path eaContentPath = Paths.get(programData, "EA Desktop", "InstallData");
        Path originContentPath = Paths.get(programData, "Origin", "LocalContent");

        // Scan EA Desktop content
        if (Files.exists(eaContentPath)) {
            games.addAll(scanEADirectory(eaContentPath, "EA"));
        }

        // Scan Origin content (legacy)
        if (Files.exists(originContentPath)) {
            games.addAll(scanEADirectory(originContentPath, "Origin"));
        }

        // Check common EA game install locations
        String[] eaPaths = {
            "C:\\Program Files\\EA Games",
            "C:\\Program Files (x86)\\EA Games",
            "C:\\Program Files\\Electronic Arts",
            "C:\\Program Files (x86)\\Electronic Arts",
            "C:\\Program Files\\Origin Games",
            "C:\\Program Files (x86)\\Origin Games"
        };

        for (String eaPath : eaPaths) {
            if (Files.exists(Paths.get(eaPath))) {
                games.addAll(scanEADirectory(Paths.get(eaPath), "EA"));
            }
        }

        return games;
    }

    private List<Game> scanEADirectory(Path basePath, String source) {
        List<Game> games = new ArrayList<>();

        try (var dirs = Files.list(basePath)) {
            dirs.filter(Files::isDirectory).forEach(dir -> {
                String folderName = dir.getFileName().toString();

                // Skip non-game folders
                if (folderName.startsWith("_") || folderName.startsWith(".") ||
                    folderName.equalsIgnoreCase("__Installer") ||
                    folderName.toLowerCase().contains("redist")) {
                    return;
                }

                Game game = createEAGame(folderName, dir.toString());
                if (game != null && game.getStatus() == Status.READY) {
                    games.add(game);
                }
            });
        } catch (IOException e) {
            System.err.println("[ScannerService] Error scanning " + source + " directory: " + e.getMessage());
        }

        return games;
    }

    private Game createEAGame(String folderName, String installPath) {
        // Clean up folder name to game title
        String title = folderName
            .replaceAll("^EA SPORTS ", "EA SPORTS ")
            .replaceAll("™|®|©", "")
            .trim();

        Game game = new Game();
        game.setTitle(title);
        game.setPlatform(Platform.SYSTEM);
        game.setUniqueId(Game.generateUniqueId(Platform.SYSTEM, "ea_" + title.toLowerCase().replace(" ", "_")));
        game.setInstallPath(installPath);

        // Find executable
        try (var stream = Files.walk(Paths.get(installPath), 2)) {
            Optional<Path> exe = stream
                .filter(p -> p.toString().toLowerCase().endsWith(".exe"))
                .filter(p -> !p.getFileName().toString().toLowerCase().contains("unins"))
                .filter(p -> !p.getFileName().toString().toLowerCase().contains("crash"))
                .filter(p -> !p.getFileName().toString().toLowerCase().contains("redist"))
                .filter(p -> !p.getFileName().toString().toLowerCase().contains("setup"))
                .findFirst();
            if (exe.isPresent()) {
                game.setExecutablePath(exe.get().toString());
                game.setStatus(Status.READY);
            } else {
                return null; // No executable found
            }
        } catch (IOException e) {
            return null;
        }

        metadataService.applyMetadata(game);
        return game;
    }

    // ==================== KNOWN STANDALONE GAMES SCANNER ====================

    public List<Game> scanKnownStandalones() {
        List<Game> games = new ArrayList<>();
        Set<String> foundGames = new HashSet<>();

        String[] registryPaths = {
            "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
            "HKEY_LOCAL_MACHINE\\SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
            "HKEY_CURRENT_USER\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall"
        };

        for (String regPath : registryPaths) {
            try {
                ProcessBuilder pb = new ProcessBuilder("reg", "query", regPath);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                List<String> subkeys = new ArrayList<>();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("HK")) subkeys.add(line.trim());
                    }
                }
                process.waitFor();

                for (String subkey : subkeys) {
                    Game game = checkKnownGame(subkey, foundGames);
                    if (game != null) games.add(game);
                }
            } catch (Exception e) {
                // Continue
            }
        }

        // Also check known paths directly
        games.addAll(scanKnownPaths(foundGames));

        return games;
    }

    private Game checkKnownGame(String regKey, Set<String> foundGames) {
        try {
            ProcessBuilder pb = new ProcessBuilder("reg", "query", regKey);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String displayName = null, installLocation = null, displayIcon = null;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("DisplayName")) displayName = extractRegistryValue(line);
                    else if (line.contains("InstallLocation")) installLocation = extractRegistryValue(line);
                    else if (line.contains("DisplayIcon")) displayIcon = extractRegistryValue(line);
                }
            }
            process.waitFor();

            if (displayName == null) return null;

            // Check if this matches a known game
            String displayNameLower = displayName.toLowerCase();
            for (Map.Entry<String, String> entry : KNOWN_STANDALONE_GAMES.entrySet()) {
                if (displayNameLower.contains(entry.getKey())) {
                    String gameName = entry.getValue();
                    String uniqueKey = normalizeTitle(gameName);

                    if (foundGames.contains(uniqueKey)) return null;
                    foundGames.add(uniqueKey);

                    Game game = new Game();
                    game.setTitle(gameName);
                    game.setPlatform(Platform.SYSTEM);
                    game.setUniqueId(Game.generateUniqueId(Platform.SYSTEM, gameName));
                    game.setInstallPath(installLocation);

                    // Find executable
                    String exePath = findExecutable(installLocation, displayIcon, gameName);
                    game.setExecutablePath(exePath);
                    game.setStatus(exePath != null && Files.exists(Paths.get(exePath)) ? Status.READY : Status.MISSING);

                    metadataService.applyMetadata(game);
                    return game;
                }
            }
        } catch (Exception e) {
            // Continue
        }
        return null;
    }

    private String extractRegistryValue(String line) {
        String[] types = {"REG_SZ", "REG_EXPAND_SZ"};
        for (String type : types) {
            int idx = line.indexOf(type);
            if (idx != -1) return line.substring(idx + type.length()).trim();
        }
        return null;
    }

    private String findExecutable(String installLocation, String displayIcon, String gameName) {
        // Try display icon first
        if (displayIcon != null) {
            String iconPath = displayIcon.split(",")[0].replace("\"", "").trim();
            if (iconPath.toLowerCase().endsWith(".exe") && Files.exists(Paths.get(iconPath))) {
                return iconPath;
            }
        }

        // Search in install location
        if (installLocation != null && !installLocation.isEmpty() && Files.exists(Paths.get(installLocation))) {
            try (var stream = Files.walk(Paths.get(installLocation), 3)) {
                Optional<Path> exe = stream
                    .filter(p -> p.toString().toLowerCase().endsWith(".exe"))
                    .filter(p -> !p.getFileName().toString().toLowerCase().contains("unins"))
                    .filter(p -> !p.getFileName().toString().toLowerCase().contains("crash"))
                    .filter(p -> !p.getFileName().toString().toLowerCase().contains("update"))
                    .findFirst();
                if (exe.isPresent()) return exe.get().toString();
            } catch (IOException e) {
                // Continue
            }
        }

        return null;
    }

    private List<Game> scanKnownPaths(Set<String> foundGames) {
        List<Game> games = new ArrayList<>();

        // Minecraft
        String minecraftPath = System.getenv("APPDATA") + "\\.minecraft";
        if (Files.exists(Paths.get(minecraftPath)) && !foundGames.contains(normalizeTitle("Minecraft"))) {
            String[] possibleExes = {
                System.getenv("ProgramFiles(x86)") + "\\Minecraft Launcher\\MinecraftLauncher.exe",
                System.getenv("ProgramFiles") + "\\Minecraft Launcher\\MinecraftLauncher.exe",
                System.getenv("LOCALAPPDATA") + "\\Programs\\Minecraft Launcher\\MinecraftLauncher.exe"
            };

            for (String exe : possibleExes) {
                if (exe != null && Files.exists(Paths.get(exe))) {
                    Game minecraft = new Game();
                    minecraft.setTitle("Minecraft");
                    minecraft.setPlatform(Platform.SYSTEM);
                    minecraft.setUniqueId(Game.generateUniqueId(Platform.SYSTEM, "Minecraft"));
                    minecraft.setInstallPath(minecraftPath);
                    minecraft.setExecutablePath(exe);
                    minecraft.setStatus(Status.READY);
                    metadataService.applyMetadata(minecraft);
                    games.add(minecraft);
                    foundGames.add(normalizeTitle("Minecraft"));
                    break;
                }
            }
        }

        return games;
    }

    // ==================== DATABASE OPERATIONS ====================

    private List<Game> mergeWithDatabase(List<Game> scannedGames) {
        List<Game> result = new ArrayList<>();
        Set<String> processedTitles = new HashSet<>();

        for (Game scanned : scannedGames) {
            try {
                String normalizedTitle = normalizeTitle(scanned.getTitle());
                if (processedTitles.contains(normalizedTitle)) continue;
                processedTitles.add(normalizedTitle);

                Optional<Game> existing = gameRepository.findByUniqueId(scanned.getUniqueId());

                if (existing.isPresent()) {
                    Game dbGame = existing.get();
                    dbGame.setInstallPath(scanned.getInstallPath());
                    dbGame.setExecutablePath(scanned.getExecutablePath());
                    dbGame.setStatus(scanned.getStatus());

                    // Update metadata if scanned has it
                    if (scanned.getCoverImageUrl() != null && !scanned.getCoverImageUrl().isEmpty()) {
                        dbGame.setCoverImageUrl(scanned.getCoverImageUrl());
                    }
                    if (scanned.getDescription() != null && !scanned.getDescription().isEmpty() &&
                        !scanned.getDescription().equals("No description available.")) {
                        dbGame.setDescription(scanned.getDescription());
                    }
                    if (scanned.getDeveloper() != null && !scanned.getDeveloper().isEmpty() &&
                        !scanned.getDeveloper().equals("Unknown Developer")) {
                        dbGame.setDeveloper(scanned.getDeveloper());
                    }
                    if (scanned.getHeroImageUrl() != null && !scanned.getHeroImageUrl().isEmpty()) {
                        dbGame.setHeroImageUrl(scanned.getHeroImageUrl());
                    }

                    gameRepository.save(dbGame);
                    result.add(dbGame);
                } else {
                    Game saved = gameRepository.save(scanned);
                    result.add(saved);
                }
            } catch (Exception e) {
                System.err.println("[ScannerService] Error saving: " + scanned.getTitle() + " - " + e.getMessage());
            }
        }

        return result;
    }

    public String getDetectedSteamPath() {
        return getSteamPath();
    }

    public String getDetectedEpicPath() {
        String programData = System.getenv("ProgramData");
        if (programData == null) programData = "C:\\ProgramData";
        return Paths.get(programData, "Epic", "EpicGamesLauncher", "Data", "Manifests").toString();
    }
}

