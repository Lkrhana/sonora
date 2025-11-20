package com.musicplayer.util;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

// Semua Icon diambil dari Google Material 3, formatnya wajib png, taro di resources > Icons
public class IconLoader {

    private static final String ICON_PATH = "/icons/";
    private static final Map<String, BufferedImage> iconCache = new HashMap<>();


    public static class Icons {
        // Logo icon (asal bae ngambil)
        public static final String LOGO = "logo.png";

        // Navigation icons
        public static final String HOME = "home.png";
        public static final String NOW_PLAYING = "nowplaying.png";
        public static final String RECOMMENDATIONS = "recommendations.png";
        public static final String FINGERPRINT = "fingerprint.png";

        // Player control icons
        public static final String PLAY = "play.png";
        public static final String PAUSE = "pause.png";
        public static final String STOP = "stop.png";
        public static final String NEXT = "next.png";
        public static final String PREVIOUS = "previous.png";
        public static final String SHUFFLE = "shuffle.png";
        public static final String SHUFFLE_ON = "shuffle_on.png";
        public static final String REPEAT = "repeat.png";
        public static final String REPEAT_ON = "repeat_on.png";

        // Volume icons
        public static final String VOLUME = "volume.png";
        public static final String VOLUME_MUTE = "volume_mute.png";

        // Action icons
        public static final String SEARCH = "search.png";
        public static final String RECORD = "record.png";
        public static final String UPLOAD = "upload.png";
        public static final String REFRESH = "refresh.png";
        public static final String CLOSE = "close.png";

        // Status icons
        public static final String LOADING = "loading.png";
        public static final String ERROR = "error.png";
        public static final String SUCCESS = "success.png";
        public static final String WARNING = "warning.png";

        // Music icons
        public static final String MUSICLIBRARY = "musiclibrary.png";
        public static final String MUSIC = "music.png";
        public static final String ALBUM = "album.png";
        public static final String ARTIST = "artist.png";
        public static final String PLAYLIST = "playlist.png";
    }

    /**
     * Load icon size default 24 x 24
     */
    public static ImageIcon loadIcon(String iconName) {
        return loadIcon(iconName, 24, 24);
    }

    /**
     * Load icon spec size
     */
    public static ImageIcon loadIcon(String iconName, int width, int height) {
        try {
            BufferedImage image = getIconImage(iconName);

            if (image != null) {
                // Resize to requested size
                Image scaledImage = image.getScaledInstance(
                        width,
                        height,
                        Image.SCALE_SMOOTH
                );
                return new ImageIcon(scaledImage);
            }
        } catch (Exception e) {
            System.err.println("Failed to load icon: " + iconName + " - " + e.getMessage());
        }

        // Return placeholder if icon not found
        return createPlaceholder(width, height);
    }

    /**
     * Load icon untuk button
     */
    public static ImageIcon loadButtonIcon(String iconName) {
        return loadIcon(iconName, 24, 24);
    }

    /**
     * Load icon untuk sidebar navigation
     */
    public static ImageIcon loadNavIcon(String iconName) {
        return loadIcon(iconName, 20, 20);
    }

    /**
     * Load icon untuk large button
     */
    public static ImageIcon loadLargeIcon(String iconName) {
        return loadIcon(iconName, 32, 32);
    }

    /**
     * Get raw icon image (cached)
     */
    private static BufferedImage getIconImage(String iconName) {
        // Check cache first
        if (iconCache.containsKey(iconName)) {
            return iconCache.get(iconName);
        }

        try {
            String path = ICON_PATH + iconName;
            InputStream is = IconLoader.class.getResourceAsStream(path);

            if (is != null) {
                BufferedImage image = ImageIO.read(is);
                if (image != null) {
                    // Cache the image
                    iconCache.put(iconName, image);
                    System.out.println("✅ Loaded icon: " + iconName);
                    return image;
                }
            } else {
                System.err.println("⚠️ Icon not found: " + path);
            }
        } catch (Exception e) {
            System.err.println("❌ Error loading icon " + iconName + ": " + e.getMessage());
        }

        return null;
    }

    /**
     * Create placeholder icon jika file tidak ditemukan
     */
    private static ImageIcon createPlaceholder(int width, int height) {
        BufferedImage placeholder = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = placeholder.createGraphics();

        // Enable anti-aliasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw placeholder background
        g2d.setColor(new Color(100, 100, 110, 150));
        g2d.fillRoundRect(0, 0, width, height, 5, 5);

        // Draw border
        g2d.setColor(new Color(150, 150, 160));
        g2d.drawRoundRect(0, 0, width - 1, height - 1, 5, 5);

        g2d.dispose();
        return new ImageIcon(placeholder);
    }

    /**
     * Clear icon cache
     */
    public static void clearCache() {
        iconCache.clear();
    }

    /**
     * Preload all icons (optional - untuk performance)
     */
    public static void preloadIcons() {
        System.out.println("🔄 Preloading icons...");

        // List all icon names dari Icons class
        String[] iconNames = {
                Icons.HOME, Icons.NOW_PLAYING, Icons.RECOMMENDATIONS, Icons.FINGERPRINT,
                Icons.PLAY, Icons.PAUSE, Icons.STOP, Icons.NEXT, Icons.PREVIOUS,
                Icons.SHUFFLE, Icons.SHUFFLE_ON, Icons.REPEAT, Icons.REPEAT_ON,
                Icons.VOLUME, Icons.VOLUME_MUTE,
                Icons.SEARCH, Icons.RECORD, Icons.UPLOAD, Icons.REFRESH, Icons.CLOSE,
                Icons.LOADING, Icons.ERROR, Icons.SUCCESS, Icons.WARNING,
                Icons.MUSIC, Icons.ALBUM, Icons.ARTIST, Icons.PLAYLIST
        };

        int loaded = 0;
        for (String iconName : iconNames) {
            if (getIconImage(iconName) != null) {
                loaded++;
            }
        }

        System.out.println("✅ Preloaded " + loaded + "/" + iconNames.length + " icons");
    }

    /**
     * Check if icon exists
     */
    public static boolean iconExists(String iconName) {
        String path = ICON_PATH + iconName;
        InputStream is = IconLoader.class.getResourceAsStream(path);
        return is != null;
    }

    /**
     * Get list of missing icons (untuk debugging)
     */
    public static void printMissingIcons() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("ICON STATUS CHECK");
        System.out.println("=".repeat(60));
        System.out.println("Icon folder: src/main/resources/icons/");
        System.out.println("Expected format: png or JPG");
        System.out.println();

        String[] requiredIcons = {
                Icons.HOME, Icons.NOW_PLAYING, Icons.RECOMMENDATIONS, Icons.FINGERPRINT,
                Icons.PLAY, Icons.PAUSE, Icons.STOP, Icons.NEXT, Icons.PREVIOUS,
                Icons.SHUFFLE, Icons.REPEAT, Icons.VOLUME,
                Icons.SEARCH, Icons.RECORD, Icons.UPLOAD, Icons.REFRESH,
                Icons.MUSIC
        };

        int found = 0;
        int missing = 0;

        for (String iconName : requiredIcons) {
            boolean exists = iconExists(iconName);
            String status = exists ? "✅ Found" : "❌ Missing";
            System.out.println(status + " - " + iconName);

            if (exists) found++;
            else missing++;
        }

        System.out.println();
        System.out.println("Summary: " + found + " found, " + missing + " missing");

        if (missing > 0) {
            System.out.println("\n⚠️ Missing icons will show as gray placeholders");
            System.out.println("📁 Place icon files in: src/main/resources/icons/");
        }

        System.out.println("=".repeat(60) + "\n");
    }
}