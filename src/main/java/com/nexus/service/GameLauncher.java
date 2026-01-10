package com.nexus.service;

import com.nexus.model.Game;
import com.nexus.model.Game.Platform;
import com.nexus.model.Game.Status;
import com.nexus.repository.GameRepository;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for launching games.
 * Handles Steam (via protocol), Epic (via protocol or direct), and System games.
 */
public class GameLauncher {

    private final GameRepository gameRepository;

    public GameLauncher() {
        this.gameRepository = new GameRepository();
    }

    public GameLauncher(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    /**
     * Launches a game based on its platform.
     */
    public boolean play(Game game) throws GameLaunchException {
        if (game == null) {
            throw new GameLaunchException("Game cannot be null");
        }

        if (game.getStatus() == Status.MISSING) {
            throw new GameLaunchException("Game executable is missing: " + game.getTitle());
        }

        System.out.println("[GameLauncher] Launching: " + game.getTitle() + " (" + game.getPlatform() + ")");

        boolean success;
        try {
            switch (game.getPlatform()) {
                case STEAM:
                    success = launchSteamGame(game);
                    break;
                case EPIC:
                    success = launchEpicGame(game);
                    break;
                case SYSTEM:
                    success = launchSystemGame(game);
                    break;
                default:
                    success = launchDirectGame(game);
            }

            if (success) {
                updateLastPlayed(game);
            }

            return success;
        } catch (Exception e) {
            throw new GameLaunchException("Failed to launch " + game.getTitle() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Launches a Steam game using the steam:// protocol.
     */
    private boolean launchSteamGame(Game game) throws IOException, InterruptedException {
        String appId = game.getAppId();
        if (appId == null || appId.isEmpty()) {
            System.out.println("[GameLauncher] No Steam AppID, falling back to direct launch");
            return launchDirectGame(game);
        }

        String steamUrl = "steam://run/" + appId;
        System.out.println("[GameLauncher] Launching via Steam protocol: " + steamUrl);

        // Use cmd /c start to open the steam:// URL - more reliable than rundll32
        ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start", "", steamUrl);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        Thread.sleep(500);
        System.out.println("[GameLauncher] Steam launch initiated for: " + game.getTitle());
        return true;
    }

    /**
     * Launches an Epic game - tries protocol first, then direct.
     */
    private boolean launchEpicGame(Game game) throws IOException, InterruptedException {
        String appId = game.getAppId();

        // Try Epic protocol first
        if (appId != null && !appId.isEmpty()) {
            String epicUrl = "com.epicgames.launcher://apps/" + appId + "?action=launch&silent=true";
            System.out.println("[GameLauncher] Trying Epic protocol: " + epicUrl);

            try {
                ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start", "", epicUrl);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                Thread.sleep(500);
                System.out.println("[GameLauncher] Epic launch initiated for: " + game.getTitle());
                return true;
            } catch (Exception e) {
                System.out.println("[GameLauncher] Epic protocol failed, trying direct launch");
            }
        }

        return launchDirectGame(game);
    }

    /**
     * Launches a system game with multiple fallback methods.
     */
    private boolean launchSystemGame(Game game) throws IOException, InterruptedException {
        String title = game.getTitle().toLowerCase();

        // Special handling for Riot Games (League, VALORANT, etc.)
        if (title.contains("league") || title.contains("valorant") || title.contains("runeterra")) {
            return launchRiotGame(game);
        }

        // Try direct launch first, with fallbacks
        return launchDirectGameWithFallbacks(game);
    }

    /**
     * Special launcher for Riot Games that uses their client.
     */
    private boolean launchRiotGame(Game game) throws IOException, InterruptedException {
        String title = game.getTitle().toLowerCase();

        // Try to launch via Riot Client
        String[] riotClientPaths = {
            "C:\\Riot Games\\Riot Client\\RiotClientServices.exe",
            System.getenv("ProgramFiles") + "\\Riot Games\\Riot Client\\RiotClientServices.exe",
            System.getenv("ProgramFiles(x86)") + "\\Riot Games\\Riot Client\\RiotClientServices.exe",
            System.getenv("LOCALAPPDATA") + "\\Riot Games\\Riot Client\\RiotClientServices.exe"
        };

        String riotClientPath = null;
        for (String path : riotClientPaths) {
            if (path != null && new File(path).exists()) {
                riotClientPath = path;
                break;
            }
        }

        if (riotClientPath != null) {
            String productArg = "";
            if (title.contains("league")) {
                productArg = "league_of_legends";
            } else if (title.contains("valorant")) {
                productArg = "valorant";
            } else if (title.contains("runeterra")) {
                productArg = "bacon"; // Riot's internal code for LoR
            }

            if (!productArg.isEmpty()) {
                System.out.println("[GameLauncher] Launching via Riot Client: " + productArg);
                ProcessBuilder pb = new ProcessBuilder(
                    riotClientPath,
                    "--launch-product=" + productArg,
                    "--launch-patchline=live"
                );
                pb.directory(new File(riotClientPath).getParentFile());
                pb.redirectErrorStream(true);
                pb.start();
                System.out.println("[GameLauncher] Riot Client launch initiated for: " + game.getTitle());
                return true;
            }
        }

        // Fallback to direct launch
        return launchDirectGameWithFallbacks(game);
    }

    /**
     * Launches a game directly with multiple fallback methods to handle access issues.
     */
    private boolean launchDirectGameWithFallbacks(Game game) throws IOException, InterruptedException {
        String exePath = game.getExecutablePath();

        if (exePath == null || exePath.isEmpty()) {
            throw new IOException("No executable path set for: " + game.getTitle());
        }

        File exeFile = new File(exePath);
        if (!exeFile.exists()) {
            throw new IOException("Executable not found: " + exePath);
        }

        File workingDir = exeFile.getParentFile();
        System.out.println("[GameLauncher] Launching: " + exePath);
        System.out.println("[GameLauncher] Working directory: " + workingDir.getAbsolutePath());

        // Method 1: Try using cmd /c start (most compatible)
        try {
            System.out.println("[GameLauncher] Method 1: Using cmd /c start");
            ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start", "", "/D",
                workingDir.getAbsolutePath(), exeFile.getName());
            pb.directory(workingDir);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            Thread.sleep(500);
            System.out.println("[GameLauncher] Launch successful via cmd /c start");
            return true;
        } catch (Exception e) {
            System.out.println("[GameLauncher] Method 1 failed: " + e.getMessage());
        }

        // Method 2: Try explorer.exe to open the file
        try {
            System.out.println("[GameLauncher] Method 2: Using explorer.exe");
            ProcessBuilder pb = new ProcessBuilder("explorer.exe", exePath);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            Thread.sleep(500);
            System.out.println("[GameLauncher] Launch successful via explorer.exe");
            return true;
        } catch (Exception e) {
            System.out.println("[GameLauncher] Method 2 failed: " + e.getMessage());
        }

        // Method 3: Try PowerShell Start-Process
        try {
            System.out.println("[GameLauncher] Method 3: Using PowerShell");
            ProcessBuilder pb = new ProcessBuilder("powershell", "-Command",
                "Start-Process", "-FilePath", "'" + exePath + "'",
                "-WorkingDirectory", "'" + workingDir.getAbsolutePath() + "'");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            Thread.sleep(500);
            System.out.println("[GameLauncher] Launch successful via PowerShell");
            return true;
        } catch (Exception e) {
            System.out.println("[GameLauncher] Method 3 failed: " + e.getMessage());
        }

        // Method 4: Direct ProcessBuilder (original method - might fail with access denied)
        try {
            System.out.println("[GameLauncher] Method 4: Direct ProcessBuilder");
            ProcessBuilder pb = new ProcessBuilder(exePath);
            pb.directory(workingDir);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            System.out.println("[GameLauncher] Launch successful via direct ProcessBuilder");
            return true;
        } catch (Exception e) {
            System.out.println("[GameLauncher] Method 4 failed: " + e.getMessage());
            throw new IOException("All launch methods failed for: " + game.getTitle() + ". Last error: " + e.getMessage());
        }
    }

    /**
     * Direct game launch (basic method).
     */
    private boolean launchDirectGame(Game game) throws IOException {
        String exePath = game.getExecutablePath();

        if (exePath == null || exePath.isEmpty()) {
            throw new IOException("No executable path set for: " + game.getTitle());
        }

        File exeFile = new File(exePath);
        if (!exeFile.exists()) {
            throw new IOException("Executable not found: " + exePath);
        }

        File workingDir = exeFile.getParentFile();

        // Use cmd /c start for better compatibility
        ProcessBuilder pb = new ProcessBuilder("cmd", "/c", "start", "", "/D",
            workingDir.getAbsolutePath(), exeFile.getName());
        pb.directory(workingDir);
        pb.redirectErrorStream(true);
        pb.start();

        System.out.println("[GameLauncher] Process started for: " + game.getTitle());
        return true;
    }

    private void updateLastPlayed(Game game) {
        try {
            game.setLastPlayed(LocalDateTime.now());
            gameRepository.save(game);
        } catch (Exception e) {
            System.err.println("[GameLauncher] Failed to update lastPlayed: " + e.getMessage());
        }
    }

    public boolean canLaunch(Game game) {
        if (game == null || game.getStatus() == Status.MISSING) {
            return false;
        }

        if (game.getPlatform() == Platform.STEAM) {
            return game.getAppId() != null && !game.getAppId().isEmpty();
        }

        String exePath = game.getExecutablePath();
        return exePath != null && !exePath.isEmpty() && new File(exePath).exists();
    }

    public void openGameFolder(Game game) throws IOException {
        String path = game.getInstallPath();
        if (path == null || path.isEmpty()) {
            path = game.getExecutablePath();
            if (path != null) {
                path = new File(path).getParent();
            }
        }

        if (path == null || !new File(path).exists()) {
            throw new IOException("Game folder not found");
        }

        ProcessBuilder pb = new ProcessBuilder("explorer.exe", path);
        pb.start();
    }

    public static class GameLaunchException extends Exception {
        public GameLaunchException(String message) {
            super(message);
        }

        public GameLaunchException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

