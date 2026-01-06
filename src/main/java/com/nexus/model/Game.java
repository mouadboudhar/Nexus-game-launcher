package com.nexus.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents a game in the Nexus Launcher library.
 */
@Entity
@Table(name = "games")
public class Game implements Serializable {

    public enum Platform {
        STEAM("Steam", "#1b2838"),
        EPIC("Epic", "#333333"),
        SYSTEM("System", "#6b21a8"),
        MANUAL("Manual", "#4f46e5");

        private final String displayName;
        private final String color;

        Platform(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() { return displayName; }
        public String getColor() { return color; }
    }

    public enum Status {
        READY("Ready", "#22c55e"),
        MISSING("Missing", "#ef4444"),
        UPDATING("Updating", "#f59e0b");

        private final String displayName;
        private final String color;

        Status(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() { return displayName; }
        public String getColor() { return color; }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    private String developer;
    private String releaseDate;
    private String executablePath;
    private String coverImageUrl;
    private String heroImageUrl;

    @Column(name = "app_id")
    private String appId;

    @Column(name = "install_path")
    private String installPath;

    @Enumerated(EnumType.STRING)
    private Platform platform;

    @Enumerated(EnumType.STRING)
    private Status status;

    private boolean favorite;

    @Column(name = "last_played")
    private LocalDateTime lastPlayed;

    @Column(name = "total_play_time")
    private long totalPlayTime;

    @Column(name = "unique_id", unique = true)
    private String uniqueId;

    @Column(name = "icon_path")
    private String iconPath;

    public Game() {}

    public Game(Long id, String title, String coverImageUrl, Platform platform, Status status) {
        this.id = id;
        this.title = title;
        this.coverImageUrl = coverImageUrl;
        this.platform = platform;
        this.status = status;
        this.favorite = false;
    }

    public static String generateUniqueId(Platform platform, String identifier) {
        return platform.name().toLowerCase() + "_" + identifier.toLowerCase().replaceAll("[^a-z0-9]", "_");
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDeveloper() { return developer; }
    public void setDeveloper(String developer) { this.developer = developer; }

    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }

    public String getExecutablePath() { return executablePath; }
    public void setExecutablePath(String executablePath) { this.executablePath = executablePath; }

    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }

    public String getHeroImageUrl() { return heroImageUrl; }
    public void setHeroImageUrl(String heroImageUrl) { this.heroImageUrl = heroImageUrl; }

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }

    public String getInstallPath() { return installPath; }
    public void setInstallPath(String installPath) { this.installPath = installPath; }

    public Platform getPlatform() { return platform; }
    public void setPlatform(Platform platform) { this.platform = platform; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }

    public LocalDateTime getLastPlayed() { return lastPlayed; }
    public void setLastPlayed(LocalDateTime lastPlayed) { this.lastPlayed = lastPlayed; }

    public long getTotalPlayTime() { return totalPlayTime; }
    public void setTotalPlayTime(long totalPlayTime) { this.totalPlayTime = totalPlayTime; }

    public String getUniqueId() { return uniqueId; }
    public void setUniqueId(String uniqueId) { this.uniqueId = uniqueId; }

    public String getIconPath() { return iconPath; }
    public void setIconPath(String iconPath) { this.iconPath = iconPath; }

    @Override
    public String toString() {
        return "Game{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", platform=" + platform +
                ", status=" + status +
                ", appId='" + appId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Game game = (Game) o;
        return uniqueId != null && uniqueId.equals(game.uniqueId);
    }

    @Override
    public int hashCode() {
        return uniqueId != null ? uniqueId.hashCode() : 0;
    }
}

