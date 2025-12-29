package com.nexus.client.service;

import com.nexus.shared.model.Game;
import com.nexus.shared.model.Game.Platform;
import com.nexus.shared.model.Game.Status;
import com.nexus.shared.repository.GameRepository;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Service responsible for launching games.
 * Handles Steam (via protocol), Epic (direct exe), and System games.
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
     *
     * @param game The game to launch
     * @return true if launch was successful, false otherwise
     * @throws GameLaunchException if launch fails
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
            if (game.getPlatform() == Platform.STEAM) {
                success = launchSteamGame(game);
            } else {
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

        // Use rundll32 to open the steam:// URL
        ProcessBuilder pb = new ProcessBuilder(
                "rundll32", "url.dll,FileProtocolHandler", steamUrl
        );

        Process process = pb.start();

        // Don't wait for the process - Steam will handle it
        // Just give it a moment to start
        Thread.sleep(500);

        System.out.println("[GameLauncher] Steam launch initiated for: " + game.getTitle());
        return true;
    }

    /**
     * Launches a game directly via its executable.
     * IMPORTANT: Sets the working directory to the game's folder.
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

        // CRUCIAL: Set the working directory to the game's folder
        // Many games crash if this is not set correctly
        File workingDir = exeFile.getParentFile();

        System.out.println("[GameLauncher] Launching: " + exePath);
        System.out.println("[GameLauncher] Working directory: " + workingDir.getAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(exePath);
        pb.directory(workingDir);

        // Don't inherit IO - let the game run independently
        pb.redirectErrorStream(true);

        // Start the process
        Process process = pb.start();

        // Don't wait for the process to complete - games are long-running
        System.out.println("[GameLauncher] Process started for: " + game.getTitle());
        return true;
    }

    /**
     * Updates the lastPlayed timestamp for a game.
     */
    private void updateLastPlayed(Game game) {
        try {
            game.setLastPlayed(LocalDateTime.now());
            gameRepository.save(game);
            System.out.println("[GameLauncher] Updated lastPlayed for: " + game.getTitle());
        } catch (Exception e) {
            System.err.println("[GameLauncher] Failed to update lastPlayed: " + e.getMessage());
        }
    }

    /**
     * Checks if a game can be launched.
     */
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

    /**
     * Opens the folder containing the game.
     */
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

        // Open in Windows Explorer
        Runtime.getRuntime().exec("explorer.exe \"" + path + "\"");
    }

    /**
     * Exception class for game launch errors.
     */
    public static class GameLaunchException extends Exception {
        public GameLaunchException(String message) {
            super(message);
        }

        public GameLaunchException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

