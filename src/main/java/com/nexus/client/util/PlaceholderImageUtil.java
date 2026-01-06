package com.nexus.client.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for generating placeholder images using placehold.co API.
 * Uses consistent styling matching the Nexus Launcher theme.
 */
public class PlaceholderImageUtil {

    // Theme colors
    private static final String INDIGO_600 = "4f46e5";
    private static final String INDIGO_500 = "6366f1";
    private static final String GRAY_800 = "1f2937";
    private static final String WHITE = "ffffff";

    // Font for placeholder text
    private static final String FONT = "roboto";

    private PlaceholderImageUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Generate a placeholder URL for a game cover image.
     *
     * @param gameTitle The game title to display
     * @param width     The image width
     * @param height    The image height
     * @return The placehold.co URL
     */
    public static String getCoverPlaceholder(String gameTitle, int width, int height) {
        return generatePlaceholderUrl(gameTitle, width, height, INDIGO_600, WHITE);
    }

    /**
     * Generate a placeholder URL for a hero/banner image.
     *
     * @param gameTitle The game title to display
     * @param width     The image width
     * @param height    The image height
     * @return The placehold.co URL
     */
    public static String getHeroPlaceholder(String gameTitle, int width, int height) {
        return generatePlaceholderUrl(gameTitle, width, height, GRAY_800, INDIGO_500);
    }

    /**
     * Generate a generic placeholder URL with custom colors.
     *
     * @param text       The text to display
     * @param width      The image width
     * @param height     The image height
     * @param bgColor    Background color (hex without #)
     * @param textColor  Text color (hex without #)
     * @return The placehold.co URL
     */
    public static String generatePlaceholderUrl(String text, int width, int height, String bgColor, String textColor) {
        String displayText = sanitizeText(text);
        String encodedText = encodeText(displayText);

        return String.format(
            "https://placehold.co/%dx%d/%s/%s.png?text=%s&font=%s",
            width, height, bgColor, textColor, encodedText, FONT
        );
    }

    /**
     * Sanitize and truncate text for display in placeholder.
     *
     * @param text The original text
     * @return Sanitized text suitable for placeholder
     */
    private static String sanitizeText(String text) {
        if (text == null || text.isEmpty()) {
            return "Game";
        }

        // Clean up the text - remove special characters that might cause issues
        String cleaned = text.trim()
                .replaceAll("[^a-zA-Z0-9\\s\\-]", "") // Keep only alphanumeric, spaces and hyphens
                .replaceAll("\\s+", " "); // Normalize whitespace

        if (cleaned.isEmpty()) {
            return "Game";
        }

        // Truncate if too long (for better display)
        if (cleaned.length() > 20) {
            cleaned = cleaned.substring(0, 17) + "...";
        }

        return cleaned;
    }

    /**
     * URL encode text for use in placeholder URL.
     *
     * @param text The text to encode
     * @return URL-encoded text
     */
    private static String encodeText(String text) {
        try {
            // URL encode and replace spaces with %20 for placehold.co
            return URLEncoder.encode(text, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Fallback to simple replacement
            return text.replace(" ", "%20");
        }
    }
}


