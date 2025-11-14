package com.musicplayer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.musicplayer.model.Track;
import com.musicplayer.repository.DatabaseManager;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Service untuk enrich metadata track dari YouTube
 * Strategy: Get metadata dari yt-dlp --dump-json (genre, tags, categories)
 * Jalan di background thread agar tidak block UI
 */
public class MetadataEnrichmentService {

    private final YtDlpService ytDlpService;
    private final DatabaseManager dbManager;
    private final ExecutorService executorService;
    private final Set<String> processingIds;

    public MetadataEnrichmentService() {
        this.ytDlpService = new YtDlpService();
        this.dbManager = DatabaseManager.getInstance();
        this.executorService = Executors.newFixedThreadPool(2); // Max 2 concurrent enrichments
        this.processingIds = Collections.synchronizedSet(new HashSet<>());
    }

    /**
     * Enrich track metadata (async, non-blocking)
     * Call ini setelah track ditambahkan dari search
     */
    public void enrichTrackAsync(Track track) {
        if (track == null || track.getYoutubeId() == null) {
            return;
        }

        String videoId = track.getYoutubeId();

        // Skip if already processing atau sudah ada metadata
        if (processingIds.contains(videoId)) {
            return;
        }

        // Skip if already has complete metadata
        if (hasCompleteMetadata(track)) {
            System.out.println("✅ Track already has complete metadata: " + track.getTitle());
            return;
        }

        processingIds.add(videoId);

        // Enrich in background
        executorService.submit(() -> {
            try {
                enrichTrackSync(track);
            } finally {
                processingIds.remove(videoId);
            }
        });
    }

    /**
     * Enrich track metadata (synchronous)
     */
    public void enrichTrackSync(Track track) {
        if (track == null || track.getYoutubeId() == null) {
            return;
        }

        String videoId = track.getYoutubeId();
        System.out.println("🔄 Enriching metadata for: " + track.getTitle());

        try {
            // Get full info dari yt-dlp
            JsonNode info = ytDlpService.getTrackInfo(videoId);

            if (info == null) {
                System.err.println("⚠️ Could not get track info for: " + videoId);
                setDefaultMetadata(track);
                saveTrack(track);
                return;
            }

            // Extract metadata
            extractMetadata(track, info);

            // Save updated track
            saveTrack(track);

            System.out.println("✅ Metadata enriched: " + track.getTitle() +
                    " [Genre: " + track.getGenre() + ", Duration: " + track.getDuration() + "s]");

        } catch (Exception e) {
            System.err.println("❌ Failed to enrich metadata: " + e.getMessage());
            setDefaultMetadata(track);
            saveTrack(track);
        }
    }

    /**
     * Extract metadata dari yt-dlp JSON response
     */
    private void extractMetadata(Track track, JsonNode info) {
        // 1. GENRE - dari categories, tags, atau title analysis
        String genre = extractGenre(info);
        track.setGenre(genre);

        // 2. DURATION
        int duration = info.path("duration").asInt(0);
        track.setDuration(duration);

        // 3. MOOD - dari tags analysis atau title/description keywords
        String mood = extractMood(info);
        track.setMood(mood);

        // 4. BPM - estimate dari genre (realistic approach, actual detection too slow)
        int bpm = estimateBPMFromGenre(genre);
        track.setBpm(bpm);

        // 5. Update album if available
        String album = info.path("album").asText("");
        if (!album.isEmpty()) {
            track.setAlbum(album);
        }

        // 6. Update artist if more complete
        String artist = info.path("artist").asText("");
        if (!artist.isEmpty() && (track.getArtist() == null || track.getArtist().equals("Unknown"))) {
            track.setArtist(artist);
        }
    }

    /**
     * Extract genre dari YouTube metadata
     */
    private String extractGenre(JsonNode info) {
        // Priority 1: From 'genre' field
        String genre = info.path("genre").asText("");
        if (!genre.isEmpty() && !genre.equalsIgnoreCase("Music")) {
            return normalizeGenre(genre);
        }

        // Priority 2: From 'categories'
        JsonNode categories = info.path("categories");
        if (categories.isArray() && categories.size() > 0) {
            String category = categories.get(0).asText("");
            if (!category.isEmpty() && !category.equalsIgnoreCase("Music")) {
                return normalizeGenre(category);
            }
        }

        // Priority 3: From 'tags' analysis
        JsonNode tags = info.path("tags");
        if (tags.isArray()) {
            String genreFromTags = extractGenreFromTags(tags);
            if (genreFromTags != null) {
                return genreFromTags;
            }
        }

        // Priority 4: From title/description keywords
        String title = info.path("title").asText("");
        String description = info.path("description").asText("");
        String genreFromText = extractGenreFromText(title + " " + description);
        if (genreFromText != null) {
            return genreFromText;
        }

        // Default
        return "Pop"; // Most common default
    }

    /**
     * Extract mood dari tags dan description
     */
    private String extractMood(JsonNode info) {
        List<String> keywords = new ArrayList<>();

        // Get tags
        JsonNode tags = info.path("tags");
        if (tags.isArray()) {
            for (JsonNode tag : tags) {
                keywords.add(tag.asText("").toLowerCase());
            }
        }

        // Get from description
        String description = info.path("description").asText("").toLowerCase();
        keywords.add(description);

        // Analyze keywords untuk mood
        String allText = String.join(" ", keywords);

        // Energetic moods
        if (allText.contains("energetic") || allText.contains("upbeat") ||
                allText.contains("party") || allText.contains("dance") ||
                allText.contains("fast") || allText.contains("hype")) {
            return "Energetic";
        }

        // Happy moods
        if (allText.contains("happy") || allText.contains("cheerful") ||
                allText.contains("positive") || allText.contains("joy")) {
            return "Happy";
        }

        // Calm/Relaxed moods
        if (allText.contains("calm") || allText.contains("relaxing") ||
                allText.contains("chill") || allText.contains("peaceful") ||
                allText.contains("ambient") || allText.contains("lofi")) {
            return "Calm";
        }

        // Sad/Melancholic moods
        if (allText.contains("sad") || allText.contains("melancholic") ||
                allText.contains("emotional") || allText.contains("heartbreak")) {
            return "Sad";
        }

        // Intense/Dark moods
        if (allText.contains("intense") || allText.contains("dark") ||
                allText.contains("heavy") || allText.contains("aggressive")) {
            return "Intense";
        }

        // Default based on genre if available
        String genre = info.path("genre").asText("").toLowerCase();
        if (genre.contains("rock") || genre.contains("metal")) {
            return "Intense";
        } else if (genre.contains("pop")) {
            return "Happy";
        } else if (genre.contains("electronic") || genre.contains("edm")) {
            return "Energetic";
        } else if (genre.contains("classical") || genre.contains("jazz")) {
            return "Calm";
        }

        return "Neutral";
    }

    /**
     * Extract genre dari tags (music genre keywords)
     */
    private String extractGenreFromTags(JsonNode tags) {
        Map<String, String> genreKeywords = new HashMap<>();
        genreKeywords.put("pop", "Pop");
        genreKeywords.put("rock", "Rock");
        genreKeywords.put("hip hop", "Hip Hop");
        genreKeywords.put("rap", "Hip Hop");
        genreKeywords.put("electronic", "Electronic");
        genreKeywords.put("edm", "Electronic");
        genreKeywords.put("dance", "Dance");
        genreKeywords.put("r&b", "R&B");
        genreKeywords.put("rnb", "R&B");
        genreKeywords.put("jazz", "Jazz");
        genreKeywords.put("classical", "Classical");
        genreKeywords.put("country", "Country");
        genreKeywords.put("metal", "Metal");
        genreKeywords.put("indie", "Indie");
        genreKeywords.put("alternative", "Alternative");
        genreKeywords.put("reggae", "Reggae");
        genreKeywords.put("blues", "Blues");
        genreKeywords.put("soul", "Soul");
        genreKeywords.put("folk", "Folk");
        genreKeywords.put("punk", "Punk");

        for (JsonNode tag : tags) {
            String tagLower = tag.asText("").toLowerCase();
            for (Map.Entry<String, String> entry : genreKeywords.entrySet()) {
                if (tagLower.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }

        return null;
    }

// Ekstrak genre dari judul
    private String extractGenreFromText(String text) {
        String textLower = text.toLowerCase();

        // Check for genre keywords in text
        if (textLower.contains("pop music") || textLower.contains(" pop ")) return "Pop";
        if (textLower.contains("rock music") || textLower.contains(" rock ")) return "Rock";
        if (textLower.contains("hip hop") || textLower.contains("rap ")) return "Hip Hop";
        if (textLower.contains("electronic") || textLower.contains("edm ")) return "Electronic";
        if (textLower.contains("jazz")) return "Jazz";
        if (textLower.contains("classical")) return "Classical";
        if (textLower.contains("country")) return "Country";
        if (textLower.contains("metal")) return "Metal";
        if (textLower.contains("indie")) return "Indie";
        if (textLower.contains("r&b") || textLower.contains("rnb")) return "R&B";

        return null;
    }

    private String normalizeGenre(String genre) {
        if (genre == null || genre.isEmpty()) return "Pop";

        String normalized = genre.trim();

        // Capitalize first letter
        if (normalized.length() > 0) {
            normalized = normalized.substring(0, 1).toUpperCase() +
                    normalized.substring(1).toLowerCase();
        }

        // Common normalizations
        Map<String, String> genreMap = new HashMap<>();
        genreMap.put("hiphop", "Hip Hop");
        genreMap.put("hip-hop", "Hip Hop");
        genreMap.put("r&b", "R&B");
        genreMap.put("rnb", "R&B");
        genreMap.put("edm", "Electronic");

        for (Map.Entry<String, String> entry : genreMap.entrySet()) {
            if (normalized.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }

        return normalized;
    }

// Estimasi BPM
    private int estimateBPMFromGenre(String genre) {
        if (genre == null) return 120;

        String genreLower = genre.toLowerCase();

        // Genre BPM ranges (averages)
        if (genreLower.contains("electronic") || genreLower.contains("edm")) {
            return 128; // EDM: 120-140
        } else if (genreLower.contains("hip hop") || genreLower.contains("rap")) {
            return 90; // Hip Hop: 80-100
        } else if (genreLower.contains("rock")) {
            return 120; // Rock: 110-140
        } else if (genreLower.contains("pop")) {
            return 120; // Pop: 100-130
        } else if (genreLower.contains("metal")) {
            return 140; // Metal: 120-180
        } else if (genreLower.contains("jazz")) {
            return 120; // Jazz: 100-140
        } else if (genreLower.contains("classical")) {
            return 80; // Classical: varies widely
        } else if (genreLower.contains("reggae")) {
            return 90; // Reggae: 80-110
        } else if (genreLower.contains("country")) {
            return 110; // Country: 90-120
        } else if (genreLower.contains("r&b") || genreLower.contains("soul")) {
            return 85; // R&B: 70-100
        } else if (genreLower.contains("dance")) {
            return 125; // Dance: 120-135
        }

        return 120; // Default BPM
    }


    private boolean hasCompleteMetadata(Track track) {
        return track.getGenre() != null && !track.getGenre().isEmpty() &&
                track.getMood() != null && !track.getMood().isEmpty() &&
                track.getBpm() > 0 &&
                track.getDuration() > 0;
    }

    /**
     * MetaData Default
     */
    private void setDefaultMetadata(Track track) {
        if (track.getGenre() == null || track.getGenre().isEmpty()) {
            track.setGenre("Pop");
        }
        if (track.getMood() == null || track.getMood().isEmpty()) {
            track.setMood("Neutral");
        }
        if (track.getBpm() <= 0) {
            track.setBpm(120);
        }
        if (track.getDuration() <= 0) {
            track.setDuration(180); // 3 minutes default
        }
    }

    /**
     * Save track to database
     */
    private void saveTrack(Track track) {
        try {
            dbManager.saveTrack(track);
        } catch (SQLException e) {
            System.err.println("Failed to save track: " + e.getMessage());
        }
    }


    public void enrichTracksAsync(List<Track> tracks) {
        for (Track track : tracks) {
            enrichTrackAsync(track);
        }
    }


    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
}