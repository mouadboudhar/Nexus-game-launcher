package com.nexus.service;

import com.nexus.model.Game;
import com.nexus.model.Game.Platform;

import java.io.*;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Metadata service using Steam API and IGDB API.
 * Uses direct IGDB ID lookups for accuracy instead of fuzzy name matching.
 */
public class CombinedMetadataService implements MetadataService {

    // Known IGDB game IDs for popular games - no search needed, direct lookup
    private static final Map<String, Integer> KNOWN_IGDB_IDS = new HashMap<>();
    static {
        // Riot Games
        KNOWN_IGDB_IDS.put("league of legends", 115);
        KNOWN_IGDB_IDS.put("valorant", 126459);
        KNOWN_IGDB_IDS.put("legends of runeterra", 119171);
        KNOWN_IGDB_IDS.put("teamfight tactics", 120227);

        // Mojang
        KNOWN_IGDB_IDS.put("minecraft", 121);
        KNOWN_IGDB_IDS.put("minecraft dungeons", 113520);
        KNOWN_IGDB_IDS.put("minecraft legends", 204642);

        // miHoYo/HoYoverse
        KNOWN_IGDB_IDS.put("genshin impact", 119277);
        KNOWN_IGDB_IDS.put("honkai: star rail", 171536);
        KNOWN_IGDB_IDS.put("honkai impact 3rd", 37582);
        KNOWN_IGDB_IDS.put("zenless zone zero", 217590);

        // Other popular games
        KNOWN_IGDB_IDS.put("fortnite", 1905);
        KNOWN_IGDB_IDS.put("roblox", 17767);
        KNOWN_IGDB_IDS.put("osu!", 3510);
        KNOWN_IGDB_IDS.put("counter-strike 2", 194078);
        KNOWN_IGDB_IDS.put("dota 2", 126459);
        KNOWN_IGDB_IDS.put("apex legends", 114455);
        KNOWN_IGDB_IDS.put("overwatch 2", 152589);
        KNOWN_IGDB_IDS.put("world of warcraft", 123);
        KNOWN_IGDB_IDS.put("diablo iv", 121971);
        KNOWN_IGDB_IDS.put("path of exile", 5);
        KNOWN_IGDB_IDS.put("path of exile 2", 119388);
        KNOWN_IGDB_IDS.put("warframe", 2357);
        KNOWN_IGDB_IDS.put("destiny 2", 25657);
        KNOWN_IGDB_IDS.put("final fantasy xiv", 393);
        KNOWN_IGDB_IDS.put("the sims 4", 5765);
        KNOWN_IGDB_IDS.put("ea sports fc 24", 252370);
        KNOWN_IGDB_IDS.put("ea sports fc 25", 280882);
        KNOWN_IGDB_IDS.put("rocket league", 9540);
        KNOWN_IGDB_IDS.put("fall guys", 119324);
        KNOWN_IGDB_IDS.put("among us", 68452);
        KNOWN_IGDB_IDS.put("dead by daylight", 14913);
        KNOWN_IGDB_IDS.put("pubg: battlegrounds", 22509);
        KNOWN_IGDB_IDS.put("grand theft auto v", 1020);
        KNOWN_IGDB_IDS.put("red dead redemption 2", 25076);
        KNOWN_IGDB_IDS.put("cyberpunk 2077", 1877);
        KNOWN_IGDB_IDS.put("elden ring", 119133);
        KNOWN_IGDB_IDS.put("dark souls iii", 11133);
        KNOWN_IGDB_IDS.put("sekiro: shadows die twice", 38050);
        KNOWN_IGDB_IDS.put("baldur's gate 3", 119171);
        KNOWN_IGDB_IDS.put("the witcher 3", 1942);
        KNOWN_IGDB_IDS.put("hogwarts legacy", 119304);
        KNOWN_IGDB_IDS.put("palworld", 217589);
        KNOWN_IGDB_IDS.put("lethal company", 238091);
    }

    private static final ConcurrentHashMap<String, CachedMetadata> cache = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRY_MS = 7 * 24 * 60 * 60 * 1000; // 7 days

    // Steam endpoints
    private static final String STEAM_APP_DETAILS = "https://store.steampowered.com/api/appdetails?appids=";
    private static final String STEAM_COVER = "https://steamcdn-a.akamaihd.net/steam/apps/%s/library_600x900_2x.jpg";
    private static final String STEAM_HERO = "https://steamcdn-a.akamaihd.net/steam/apps/%s/library_hero.jpg";

    // IGDB endpoints
    private static final String IGDB_GAMES = "https://api.igdb.com/v4/games";
    private static final String IGDB_COVERS = "https://api.igdb.com/v4/covers";
    private static final String TWITCH_TOKEN_URL = "https://id.twitch.tv/oauth2/token";

    private String igdbClientId;
    private String igdbClientSecret;
    private String igdbAccessToken;
    private long tokenExpiry = 0;

    private final PlaceholderMetadataService fallbackService = new PlaceholderMetadataService();

    public CombinedMetadataService() {
        loadIgdbCredentials();
    }

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

                if (igdbClientId != null && !igdbClientId.startsWith("YOUR_") && igdbClientSecret != null) {
                    System.out.println("[CombinedMetadataService] IGDB credentials loaded");
                } else {
                    igdbClientId = null;
                    igdbClientSecret = null;
                }
            }
        } catch (Exception e) {
            System.err.println("[CombinedMetadataService] Could not load IGDB credentials: " + e.getMessage());
        }
    }

    public boolean hasIgdbCredentials() {
        return igdbClientId != null && !igdbClientId.isEmpty()
            && igdbClientSecret != null && !igdbClientSecret.isEmpty();
    }

    @Override
    public void applyMetadata(Game game) {
        if (game == null || game.getTitle() == null) return;

        String cacheKey = getCacheKey(game);
        CachedMetadata cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            applyFromCache(game, cached);
            return;
        }

        // For Steam games, use Steam API directly (most reliable)
        if (game.getPlatform() == Platform.STEAM && game.getAppId() != null) {
            if (applySteamMetadata(game)) {
                cacheResult(game);
                return;
            }
        }

        // Try IGDB with direct ID lookup
        if (hasIgdbCredentials()) {
            if (applyIgdbMetadataById(game)) {
                cacheResult(game);
                return;
            }
        }

        // Fallback to hardcoded data
        fallbackService.applyMetadata(game);
        cacheResult(game);
    }

    /**
     * Applies IGDB metadata using direct ID lookup.
     * First checks known IDs, then does a precise search if needed.
     */
    private boolean applyIgdbMetadataById(Game game) {
        if (!ensureIgdbToken()) return false;

        String titleLower = game.getTitle().toLowerCase().trim();

        // Check if we have a known IGDB ID for this game
        Integer knownId = KNOWN_IGDB_IDS.get(titleLower);

        // Also check partial matches for known games
        if (knownId == null) {
            for (Map.Entry<String, Integer> entry : KNOWN_IGDB_IDS.entrySet()) {
                if (titleLower.contains(entry.getKey()) || entry.getKey().contains(titleLower)) {
                    knownId = entry.getValue();
                    System.out.println("[IGDB] Matched '" + game.getTitle() + "' to known game ID: " + knownId);
                    break;
                }
            }
        }

        if (knownId != null) {
            // Direct lookup by ID - fast and accurate
            System.out.println("[IGDB] Using known ID " + knownId + " for: " + game.getTitle());
            return fetchMetadataByIgdbId(game, knownId);
        }

        // If not a known game, do a precise search to find the ID first
        Integer foundId = searchIgdbForGameId(game.getTitle());
        if (foundId != null) {
            System.out.println("[IGDB] Found ID " + foundId + " for: " + game.getTitle());
            return fetchMetadataByIgdbId(game, foundId);
        }

        System.out.println("[IGDB] No match found for: " + game.getTitle());
        return false;
    }

    /**
     * Searches IGDB to find the game ID. Returns only if there's a confident match.
     */
    private Integer searchIgdbForGameId(String title) {
        try {
            // Clean up the title for better search results
            String cleanTitle = title
                .replaceAll("\\s*[:\\-–]\\s*(Standard|Deluxe|Ultimate|Game of the Year|GOTY|Edition|Remastered|Definitive).*$", "")
                .replaceAll("\\s+", " ")
                .trim();

            // IGDB search query - get only the most relevant result
            String query = String.format(
                "search \"%s\"; fields id,name; where category = (0,8,9,10,11); limit 1;",
                cleanTitle.replace("\"", "")
            );

            String response = igdbPost(IGDB_GAMES, query);
            if (response == null || response.equals("[]")) return null;

            // Extract the ID from the first (and only) result
            Pattern idPattern = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");
            Pattern namePattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");

            Matcher idMatcher = idPattern.matcher(response);
            Matcher nameMatcher = namePattern.matcher(response);

            if (idMatcher.find() && nameMatcher.find()) {
                int id = Integer.parseInt(idMatcher.group(1));
                String foundName = nameMatcher.group(1);

                // Verify the match is reasonable (name similarity check)
                String cleanFoundName = foundName.toLowerCase().replaceAll("[^a-z0-9]", "");
                String cleanSearchName = cleanTitle.toLowerCase().replaceAll("[^a-z0-9]", "");

                // Accept if names are similar enough
                if (cleanFoundName.contains(cleanSearchName) || cleanSearchName.contains(cleanFoundName) ||
                    calculateSimilarity(cleanFoundName, cleanSearchName) > 0.7) {
                    System.out.println("[IGDB] Search matched '" + title + "' -> '" + foundName + "' (ID: " + id + ")");
                    return id;
                } else {
                    System.out.println("[IGDB] Search result '" + foundName + "' doesn't match '" + title + "' well enough");
                }
            }
        } catch (Exception e) {
            System.err.println("[IGDB] Search error: " + e.getMessage());
        }
        return null;
    }

    /**
     * Fetches complete metadata from IGDB using the game ID.
     */
    private boolean fetchMetadataByIgdbId(Game game, int igdbId) {
        try {
            // Fetch full game details by ID
            String query = String.format(
                "fields name,summary,cover.url,involved_companies.company.name,involved_companies.developer,first_release_date; where id = %d;",
                igdbId
            );

            String response = igdbPost(IGDB_GAMES, query);
            if (response == null || response.equals("[]")) return false;

            // Parse summary
            Pattern summaryPattern = Pattern.compile("\"summary\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
            Matcher summaryMatcher = summaryPattern.matcher(response);
            if (summaryMatcher.find()) {
                String summary = summaryMatcher.group(1)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
                game.setDescription(summary);
            }

            // Parse cover URL (nested object)
            Pattern coverUrlPattern = Pattern.compile("\"cover\"\\s*:\\s*\\{[^}]*\"url\"\\s*:\\s*\"([^\"]+)\"");
            Matcher coverMatcher = coverUrlPattern.matcher(response);
            if (coverMatcher.find()) {
                String coverUrl = coverMatcher.group(1)
                    .replace("t_thumb", "t_cover_big")
                    .replace("//", "https://");
                game.setCoverImageUrl(coverUrl);
                game.setHeroImageUrl(coverUrl.replace("t_cover_big", "t_1080p"));
                System.out.println("[IGDB] Set cover for " + game.getTitle() + ": " + coverUrl);
            }

            // Parse developer from involved_companies
            Pattern devPattern = Pattern.compile("\"involved_companies\"\\s*:\\s*\\[\\s*\\{[^}]*\"developer\"\\s*:\\s*true[^}]*\"company\"\\s*:\\s*\\{[^}]*\"name\"\\s*:\\s*\"([^\"]+)\"");
            Matcher devMatcher = devPattern.matcher(response);
            if (devMatcher.find()) {
                game.setDeveloper(devMatcher.group(1));
            } else {
                // Try simpler pattern
                Pattern simpleDevPattern = Pattern.compile("\"company\"\\s*:\\s*\\{[^}]*\"name\"\\s*:\\s*\"([^\"]+)\"");
                Matcher simpleDevMatcher = simpleDevPattern.matcher(response);
                if (simpleDevMatcher.find()) {
                    game.setDeveloper(simpleDevMatcher.group(1));
                }
            }

            return game.getCoverImageUrl() != null || game.getDescription() != null;

        } catch (Exception e) {
            System.err.println("[IGDB] Fetch error for ID " + igdbId + ": " + e.getMessage());
        }
        return false;
    }

    /**
     * Applies Steam metadata for Steam games.
     */
    private boolean applySteamMetadata(Game game) {
        String appId = game.getAppId();
        if (appId == null || appId.isEmpty()) return false;

        try {
            // Set Steam CDN URLs directly (always work for valid appIds)
            game.setCoverImageUrl(String.format(STEAM_COVER, appId));
            game.setHeroImageUrl(String.format(STEAM_HERO, appId));

            // Fetch additional details from Steam API
            String response = httpGet(STEAM_APP_DETAILS + appId);
            if (response != null && response.contains("\"success\":true")) {
                // Parse description
                Pattern descPattern = Pattern.compile("\"short_description\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
                Matcher descMatcher = descPattern.matcher(response);
                if (descMatcher.find()) {
                    game.setDescription(descMatcher.group(1)
                        .replace("\\n", "\n")
                        .replace("\\\"", "\""));
                }

                // Parse developer
                Pattern devPattern = Pattern.compile("\"developers\"\\s*:\\s*\\[\\s*\"([^\"]+)\"");
                Matcher devMatcher = devPattern.matcher(response);
                if (devMatcher.find()) {
                    game.setDeveloper(devMatcher.group(1));
                }
            }

            return true;
        } catch (Exception e) {
            System.err.println("[Steam] Error fetching metadata: " + e.getMessage());
        }
        return false;
    }

    /**
     * Validates if an application is a game using IGDB.
     */
    public GameValidationResult validateAsGame(String appName) {
        if (appName == null || appName.isEmpty()) {
            return GameValidationResult.notAGame();
        }

        String titleLower = appName.toLowerCase().trim();

        // Check known games first
        Integer knownId = KNOWN_IGDB_IDS.get(titleLower);
        if (knownId == null) {
            for (Map.Entry<String, Integer> entry : KNOWN_IGDB_IDS.entrySet()) {
                if (titleLower.contains(entry.getKey()) || entry.getKey().contains(titleLower)) {
                    knownId = entry.getValue();
                    break;
                }
            }
        }

        if (knownId != null) {
            // Fetch metadata using the known ID
            return fetchValidationResultById(knownId, appName);
        }

        // Search IGDB for the game ID
        if (!hasIgdbCredentials() || !ensureIgdbToken()) {
            return GameValidationResult.notAGame();
        }

        Integer foundId = searchIgdbForGameId(appName);
        if (foundId != null) {
            return fetchValidationResultById(foundId, appName);
        }

        return GameValidationResult.notAGame();
    }

    private GameValidationResult fetchValidationResultById(int igdbId, String originalName) {
        try {
            if (!ensureIgdbToken()) return GameValidationResult.notAGame();

            String query = String.format(
                "fields name,summary,cover.url,involved_companies.company.name; where id = %d;",
                igdbId
            );

            String response = igdbPost(IGDB_GAMES, query);
            if (response == null || response.equals("[]")) {
                return GameValidationResult.notAGame();
            }

            String name = null, summary = null, coverUrl = null, developer = null;

            // Parse name
            Pattern namePattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
            Matcher nameMatcher = namePattern.matcher(response);
            if (nameMatcher.find()) name = nameMatcher.group(1);

            // Parse summary
            Pattern summaryPattern = Pattern.compile("\"summary\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
            Matcher summaryMatcher = summaryPattern.matcher(response);
            if (summaryMatcher.find()) {
                summary = summaryMatcher.group(1).replace("\\n", "\n").replace("\\\"", "\"");
            }

            // Parse cover
            Pattern coverPattern = Pattern.compile("\"cover\"\\s*:\\s*\\{[^}]*\"url\"\\s*:\\s*\"([^\"]+)\"");
            Matcher coverMatcher = coverPattern.matcher(response);
            if (coverMatcher.find()) {
                coverUrl = coverMatcher.group(1).replace("t_thumb", "t_cover_big").replace("//", "https://");
            }

            // Parse developer
            Pattern devPattern = Pattern.compile("\"company\"\\s*:\\s*\\{[^}]*\"name\"\\s*:\\s*\"([^\"]+)\"");
            Matcher devMatcher = devPattern.matcher(response);
            if (devMatcher.find()) developer = devMatcher.group(1);

            return new GameValidationResult(true, String.valueOf(igdbId), name, coverUrl, summary, developer, 1.0);

        } catch (Exception e) {
            System.err.println("[IGDB] Validation fetch error: " + e.getMessage());
        }
        return GameValidationResult.notAGame();
    }

    private boolean ensureIgdbToken() {
        if (igdbAccessToken != null && System.currentTimeMillis() < tokenExpiry) {
            return true;
        }

        if (igdbClientId == null || igdbClientSecret == null) return false;

        try {
            String tokenUrl = TWITCH_TOKEN_URL +
                "?client_id=" + igdbClientId +
                "&client_secret=" + igdbClientSecret +
                "&grant_type=client_credentials";

            String response = httpPost(tokenUrl, "");
            if (response != null && response.contains("access_token")) {
                Pattern tokenPattern = Pattern.compile("\"access_token\"\\s*:\\s*\"([^\"]+)\"");
                Matcher tokenMatcher = tokenPattern.matcher(response);
                if (tokenMatcher.find()) {
                    igdbAccessToken = tokenMatcher.group(1);
                }

                Pattern expiryPattern = Pattern.compile("\"expires_in\"\\s*:\\s*(\\d+)");
                Matcher expiryMatcher = expiryPattern.matcher(response);
                if (expiryMatcher.find()) {
                    long expiresIn = Long.parseLong(expiryMatcher.group(1));
                    tokenExpiry = System.currentTimeMillis() + (expiresIn * 1000) - 60000;
                }

                return igdbAccessToken != null;
            }
        } catch (Exception e) {
            System.err.println("[IGDB] Token refresh failed: " + e.getMessage());
        }
        return false;
    }

    private String igdbPost(String url, String body) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new java.net.URI(url).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Client-ID", igdbClientId);
            conn.setRequestProperty("Authorization", "Bearer " + igdbAccessToken);
            conn.setRequestProperty("Content-Type", "text/plain");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            System.err.println("[IGDB] Request error: " + e.getMessage());
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private String httpGet(String urlString) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new java.net.URI(urlString).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "NexusLauncher/1.0");

            if (conn.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            // Silent fail
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private String httpPost(String urlString, String body) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new java.net.URI(urlString).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            if (conn.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            // Silent fail
        } finally {
            if (conn != null) conn.disconnect();
        }
        return null;
    }

    private double calculateSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;

        int maxLen = Math.max(s1.length(), s2.length());
        int distance = levenshteinDistance(s1, s2);
        return 1.0 - ((double) distance / maxLen);
    }

    private int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= s2.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[s1.length()][s2.length()];
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
            game.getDescription(), game.getDeveloper()
        ));
    }

    private void applyFromCache(Game game, CachedMetadata cached) {
        if (game.getCoverImageUrl() == null) game.setCoverImageUrl(cached.coverUrl);
        if (game.getHeroImageUrl() == null) game.setHeroImageUrl(cached.heroUrl);
        if (game.getDescription() == null) game.setDescription(cached.description);
        if (game.getDeveloper() == null) game.setDeveloper(cached.developer);
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

    // Inner classes
    public static class GameValidationResult {
        private final boolean isGame;
        private final String igdbId;
        private final String igdbName;
        private final String coverUrl;
        private final String summary;
        private final String developer;
        private final double relevanceScore;

        public GameValidationResult(boolean isGame, String igdbId, String igdbName,
                                    String coverUrl, String summary, String developer, double relevanceScore) {
            this.isGame = isGame;
            this.igdbId = igdbId;
            this.igdbName = igdbName;
            this.coverUrl = coverUrl;
            this.summary = summary;
            this.developer = developer;
            this.relevanceScore = relevanceScore;
        }

        public boolean isGame() { return isGame; }
        public String getIgdbId() { return igdbId; }
        public String getIgdbName() { return igdbName; }
        public String getCoverUrl() { return coverUrl; }
        public String getSummary() { return summary; }
        public String getDeveloper() { return developer; }
        public double getRelevanceScore() { return relevanceScore; }

        public static GameValidationResult notAGame() {
            return new GameValidationResult(false, null, null, null, null, null, 0);
        }
    }

    private static class CachedMetadata {
        final String coverUrl, heroUrl, description, developer;
        final long timestamp;

        CachedMetadata(String coverUrl, String heroUrl, String description, String developer) {
            this.coverUrl = coverUrl;
            this.heroUrl = heroUrl;
            this.description = description;
            this.developer = developer;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRY_MS;
        }
    }
}

