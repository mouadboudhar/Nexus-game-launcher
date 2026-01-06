package com.nexus.client.service;

import com.nexus.model.Game;
import com.nexus.model.Game.Platform;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Combined metadata service that uses:
 * 1. Steam API for Steam games (no key required)
 * 2. IGDB API for non-Steam games (requires Twitch Client ID/Secret)
 *
 * IGDB Setup:
 * 1. Go to https://dev.twitch.tv/console/apps
 * 2. Create an application
 * 3. Get Client ID and Client Secret
 * 4. Create 'nexus.properties' in app directory with:
 *    igdb.client.id=YOUR_CLIENT_ID
 *    igdb.client.secret=YOUR_CLIENT_SECRET
 */
public class CombinedMetadataService implements MetadataService {

    private static final ConcurrentHashMap<String, CachedMetadata> cache = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRY_MS = 24 * 60 * 60 * 1000; // 24 hours

    // Steam endpoints
    private static final String STEAM_APP_DETAILS = "https://store.steampowered.com/api/appdetails?appids=";
    private static final String STEAM_SEARCH = "https://store.steampowered.com/api/storesearch/?term=%s&cc=us&l=en";
    private static final String STEAM_COVER = "https://steamcdn-a.akamaihd.net/steam/apps/%s/library_600x900_2x.jpg";
    private static final String STEAM_HERO = "https://steamcdn-a.akamaihd.net/steam/apps/%s/library_hero.jpg";
    private static final String STEAM_HEADER = "https://steamcdn-a.akamaihd.net/steam/apps/%s/header.jpg";

    // IGDB endpoints
    private static final String IGDB_GAMES = "https://api.igdb.com/v4/games";
    private static final String IGDB_COVERS = "https://api.igdb.com/v4/covers";
    private static final String TWITCH_TOKEN_URL = "https://id.twitch.tv/oauth2/token";

    // IGDB credentials (loaded from properties file)
    private String igdbClientId;
    private String igdbClientSecret;
    private String igdbAccessToken;
    private long tokenExpiry = 0;

    // Fallback service for when APIs fail
    private final PlaceholderMetadataService fallbackService = new PlaceholderMetadataService();

    public CombinedMetadataService() {
        loadIgdbCredentials();
    }

    /**
     * Loads IGDB credentials from nexus.properties file.
     */
    private void loadIgdbCredentials() {
        try {
            Path propsPath = Paths.get("nexus.properties");
            if (Files.exists(propsPath)) {
                Properties props = new Properties();
                try (InputStream is = Files.newInputStream(propsPath)) {
                    props.load(is);
                }
                igdbClientId = props.getProperty("igdb.client.id");
                igdbClientSecret = props.getProperty("igdb.client.secret");

                if (igdbClientId != null && igdbClientSecret != null) {
                    System.out.println("[CombinedMetadataService] IGDB credentials loaded");
                }
            }
        } catch (Exception e) {
            System.err.println("[CombinedMetadataService] Could not load IGDB credentials: " + e.getMessage());
        }
    }

    @Override
    public void applyMetadata(Game game) {
        if (game == null || game.getTitle() == null) return;

        String cacheKey = getCacheKey(game);

        // Check cache first
        CachedMetadata cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            applyFromCache(game, cached);
            return;
        }

        // Try Steam first for Steam games or any game that might be on Steam
        if (game.getPlatform() == Platform.STEAM || game.getAppId() != null) {
            if (applySteamMetadata(game)) {
                cacheResult(game);
                return;
            }
        }

        // Try IGDB for non-Steam games
        if (igdbClientId != null && applyIgdbMetadata(game)) {
            cacheResult(game);
            return;
        }

        // Try Steam search as fallback (game might exist on Steam even if not installed via Steam)
        if (applySteamSearchMetadata(game)) {
            cacheResult(game);
            return;
        }

        // Use hardcoded fallback as last resort
        fallbackService.applyMetadata(game);
        cacheResult(game);
    }

    /**
     * Applies metadata from Steam API using appId.
     */
    private boolean applySteamMetadata(Game game) {
        String appId = game.getAppId();
        if (appId == null || appId.isEmpty()) return false;

        try {
            // Set CDN image URLs
            if (game.getCoverImageUrl() == null || game.getCoverImageUrl().isEmpty()) {
                game.setCoverImageUrl(String.format(STEAM_COVER, appId));
            }
            if (game.getHeroImageUrl() == null || game.getHeroImageUrl().isEmpty()) {
                game.setHeroImageUrl(String.format(STEAM_HERO, appId));
            }

            // Fetch details from Steam Store API
            String response = httpGet(STEAM_APP_DETAILS + appId, null, null);
            if (response != null && response.contains("\"success\":true")) {
                parseSteamDetails(game, response);
                return true;
            }
        } catch (Exception e) {
            System.err.println("[CombinedMetadataService] Steam API error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Searches Steam for a game and applies metadata.
     */
    private boolean applySteamSearchMetadata(Game game) {
        try {
            String encodedTitle = java.net.URLEncoder.encode(game.getTitle(), StandardCharsets.UTF_8);
            String response = httpGet(String.format(STEAM_SEARCH, encodedTitle), null, null);

            if (response != null && response.contains("\"items\"")) {
                String appId = findBestSteamMatch(response, game.getTitle());
                if (appId != null) {
                    game.setAppId(appId);
                    return applySteamMetadata(game);
                }
            }
        } catch (Exception e) {
            System.err.println("[CombinedMetadataService] Steam search error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Applies metadata from IGDB API.
     */
    private boolean applyIgdbMetadata(Game game) {
        if (igdbClientId == null || igdbClientSecret == null) return false;

        try {
            // Get/refresh access token
            if (!ensureIgdbToken()) return false;

            // Search for game
            String query = "search \"" + game.getTitle().replace("\"", "") + "\"; " +
                          "fields name,summary,cover,first_release_date,involved_companies.company.name; " +
                          "limit 5;";

            String response = httpPost(IGDB_GAMES, query, igdbClientId, igdbAccessToken);
            if (response == null || response.equals("[]")) return false;

            // Parse first matching result
            return parseIgdbGame(game, response);

        } catch (Exception e) {
            System.err.println("[CombinedMetadataService] IGDB error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Ensures we have a valid IGDB access token.
     */
    private boolean ensureIgdbToken() {
        if (igdbAccessToken != null && System.currentTimeMillis() < tokenExpiry) {
            return true;
        }

        try {
            String tokenUrl = TWITCH_TOKEN_URL +
                "?client_id=" + igdbClientId +
                "&client_secret=" + igdbClientSecret +
                "&grant_type=client_credentials";

            String response = httpPost(tokenUrl, "", null, null);
            if (response != null && response.contains("access_token")) {
                // Extract token
                Pattern tokenPattern = Pattern.compile("\"access_token\"\\s*:\\s*\"([^\"]+)\"");
                Matcher tokenMatcher = tokenPattern.matcher(response);
                if (tokenMatcher.find()) {
                    igdbAccessToken = tokenMatcher.group(1);
                }

                // Extract expiry
                Pattern expiryPattern = Pattern.compile("\"expires_in\"\\s*:\\s*(\\d+)");
                Matcher expiryMatcher = expiryPattern.matcher(response);
                if (expiryMatcher.find()) {
                    long expiresIn = Long.parseLong(expiryMatcher.group(1));
                    tokenExpiry = System.currentTimeMillis() + (expiresIn * 1000) - 60000; // 1 min buffer
                }

                return igdbAccessToken != null;
            }
        } catch (Exception e) {
            System.err.println("[CombinedMetadataService] Token refresh failed: " + e.getMessage());
        }
        return false;
    }

    /**
     * Parses IGDB game response.
     */
    private boolean parseIgdbGame(Game game, String json) {
        try {
            // Find best matching game by name
            Pattern namePattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
            Pattern summaryPattern = Pattern.compile("\"summary\"\\s*:\\s*\"([^\"]*(?:\\\\.[^\"]*)*)\"");
            Pattern coverPattern = Pattern.compile("\"cover\"\\s*:\\s*(\\d+)");

            Matcher nameMatcher = namePattern.matcher(json);
            Matcher summaryMatcher = summaryPattern.matcher(json);
            Matcher coverMatcher = coverPattern.matcher(json);

            // Apply summary
            if (summaryMatcher.find() && (game.getDescription() == null || game.getDescription().isEmpty())) {
                String summary = summaryMatcher.group(1)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
                game.setDescription(summary);
            }

            // Get cover image
            if (coverMatcher.find()) {
                String coverId = coverMatcher.group(1);
                String coverUrl = fetchIgdbCover(coverId);
                if (coverUrl != null) {
                    if (game.getCoverImageUrl() == null || game.getCoverImageUrl().isEmpty()) {
                        game.setCoverImageUrl(coverUrl);
                    }
                    if (game.getHeroImageUrl() == null || game.getHeroImageUrl().isEmpty()) {
                        // Use larger version for hero
                        game.setHeroImageUrl(coverUrl.replace("t_cover_big", "t_1080p"));
                    }
                }
            }

            // Extract developer from involved_companies
            Pattern devPattern = Pattern.compile("\"company\"\\s*:\\s*\\{[^}]*\"name\"\\s*:\\s*\"([^\"]+)\"");
            Matcher devMatcher = devPattern.matcher(json);
            if (devMatcher.find() && (game.getDeveloper() == null || game.getDeveloper().isEmpty())) {
                game.setDeveloper(devMatcher.group(1));
            }

            return game.getDescription() != null || game.getCoverImageUrl() != null;

        } catch (Exception e) {
            System.err.println("[CombinedMetadataService] IGDB parse error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Fetches cover URL from IGDB.
     */
    private String fetchIgdbCover(String coverId) {
        try {
            String query = "fields url; where id = " + coverId + ";";
            String response = httpPost(IGDB_COVERS, query, igdbClientId, igdbAccessToken);

            Pattern urlPattern = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = urlPattern.matcher(response);
            if (matcher.find()) {
                String url = matcher.group(1);
                // Convert to HTTPS and larger size
                url = url.replace("//", "https://").replace("t_thumb", "t_cover_big");
                return url;
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Parses Steam API details response.
     */
    private void parseSteamDetails(Game game, String json) {
        // Description
        if (game.getDescription() == null || game.getDescription().isEmpty()) {
            String desc = extractJsonString(json, "short_description");
            if (desc != null) {
                desc = desc.replace("&quot;", "\"").replace("&amp;", "&")
                           .replace("&lt;", "<").replace("&gt;", ">").replace("&#39;", "'");
                game.setDescription(desc);
            }
        }

        // Developer
        if (game.getDeveloper() == null || game.getDeveloper().isEmpty()) {
            String dev = extractJsonArrayFirst(json, "developers");
            if (dev != null) game.setDeveloper(dev);
        }

        // Release date
        if (game.getReleaseDate() == null || game.getReleaseDate().isEmpty()) {
            Pattern releaseDatePattern = Pattern.compile("\"release_date\"\\s*:\\s*\\{[^}]*\"date\"\\s*:\\s*\"([^\"]+)\"");
            Matcher matcher = releaseDatePattern.matcher(json);
            if (matcher.find()) {
                game.setReleaseDate(matcher.group(1));
            }
        }
    }

    /**
     * Finds best matching Steam appId from search results.
     */
    private String findBestSteamMatch(String json, String title) {
        Pattern itemPattern = Pattern.compile("\"id\":(\\d+),\"type\":\"app\",\"name\":\"([^\"]+)\"");
        Matcher matcher = itemPattern.matcher(json);

        String titleLower = title.toLowerCase();
        String bestId = null;
        int bestScore = 0;

        while (matcher.find()) {
            String id = matcher.group(1);
            String name = matcher.group(2).toLowerCase();

            int score = 0;
            if (name.equals(titleLower)) score = 100;
            else if (name.contains(titleLower)) score = 50 + titleLower.length();
            else if (titleLower.contains(name)) score = 40 + name.length();

            if (score > bestScore) {
                bestScore = score;
                bestId = id;
            }
        }

        return bestId;
    }

    private String extractJsonString(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]*(?:\\\\.[^\"]*)*)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1).replace("\\\"", "\"") : null;
    }

    private String extractJsonArrayFirst(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\"\\s*:\\s*\\[\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String httpGet(String urlString, String clientId, String token) {
        return httpRequest(urlString, "GET", null, clientId, token);
    }

    private String httpPost(String urlString, String body, String clientId, String token) {
        return httpRequest(urlString, "POST", body, clientId, token);
    }

    private String httpRequest(String urlString, String method, String body, String clientId, String token) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "NexusLauncher/1.0");
            conn.setRequestProperty("Accept", "application/json");

            if (clientId != null) {
                conn.setRequestProperty("Client-ID", clientId);
            }
            if (token != null) {
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }

            if ("POST".equals(method) && body != null) {
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    return response.toString();
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private String getCacheKey(Game game) {
        if (game.getAppId() != null && !game.getAppId().isEmpty()) {
            return "steam_" + game.getAppId();
        }
        return "title_" + game.getTitle().toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private void cacheResult(Game game) {
        cache.put(getCacheKey(game), new CachedMetadata(
            game.getCoverImageUrl(), game.getHeroImageUrl(),
            game.getDescription(), game.getDeveloper(), game.getReleaseDate()
        ));
    }

    private void applyFromCache(Game game, CachedMetadata cached) {
        if (game.getCoverImageUrl() == null || game.getCoverImageUrl().isEmpty())
            game.setCoverImageUrl(cached.coverUrl);
        if (game.getHeroImageUrl() == null || game.getHeroImageUrl().isEmpty())
            game.setHeroImageUrl(cached.heroUrl);
        if (game.getDescription() == null || game.getDescription().isEmpty())
            game.setDescription(cached.description);
        if (game.getDeveloper() == null || game.getDeveloper().isEmpty())
            game.setDeveloper(cached.developer);
        if (game.getReleaseDate() == null || game.getReleaseDate().isEmpty())
            game.setReleaseDate(cached.releaseDate);
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

    public static void clearCache() {
        cache.clear();
    }

    private static class CachedMetadata {
        final String coverUrl, heroUrl, description, developer, releaseDate;
        final long timestamp;

        CachedMetadata(String coverUrl, String heroUrl, String description, String developer, String releaseDate) {
            this.coverUrl = coverUrl;
            this.heroUrl = heroUrl;
            this.description = description;
            this.developer = developer;
            this.releaseDate = releaseDate;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS;
        }
    }
}

