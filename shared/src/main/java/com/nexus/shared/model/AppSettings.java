package com.nexus.shared.model;

import jakarta.persistence.*;
import java.io.Serializable;

/**
 * Represents application settings for the Nexus Launcher.
 */
@Entity
@Table(name = "app_settings")
public class AppSettings implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private boolean launchOnStartup;
    private boolean closeToTray;
    private boolean darkMode = true;
    private String steamLibraryPath;
    private String epicGamesPath;

    public AppSettings() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public boolean isLaunchOnStartup() { return launchOnStartup; }
    public void setLaunchOnStartup(boolean launchOnStartup) { this.launchOnStartup = launchOnStartup; }

    public boolean isCloseToTray() { return closeToTray; }
    public void setCloseToTray(boolean closeToTray) { this.closeToTray = closeToTray; }

    public boolean isDarkMode() { return darkMode; }
    public void setDarkMode(boolean darkMode) { this.darkMode = darkMode; }

    public String getSteamLibraryPath() { return steamLibraryPath; }
    public void setSteamLibraryPath(String steamLibraryPath) { this.steamLibraryPath = steamLibraryPath; }

    public String getEpicGamesPath() { return epicGamesPath; }
    public void setEpicGamesPath(String epicGamesPath) { this.epicGamesPath = epicGamesPath; }

    @Override
    public String toString() {
        return "AppSettings{" +
                "id=" + id +
                ", launchOnStartup=" + launchOnStartup +
                ", closeToTray=" + closeToTray +
                ", darkMode=" + darkMode +
                '}';
    }
}
