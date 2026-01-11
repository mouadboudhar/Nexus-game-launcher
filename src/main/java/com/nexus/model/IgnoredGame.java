package com.nexus.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Represents a game that has been ignored/hidden by the user.
 * Ignored games will not appear in the library and will be skipped during scans.
 */
@Entity
@Table(name = "ignored_games")
public class IgnoredGame implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "normalized_title")
    private String normalizedTitle;

    @Column(name = "install_path")
    private String installPath;

    @Column(name = "unique_id")
    private String uniqueId;

    @Column(name = "ignored_at")
    private LocalDateTime ignoredAt;

    public IgnoredGame() {
        this.ignoredAt = LocalDateTime.now();
    }

    public IgnoredGame(String title, String installPath, String uniqueId) {
        this.title = title;
        this.normalizedTitle = normalizeTitle(title);
        this.installPath = installPath;
        this.uniqueId = uniqueId;
        this.ignoredAt = LocalDateTime.now();
    }

    /**
     * Normalizes a title for consistent matching.
     */
    public static String normalizeTitle(String title) {
        if (title == null) return "";
        return title.toLowerCase().replaceAll("[^a-z0-9]", "").trim();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) {
        this.title = title;
        this.normalizedTitle = normalizeTitle(title);
    }

    public String getNormalizedTitle() { return normalizedTitle; }
    public void setNormalizedTitle(String normalizedTitle) { this.normalizedTitle = normalizedTitle; }

    public String getInstallPath() { return installPath; }
    public void setInstallPath(String installPath) { this.installPath = installPath; }

    public String getUniqueId() { return uniqueId; }
    public void setUniqueId(String uniqueId) { this.uniqueId = uniqueId; }

    public LocalDateTime getIgnoredAt() { return ignoredAt; }
    public void setIgnoredAt(LocalDateTime ignoredAt) { this.ignoredAt = ignoredAt; }

    @Override
    public String toString() {
        return "IgnoredGame{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", installPath='" + installPath + '\'' +
                ", uniqueId='" + uniqueId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IgnoredGame that = (IgnoredGame) o;
        return uniqueId != null && uniqueId.equals(that.uniqueId);
    }

    @Override
    public int hashCode() {
        return uniqueId != null ? uniqueId.hashCode() : 0;
    }
}

