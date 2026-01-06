package com.nexus.client.service;

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
 * Service responsible for scanning installed games from Steam, Epic Games, and System.
 * Uses Windows Registry and file parsing to detect installed games.
 */
public class ScannerService {

    private final GameRepository gameRepository;
    private final MetadataService metadataService;

    // Games to look for in the registry (displayName -> executable pattern)
    // ONLY actual games, NOT launchers or tools
    private static final Map<String, String> KNOWN_SYSTEM_GAMES = new LinkedHashMap<>();
    static {
        // Actual games only - no launchers
        KNOWN_SYSTEM_GAMES.put("Minecraft", "MinecraftLauncher");
        KNOWN_SYSTEM_GAMES.put("League of Legends", "LeagueClient");
        KNOWN_SYSTEM_GAMES.put("VALORANT", "VALORANT");
        KNOWN_SYSTEM_GAMES.put("Genshin Impact", "GenshinImpact");
        KNOWN_SYSTEM_GAMES.put("Honkai", "StarRail");
        KNOWN_SYSTEM_GAMES.put("Zenless Zone Zero", "ZenlessZoneZero");
        KNOWN_SYSTEM_GAMES.put("Osu!", "osu!");
        KNOWN_SYSTEM_GAMES.put("Roblox", "RobloxPlayer");
    }

    // Apps/Launchers to EXCLUDE from scanning
    private static final Set<String> EXCLUDED_APPS = new HashSet<>();
    static {
        EXCLUDED_APPS.add("discord");
        EXCLUDED_APPS.add("battle.net");
        EXCLUDED_APPS.add("blizzard");
        EXCLUDED_APPS.add("ubisoft connect");
        EXCLUDED_APPS.add("uplay");
        EXCLUDED_APPS.add("origin");
        EXCLUDED_APPS.add("ea app");
        EXCLUDED_APPS.add("ea desktop");
        EXCLUDED_APPS.add("gog galaxy");
        EXCLUDED_APPS.add("steam");
        EXCLUDED_APPS.add("epic games launcher");
        EXCLUDED_APPS.add("riot client");
        EXCLUDED_APPS.add("rockstar games launcher");
        EXCLUDED_APPS.add("bethesda.net launcher");
        EXCLUDED_APPS.add("amazon games");
        EXCLUDED_APPS.add("xbox");
        EXCLUDED_APPS.add("nvidia");
        EXCLUDED_APPS.add("geforce");
        EXCLUDED_APPS.add("amd software");
        EXCLUDED_APPS.add("razer");
        EXCLUDED_APPS.add("logitech");
        EXCLUDED_APPS.add("steelseries");
        EXCLUDED_APPS.add("corsair");
        EXCLUDED_APPS.add("msi");
        EXCLUDED_APPS.add("asus");
        EXCLUDED_APPS.add("overwolf");
    }

    public ScannerService() {
        this.gameRepository = new GameRepository();
        // Use dynamic API-based metadata service
        this.metadataService = new CombinedMetadataService();
    }

    public ScannerService(GameRepository gameRepository, MetadataService metadataService) {
        this.gameRepository = gameRepository;
        this.metadataService = metadataService;
    }

    /**
     * Performs a complete rescan by clearing the database first.
     * Use this to fix duplicate issues.
     */
    public List<Game> fullRescan() {
        System.out.println("[ScannerService] Performing full rescan (clearing database first)...");
        try {
            int deleted = gameRepository.deleteAll();
            System.out.println("[ScannerService] Cleared " + deleted + " games from database");
        } catch (Exception e) {
            System.err.println("[ScannerService] Error clearing database: " + e.getMessage());
        }
        return scanAll();
    }

    /**
     * Scans all sources and returns a merged list of games.
     * Updates existing games in DB and adds new ones.
     */
    public List<Game> scanAll() {
        System.out.println("[ScannerService] Starting full scan...");

        // Use multiple maps to ensure robust deduplication
        Map<String, Game> gameByUniqueId = new LinkedHashMap<>();
        Set<String> seenAppIds = new HashSet<>();  // For Steam deduplication
        Set<String> seenNormalizedTitles = new HashSet<>(); // Title-based dedup

        // Scan Steam
        try {
            List<Game> steamGames = scanSteam();
            System.out.println("[ScannerService] Found " + steamGames.size() + " Steam games");
            for (Game g : steamGames) {
                if (g.getAppId() != null && !seenAppIds.contains(g.getAppId())) {
                    String normalizedTitle = normalizeTitle(g.getTitle());
                    if (!seenNormalizedTitles.contains(normalizedTitle)) {
                        seenAppIds.add(g.getAppId());
                        seenNormalizedTitles.add(normalizedTitle);
                        gameByUniqueId.put(g.getUniqueId(), g);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ScannerService] Steam scan failed: " + e.getMessage());
        }

        // Scan Epic
        try {
            List<Game> epicGames = scanEpic();
            System.out.println("[ScannerService] Found " + epicGames.size() + " Epic games");
            for (Game g : epicGames) {
                String normalizedTitle = normalizeTitle(g.getTitle());
                // Skip if we already have a game with the same normalized title (from Steam)
                if (!seenNormalizedTitles.contains(normalizedTitle)) {
                    if (g.getUniqueId() != null && !gameByUniqueId.containsKey(g.getUniqueId())) {
                        seenNormalizedTitles.add(normalizedTitle);
                        gameByUniqueId.put(g.getUniqueId(), g);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ScannerService] Epic scan failed: " + e.getMessage());
        }

        // System scan disabled - it was adding launchers like Discord, Battle.net
        // Users can manually add system games if needed

        List<Game> allGames = new ArrayList<>(gameByUniqueId.values());

        // Merge with database
        List<Game> mergedGames = mergeWithDatabase(allGames);
        System.out.println("[ScannerService] Scan complete. Total games: " + mergedGames.size());

        return mergedGames;
    }

    /**
     * Normalizes a game title for comparison to detect duplicates.
     * Removes punctuation, extra spaces, and common suffixes.
     */
    private String normalizeTitle(String title) {
        if (title == null) return "";
        return title.toLowerCase()
                .replaceAll("[^a-z0-9]", "")  // Remove all non-alphanumeric
                .replaceAll("edition$", "")
                .replaceAll("remastered$", "")
                .replaceAll("definitive$", "")
                .replaceAll("goty$", "")
                .replaceAll("complete$", "")
                .trim();
    }

    /**
     * Scans Steam library by reading the Windows Registry and parsing .acf files.
     * Uses robust deduplication to prevent multiple entries for the same game.
     */
    public List<Game> scanSteam() {
        // Use LinkedHashMap keyed by appId to deduplicate at scanning level
        Map<String, Game> gamesByAppId = new LinkedHashMap<>();
        String steamPath = getSteamPath();

        if (steamPath == null || steamPath.isEmpty()) {
            System.out.println("[ScannerService] Steam not found in registry");
            return new ArrayList<>();
        }

        System.out.println("[ScannerService] Steam path: " + steamPath);

        // Find all library folders
        List<String> libraryPaths = getSteamLibraryPaths(steamPath);
        System.out.println("[ScannerService] Found " + libraryPaths.size() + " Steam library paths");

        for (String libraryPath : libraryPaths) {
            Path steamappsPath = Paths.get(libraryPath, "steamapps");
            if (!Files.exists(steamappsPath)) {
                continue;
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(steamappsPath, "appmanifest_*.acf")) {
                for (Path acfFile : stream) {
                    Game game = parseSteamAcf(acfFile);
                    if (game != null && game.getAppId() != null) {
                        // Only add if not already present (first one wins by appId)
                        if (!gamesByAppId.containsKey(game.getAppId())) {
                            gamesByAppId.put(game.getAppId(), game);
                            System.out.println("[ScannerService] Added Steam game: " + game.getTitle() + " (AppId: " + game.getAppId() + ")");
                        } else {
                            System.out.println("[ScannerService] Skipped duplicate Steam game: " + game.getTitle() + " (AppId: " + game.getAppId() + ")");
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("[ScannerService] Error reading Steam library: " + e.getMessage());
            }
        }

        System.out.println("[ScannerService] Total unique Steam games: " + gamesByAppId.size());
        return new ArrayList<>(gamesByAppId.values());
    }

    /**
     * Gets Steam installation path from Windows Registry.
     */
    private String getSteamPath() {
        try {
            // Query Windows Registry for Steam path
            ProcessBuilder pb = new ProcessBuilder(
                    "reg", "query", "HKEY_CURRENT_USER\\Software\\Valve\\Steam", "/v", "SteamPath"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("SteamPath")) {
                        // Extract the path value
                        String[] parts = line.split("REG_SZ");
                        if (parts.length > 1) {
                            return parts[1].trim();
                        }
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            System.err.println("[ScannerService] Error reading Steam registry: " + e.getMessage());
        }
        return null;
    }

    /**
     * Gets all Steam library paths (main + additional library folders).
     */
    private List<String> getSteamLibraryPaths(String steamPath) {
        List<String> paths = new ArrayList<>();
        paths.add(steamPath);

        // Parse libraryfolders.vdf for additional library locations
        Path libraryFoldersPath = Paths.get(steamPath, "steamapps", "libraryfolders.vdf");
        if (Files.exists(libraryFoldersPath)) {
            try {
                String content = Files.readString(libraryFoldersPath);
                // Match "path" entries in VDF format
                Pattern pathPattern = Pattern.compile("\"path\"\\s*\"([^\"]+)\"");
                Matcher matcher = pathPattern.matcher(content);
                while (matcher.find()) {
                    String path = matcher.group(1).replace("\\\\", "\\");
                    if (!paths.contains(path)) {
                        paths.add(path);
                    }
                }
            } catch (IOException e) {
                System.err.println("[ScannerService] Error reading libraryfolders.vdf: " + e.getMessage());
            }
        }

        return paths;
    }

    /**
     * Parses a Steam .acf manifest file to extract game info.
     */
    private Game parseSteamAcf(Path acfFile) {
        try {
            String content = Files.readString(acfFile, StandardCharsets.UTF_8);

            String appId = extractVdfValue(content, "appid");
            String name = extractVdfValue(content, "name");
            String installDir = extractVdfValue(content, "installdir");

            if (appId == null || name == null || name.isEmpty()) {
                return null;
            }

            // Skip tools, SDKs, Proton, and other non-game content
            String nameLower = name.toLowerCase();
            if (nameLower.contains("proton") ||
                nameLower.contains("steamworks") ||
                nameLower.contains("redistributable") ||
                nameLower.contains("dedicated server") ||
                nameLower.contains("sdk") ||
                nameLower.contains("directx") ||
                nameLower.contains("vcredist") ||
                nameLower.contains("runtime") ||
                nameLower.contains("tool") ||
                nameLower.contains("benchmark") ||
                nameLower.contains("soundtrack") ||
                nameLower.contains("artbook") ||
                nameLower.contains("wallpaper") ||
                nameLower.contains(" demo") ||
                nameLower.equals("demo") ||
                nameLower.contains("playtest") ||
                nameLower.contains("trailer") ||
                nameLower.contains("video")) {
                return null;
            }

            // Skip if installDir is null
            if (installDir == null || installDir.isEmpty()) {
                return null;
            }

            Game game = new Game();
            game.setTitle(name);
            game.setAppId(appId);
            game.setPlatform(Platform.STEAM);
            game.setUniqueId(Game.generateUniqueId(Platform.STEAM, appId));

            // Determine install path
            Path steamappsPath = acfFile.getParent();
            Path gamePath = steamappsPath.resolve("common").resolve(installDir);
            game.setInstallPath(gamePath.toString());

            // Check if game folder exists
            if (Files.exists(gamePath)) {
                game.setStatus(Status.READY);
            } else {
                game.setStatus(Status.MISSING);
            }

            // Apply metadata (cover image, description)
            metadataService.applyMetadata(game);

            return game;
        } catch (IOException e) {
            System.err.println("[ScannerService] Error parsing ACF file: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extracts a value from VDF format text.
     */
    private String extractVdfValue(String content, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Scans Epic Games library by parsing manifest files.
     */
    public List<Game> scanEpic() {
        List<Game> games = new ArrayList<>();

        // Epic Games manifests location
        String programData = System.getenv("ProgramData");
        if (programData == null) {
            programData = "C:\\ProgramData";
        }

        Path manifestsPath = Paths.get(programData, "Epic", "EpicGamesLauncher", "Data", "Manifests");

        if (!Files.exists(manifestsPath)) {
            System.out.println("[ScannerService] Epic manifests folder not found: " + manifestsPath);
            return games;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(manifestsPath, "*.item")) {
            for (Path itemFile : stream) {
                Game game = parseEpicManifest(itemFile);
                if (game != null) {
                    games.add(game);
                }
            }
        } catch (IOException e) {
            System.err.println("[ScannerService] Error reading Epic manifests: " + e.getMessage());
        }

        return games;
    }

    /**
     * Parses an Epic Games .item manifest file (JSON format).
     */
    private Game parseEpicManifest(Path itemFile) {
        try {
            String content = Files.readString(itemFile, StandardCharsets.UTF_8);

            // Simple JSON parsing without external library
            String displayName = extractJsonValue(content, "DisplayName");
            String installLocation = extractJsonValue(content, "InstallLocation");
            String launchExecutable = extractJsonValue(content, "LaunchExecutable");
            String appName = extractJsonValue(content, "AppName");

            if (displayName == null || displayName.isEmpty()) {
                return null;
            }

            // Check if this is an excluded app (launcher, tool, etc.)
            String nameLower = displayName.toLowerCase();
            for (String excluded : EXCLUDED_APPS) {
                if (nameLower.contains(excluded)) {
                    return null;
                }
            }

            // Skip if this looks like a launcher or plugin
            if (nameLower.contains("launcher") ||
                nameLower.contains("plugin") ||
                nameLower.contains("unreal engine") ||
                nameLower.contains("editor")) {
                return null;
            }

            Game game = new Game();
            game.setTitle(displayName);
            game.setAppId(appName);
            game.setPlatform(Platform.EPIC);
            game.setUniqueId(Game.generateUniqueId(Platform.EPIC, appName != null ? appName : displayName));
            game.setInstallPath(installLocation);

            // Build executable path
            if (installLocation != null && launchExecutable != null) {
                Path exePath = Paths.get(installLocation, launchExecutable);
                game.setExecutablePath(exePath.toString());

                if (Files.exists(exePath)) {
                    game.setStatus(Status.READY);
                } else {
                    game.setStatus(Status.MISSING);
                }
            } else {
                game.setStatus(Status.MISSING);
            }

            // Apply metadata
            metadataService.applyMetadata(game);

            return game;
        } catch (IOException e) {
            System.err.println("[ScannerService] Error parsing Epic manifest: " + e.getMessage());
            return null;
        }
    }

    /**
     * Simple JSON value extractor (without external library).
     */
    private String extractJsonValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1).replace("\\\\", "\\").replace("\\/", "/");
        }
        return null;
    }

    /**
     * Scans system for standalone games using Windows Registry and known paths.
     */
    public List<Game> scanSystem() {
        List<Game> games = new ArrayList<>();
        Set<String> foundGames = new HashSet<>();

        // Scan Windows Uninstall Registry
        games.addAll(scanUninstallRegistry(foundGames));

        // Check known game paths
        games.addAll(scanKnownPaths(foundGames));

        return games;
    }

    /**
     * Scans Windows Uninstall Registry for installed applications.
     */
    private List<Game> scanUninstallRegistry(Set<String> foundGames) {
        List<Game> games = new ArrayList<>();

        String[] registryPaths = {
                "HKEY_LOCAL_MACHINE\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
                "HKEY_LOCAL_MACHINE\\SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
                "HKEY_CURRENT_USER\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall"
        };

        for (String regPath : registryPaths) {
            try {
                // Get all subkeys
                ProcessBuilder pb = new ProcessBuilder("reg", "query", regPath);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                List<String> subkeys = new ArrayList<>();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("HK")) {
                            subkeys.add(line.trim());
                        }
                    }
                }
                process.waitFor();

                // Query each subkey for game info
                for (String subkey : subkeys) {
                    Game game = queryRegistryForGame(subkey, foundGames);
                    if (game != null) {
                        games.add(game);
                    }
                }
            } catch (Exception e) {
                // Silent fail for registry access
            }
        }

        return games;
    }

    /**
     * Queries a registry key and determines if it's a known game.
     */
    private Game queryRegistryForGame(String regKey, Set<String> foundGames) {
        try {
            ProcessBuilder pb = new ProcessBuilder("reg", "query", regKey);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String displayName = null;
            String installLocation = null;
            String displayIcon = null;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("DisplayName")) {
                        displayName = extractRegistryValue(line);
                    } else if (line.contains("InstallLocation")) {
                        installLocation = extractRegistryValue(line);
                    } else if (line.contains("DisplayIcon")) {
                        displayIcon = extractRegistryValue(line);
                    }
                }
            }
            process.waitFor();

            if (displayName == null || displayName.isEmpty()) {
                return null;
            }

            // Check if this is a known game
            String matchedGame = null;
            for (Map.Entry<String, String> entry : KNOWN_SYSTEM_GAMES.entrySet()) {
                if (displayName.toLowerCase().contains(entry.getKey().toLowerCase())) {
                    matchedGame = entry.getKey();
                    break;
                }
            }

            if (matchedGame == null) {
                return null;
            }

            // Avoid duplicates
            String uniqueKey = matchedGame.toLowerCase();
            if (foundGames.contains(uniqueKey)) {
                return null;
            }
            foundGames.add(uniqueKey);

            Game game = new Game();
            game.setTitle(matchedGame);
            game.setPlatform(Platform.SYSTEM);
            game.setUniqueId(Game.generateUniqueId(Platform.SYSTEM, matchedGame));
            game.setInstallPath(installLocation);

            // Try to find executable
            String exePath = findExecutable(installLocation, displayIcon, KNOWN_SYSTEM_GAMES.get(matchedGame));
            game.setExecutablePath(exePath);
            game.setStatus(exePath != null && Files.exists(Paths.get(exePath)) ? Status.READY : Status.MISSING);

            // Apply metadata
            metadataService.applyMetadata(game);

            return game;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extracts the value part from a registry query line.
     */
    private String extractRegistryValue(String line) {
        String[] types = {"REG_SZ", "REG_EXPAND_SZ"};
        for (String type : types) {
            int idx = line.indexOf(type);
            if (idx != -1) {
                return line.substring(idx + type.length()).trim();
            }
        }
        return null;
    }

    /**
     * Tries to find the game executable.
     */
    private String findExecutable(String installLocation, String displayIcon, String exePattern) {
        // First try display icon (often points to exe)
        if (displayIcon != null) {
            String iconPath = displayIcon.split(",")[0].replace("\"", "").trim();
            if (iconPath.toLowerCase().endsWith(".exe") && Files.exists(Paths.get(iconPath))) {
                return iconPath;
            }
        }

        // Search in install location
        if (installLocation != null && !installLocation.isEmpty()) {
            Path installPath = Paths.get(installLocation);
            if (Files.exists(installPath)) {
                try (var stream = Files.walk(installPath, 2)) {
                    // Find exe matching pattern
                    Optional<Path> exe = stream
                            .filter(p -> p.toString().toLowerCase().endsWith(".exe"))
                            .filter(p -> p.getFileName().toString().toLowerCase().contains(exePattern.toLowerCase()))
                            .findFirst();
                    if (exe.isPresent()) {
                        return exe.get().toString();
                    }
                } catch (IOException ignored) {}
            }
        }

        return null;
    }

    /**
     * Scans known game paths for standalone games.
     */
    private List<Game> scanKnownPaths(Set<String> foundGames) {
        List<Game> games = new ArrayList<>();

        // Check Minecraft
        String minecraftPath = System.getenv("APPDATA") + "\\.minecraft";
        if (Files.exists(Paths.get(minecraftPath)) && !foundGames.contains("minecraft")) {
            Game minecraft = new Game();
            minecraft.setTitle("Minecraft");
            minecraft.setPlatform(Platform.SYSTEM);
            minecraft.setUniqueId(Game.generateUniqueId(Platform.SYSTEM, "minecraft"));
            minecraft.setInstallPath(minecraftPath);

            // Find minecraft launcher
            String[] possibleExes = {
                    System.getenv("ProgramFiles(x86)") + "\\Minecraft Launcher\\MinecraftLauncher.exe",
                    System.getenv("ProgramFiles") + "\\Minecraft Launcher\\MinecraftLauncher.exe",
                    System.getenv("LOCALAPPDATA") + "\\Programs\\Minecraft Launcher\\MinecraftLauncher.exe"
            };
            for (String exe : possibleExes) {
                if (Files.exists(Paths.get(exe))) {
                    minecraft.setExecutablePath(exe);
                    minecraft.setStatus(Status.READY);
                    break;
                }
            }
            if (minecraft.getExecutablePath() == null) {
                minecraft.setStatus(Status.MISSING);
            }

            metadataService.applyMetadata(minecraft);
            games.add(minecraft);
            foundGames.add("minecraft");
        }

        // Check Riot Games - expanded paths
        String[] riotPaths = {
                "C:\\Riot Games",
                "D:\\Riot Games",
                "E:\\Riot Games",
                System.getenv("ProgramFiles") + "\\Riot Games",
                System.getenv("ProgramFiles(x86)") + "\\Riot Games",
                System.getenv("LOCALAPPDATA") + "\\Riot Games"
        };

        for (String riotPath : riotPaths) {
            if (riotPath == null || !Files.exists(Paths.get(riotPath))) {
                continue;
            }

            // League of Legends - check multiple possible locations
            if (!foundGames.contains("league of legends")) {
                Path lolPath = Paths.get(riotPath, "League of Legends");
                if (Files.exists(lolPath)) {
                    Game lol = createRiotGame("League of Legends", lolPath.toString(),
                            new String[]{"LeagueClient.exe", "League of Legends.exe", "LeagueClientUx.exe"});
                    if (lol != null) {
                        games.add(lol);
                        foundGames.add("league of legends");
                        System.out.println("[ScannerService] Found League of Legends at: " + lolPath);
                    }
                }
            }

            // VALORANT
            if (!foundGames.contains("valorant")) {
                Path valorantPath = Paths.get(riotPath, "VALORANT");
                if (!Files.exists(valorantPath)) {
                    valorantPath = Paths.get(riotPath, "Valorant"); // Try alternate casing
                }
                if (Files.exists(valorantPath)) {
                    Game valorant = createRiotGame("VALORANT", valorantPath.toString(),
                            new String[]{"VALORANT.exe", "ShooterGame.exe", "Valorant.exe"});
                    if (valorant != null) {
                        games.add(valorant);
                        foundGames.add("valorant");
                        System.out.println("[ScannerService] Found VALORANT at: " + valorantPath);
                    }
                }
            }

            // Legends of Runeterra
            if (!foundGames.contains("legends of runeterra")) {
                Path lorPath = Paths.get(riotPath, "LoR");
                if (Files.exists(lorPath)) {
                    Game lor = createRiotGame("Legends of Runeterra", lorPath.toString(),
                            new String[]{"LoR.exe", "Legends of Runeterra.exe"});
                    if (lor != null) {
                        games.add(lor);
                        foundGames.add("legends of runeterra");
                    }
                }
            }
        }

        // Also check for Riot Client installation via registry
        games.addAll(scanRiotClientRegistry(foundGames));

        return games;
    }

    /**
     * Creates a Riot game entry with multiple possible executable names.
     */
    private Game createRiotGame(String title, String installPath, String[] exeNames) {
        Game game = new Game();
        game.setTitle(title);
        game.setPlatform(Platform.SYSTEM);
        game.setUniqueId(Game.generateUniqueId(Platform.SYSTEM, title));
        game.setInstallPath(installPath);

        // Find executable - try each possible name
        try {
            Path foundExe = null;
            for (String exeName : exeNames) {
                try (var stream = Files.walk(Paths.get(installPath), 4)) {
                    Optional<Path> exe = stream
                            .filter(p -> p.getFileName().toString().equalsIgnoreCase(exeName))
                            .findFirst();
                    if (exe.isPresent()) {
                        foundExe = exe.get();
                        break;
                    }
                }
            }

            if (foundExe != null) {
                game.setExecutablePath(foundExe.toString());
                game.setStatus(Status.READY);
            } else {
                // If no specific exe found, look for any exe in Game folder
                try (var stream = Files.walk(Paths.get(installPath), 4)) {
                    Optional<Path> anyExe = stream
                            .filter(p -> p.toString().toLowerCase().endsWith(".exe"))
                            .filter(p -> !p.toString().toLowerCase().contains("unins"))
                            .filter(p -> !p.toString().toLowerCase().contains("crash"))
                            .filter(p -> !p.toString().toLowerCase().contains("update"))
                            .findFirst();
                    if (anyExe.isPresent()) {
                        game.setExecutablePath(anyExe.get().toString());
                        game.setStatus(Status.READY);
                    } else {
                        game.setStatus(Status.MISSING);
                    }
                }
            }
        } catch (IOException e) {
            game.setStatus(Status.MISSING);
        }

        metadataService.applyMetadata(game);
        return game;
    }

    /**
     * Scans Windows Registry for Riot Client installations.
     */
    private List<Game> scanRiotClientRegistry(Set<String> foundGames) {
        List<Game> games = new ArrayList<>();

        try {
            // Try to find Riot Client installation from registry
            ProcessBuilder pb = new ProcessBuilder(
                    "reg", "query", "HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
                    "/s", "/f", "Riot"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");

                    // Look for InstallLocation containing game info
                    if (line.contains("InstallLocation") && line.contains("REG_SZ")) {
                        String path = extractRegistryValue(line);
                        if (path != null && !path.isEmpty()) {
                            // Check what game this is
                            String pathLower = path.toLowerCase();
                            if (pathLower.contains("league") && !foundGames.contains("league of legends")) {
                                Game lol = createRiotGame("League of Legends", path,
                                        new String[]{"LeagueClient.exe", "League of Legends.exe"});
                                if (lol != null && lol.getStatus() == Status.READY) {
                                    games.add(lol);
                                    foundGames.add("league of legends");
                                }
                            } else if (pathLower.contains("valorant") && !foundGames.contains("valorant")) {
                                Game valorant = createRiotGame("VALORANT", path,
                                        new String[]{"VALORANT.exe", "ShooterGame.exe"});
                                if (valorant != null && valorant.getStatus() == Status.READY) {
                                    games.add(valorant);
                                    foundGames.add("valorant");
                                }
                            }
                        }
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            // Silent fail for registry access
        }

        return games;
    }

    /**
     * Merges scanned games with the database.
     * Updates existing games and adds new ones.
     * Also cleans up any duplicates in the database.
     */
    private List<Game> mergeWithDatabase(List<Game> scannedGames) {
        // First, clean up duplicates in the database
        cleanupDatabaseDuplicates();

        List<Game> result = new ArrayList<>();
        Set<String> processedTitles = new HashSet<>();

        for (Game scanned : scannedGames) {
            try {
                // Skip if we've already processed a game with the same normalized title
                String normalizedTitle = normalizeTitle(scanned.getTitle());
                if (processedTitles.contains(normalizedTitle)) {
                    System.out.println("[ScannerService] Skipping duplicate during merge: " + scanned.getTitle());
                    continue;
                }
                processedTitles.add(normalizedTitle);

                Optional<Game> existing = gameRepository.findByUniqueId(scanned.getUniqueId());

                if (existing.isPresent()) {
                    // Update existing game
                    Game dbGame = existing.get();
                    dbGame.setInstallPath(scanned.getInstallPath());
                    dbGame.setExecutablePath(scanned.getExecutablePath());
                    dbGame.setStatus(scanned.getStatus());
                    // Don't overwrite user data like favorites, cover, etc.
                    if (dbGame.getCoverImageUrl() == null || dbGame.getCoverImageUrl().isEmpty()) {
                        dbGame.setCoverImageUrl(scanned.getCoverImageUrl());
                    }
                    gameRepository.save(dbGame);
                    result.add(dbGame);
                } else {
                    // Check if a game with similar title already exists (to avoid duplicates)
                    Optional<Game> existingByTitle = findGameByNormalizedTitle(normalizedTitle);
                    if (existingByTitle.isPresent()) {
                        // Update existing game with new unique ID
                        Game dbGame = existingByTitle.get();
                        dbGame.setUniqueId(scanned.getUniqueId());
                        dbGame.setInstallPath(scanned.getInstallPath());
                        dbGame.setExecutablePath(scanned.getExecutablePath());
                        dbGame.setStatus(scanned.getStatus());
                        gameRepository.save(dbGame);
                        result.add(dbGame);
                        System.out.println("[ScannerService] Updated existing game by title: " + dbGame.getTitle());
                    } else {
                        // Add new game
                        Game saved = gameRepository.save(scanned);
                        result.add(saved);
                    }
                }
            } catch (Exception e) {
                System.err.println("[ScannerService] Error saving game: " + scanned.getTitle() + " - " + e.getMessage());
            }
        }

        return result;
    }

    /**
     * Finds a game in the database by normalized title.
     */
    private Optional<Game> findGameByNormalizedTitle(String normalizedTitle) {
        List<Game> allGames = gameRepository.findAll();
        for (Game game : allGames) {
            if (normalizeTitle(game.getTitle()).equals(normalizedTitle)) {
                return Optional.of(game);
            }
        }
        return Optional.empty();
    }

    /**
     * Removes duplicate games from the database, keeping only the first occurrence.
     */
    private void cleanupDatabaseDuplicates() {
        try {
            List<Game> allGames = gameRepository.findAll();
            Map<String, Game> uniqueGames = new LinkedHashMap<>();
            List<Game> duplicatesToDelete = new ArrayList<>();

            for (Game game : allGames) {
                String normalizedTitle = normalizeTitle(game.getTitle());
                if (uniqueGames.containsKey(normalizedTitle)) {
                    // This is a duplicate - mark for deletion
                    duplicatesToDelete.add(game);
                    System.out.println("[ScannerService] Found duplicate to remove: " + game.getTitle() + " (ID: " + game.getId() + ")");
                } else {
                    uniqueGames.put(normalizedTitle, game);
                }
            }

            // Delete duplicates
            for (Game dup : duplicatesToDelete) {
                try {
                    gameRepository.delete(dup.getId());
                    System.out.println("[ScannerService] Deleted duplicate: " + dup.getTitle());
                } catch (Exception e) {
                    System.err.println("[ScannerService] Error deleting duplicate: " + e.getMessage());
                }
            }

            if (!duplicatesToDelete.isEmpty()) {
                System.out.println("[ScannerService] Cleaned up " + duplicatesToDelete.size() + " duplicate games from database");
            }
        } catch (Exception e) {
            System.err.println("[ScannerService] Error during duplicate cleanup: " + e.getMessage());
        }
    }

    /**
     * Gets the detected Steam path.
     */
    public String getDetectedSteamPath() {
        return getSteamPath();
    }

    /**
     * Gets the Epic Games manifests path.
     */
    public String getDetectedEpicPath() {
        String programData = System.getenv("ProgramData");
        if (programData == null) {
            programData = "C:\\ProgramData";
        }
        return Paths.get(programData, "Epic", "EpicGamesLauncher", "Data", "Manifests").toString();
    }
}

