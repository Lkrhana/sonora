package com.musicplayer.service;

import com.musicplayer.model.Recommendation;
import com.musicplayer.model.Track;
import com.musicplayer.repository.DatabaseManager;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Improved Recommendation Service dengan algoritma yang lebih baik
 */
public class RecommendationService {
    private final DatabaseManager dbManager;

    public RecommendationService() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Generate Daily Mix - Mix of favorites + diverse new recommendations
     */
    public List<Recommendation> generateDailyMix(int limit) throws SQLException {
        List<Track> allTracks = dbManager.getAllTracks();

        System.out.println("📊 Database has " + allTracks.size() + " tracks");

        if (allTracks.isEmpty()) {
            System.out.println("⚠️ No tracks in database for Daily Mix");
            return Collections.emptyList();
        }

        // Remove duplicates by ID
        List<Track> uniqueTracks = new ArrayList<>();
        Set<String> seenIds = new HashSet<>();

        for (Track track : allTracks) {
            String uniqueKey = track.getArtist() + "|" + track.getTitle();
            if (!seenIds.contains(uniqueKey)) {
                seenIds.add(uniqueKey);
                uniqueTracks.add(track);
            }
        }

        System.out.println("📊 After removing duplicates: " + uniqueTracks.size() + " unique tracks");
        allTracks = uniqueTracks;

        // Get play history
        List<Track> mostPlayed = dbManager.getMostPlayedTracks(Math.min(10, allTracks.size()));

        List<Recommendation> dailyMix = new ArrayList<>();

        if (!mostPlayed.isEmpty()) {
            // Analyze user preferences from play history
            Map<String, Integer> genrePreference = new HashMap<>();
            Map<String, Integer> artistPreference = new HashMap<>();
            Map<String, Integer> moodPreference = new HashMap<>();
            Set<String> playedIds = new HashSet<>();

            for (Track track : mostPlayed) {
                playedIds.add(track.getId());

                if (track.getGenre() != null && !track.getGenre().isEmpty()) {
                    genrePreference.merge(track.getGenre(), 1, Integer::sum);
                }
                if (track.getArtist() != null && !track.getArtist().isEmpty()) {
                    artistPreference.merge(track.getArtist(), 1, Integer::sum);
                }
                if (track.getMood() != null && !track.getMood().isEmpty()) {
                    moodPreference.merge(track.getMood(), 1, Integer::sum);
                }
            }

            // Add some favorite tracks (20% of mix)
            int favoriteCount = Math.min(4, limit / 5);
            for (int i = 0; i < favoriteCount && i < mostPlayed.size(); i++) {
                dailyMix.add(new Recommendation(
                        mostPlayed.get(i),
                        1.0,
                        "Your Favorite"
                ));
            }

            // Score all other tracks based on preferences
            List<ScoredTrack> scoredTracks = new ArrayList<>();
            for (Track track : allTracks) {
                if (playedIds.contains(track.getId())) {
                    continue; // Skip already played
                }

                double score = calculatePreferenceScore(
                        track,
                        genrePreference,
                        artistPreference,
                        moodPreference
                );

                if (score > 0.2) { // Threshold to include diverse recommendations
                    scoredTracks.add(new ScoredTrack(track, score));
                }
            }

            // Sort by score and add diversity
            Collections.sort(scoredTracks, (a, b) -> Double.compare(b.score, a.score));

            // Add top recommendations with diversity
            Set<String> usedGenres = new HashSet<>();
            Set<String> usedArtists = new HashSet<>();
            int remaining = limit - dailyMix.size();
            int added = 0;

            // First pass: Add highly scored tracks with diversity
            for (ScoredTrack st : scoredTracks) {
                if (added >= remaining) break;

                String genre = st.track.getGenre();
                String artist = st.track.getArtist();

                // Prefer diverse genres and artists
                boolean isDiverse = !usedGenres.contains(genre) || !usedArtists.contains(artist);

                if (isDiverse || added > remaining / 2) {
                    dailyMix.add(new Recommendation(
                            st.track,
                            st.score,
                            getRecommendationReason(st.track, genrePreference, artistPreference)
                    ));

                    if (genre != null) usedGenres.add(genre);
                    if (artist != null) usedArtists.add(artist);
                    added++;
                }
            }

            // Second pass: Fill remaining slots with best matches
            for (ScoredTrack st : scoredTracks) {
                if (added >= remaining) break;

                boolean alreadyAdded = dailyMix.stream()
                        .anyMatch(r -> r.getTrack().getId().equals(st.track.getId()));

                if (!alreadyAdded) {
                    dailyMix.add(new Recommendation(
                            st.track,
                            st.score,
                            "Recommended for you"
                    ));
                    added++;
                }
            }

        } else {
            // No play history - return diverse selection
            Collections.shuffle(allTracks);
            for (int i = 0; i < Math.min(limit, allTracks.size()); i++) {
                dailyMix.add(new Recommendation(
                        allTracks.get(i),
                        0.5,
                        "Discover this"
                ));
            }
        }

        // Shuffle for variety but keep favorites at start
        if (dailyMix.size() > 4) {
            List<Recommendation> favorites = dailyMix.subList(0, Math.min(4, dailyMix.size()));
            List<Recommendation> others = new ArrayList<>(dailyMix.subList(Math.min(4, dailyMix.size()), dailyMix.size()));
            Collections.shuffle(others);
            dailyMix = new ArrayList<>(favorites);
            dailyMix.addAll(others);
        }

        return dailyMix;
    }

    /**
     * Get similar tracks to a specific track
     */
    public List<Recommendation> getSimilarTracks(Track referenceTrack, int limit) throws SQLException {
        List<Track> allTracks = dbManager.getAllTracks();
        List<Recommendation> recommendations = new ArrayList<>();

        for (Track track : allTracks) {
            if (track.getId().equals(referenceTrack.getId())) {
                continue;
            }

            double similarity = calculateContentSimilarity(referenceTrack, track);

            if (similarity > 0.25) { // Lower threshold for more results
                recommendations.add(new Recommendation(
                        track,
                        similarity,
                        getSimilarityReason(referenceTrack, track)
                ));
            }
        }

        recommendations.sort((r1, r2) -> Double.compare(r2.getScore(), r1.getScore()));

        return recommendations.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Calculate content-based similarity (improved)
     */
    private double calculateContentSimilarity(Track track1, Track track2) {
        double similarity = 0.0;

        // Same artist = high similarity (30%)
        if (track1.getArtist() != null && track2.getArtist() != null &&
                track1.getArtist().equalsIgnoreCase(track2.getArtist())) {
            similarity += 0.30;
        }

        // Same genre = moderate similarity (25%)
        if (track1.getGenre() != null && track2.getGenre() != null &&
                track1.getGenre().equalsIgnoreCase(track2.getGenre())) {
            similarity += 0.25;
        }

        // Same mood = moderate similarity (20%)
        if (track1.getMood() != null && track2.getMood() != null &&
                track1.getMood().equalsIgnoreCase(track2.getMood())) {
            similarity += 0.20;
        }

        // Similar BPM = small similarity (15%)
        if (track1.getBpm() > 0 && track2.getBpm() > 0) {
            int bpmDiff = Math.abs(track1.getBpm() - track2.getBpm());
            double bpmSimilarity = Math.max(0, 1.0 - (bpmDiff / 80.0)); // Within 80 BPM
            similarity += bpmSimilarity * 0.15;
        }

        // Same album = bonus (10%)
        if (track1.getAlbum() != null && track2.getAlbum() != null &&
                !track1.getAlbum().isEmpty() && !track2.getAlbum().isEmpty() &&
                track1.getAlbum().equalsIgnoreCase(track2.getAlbum())) {
            similarity += 0.10;
        }

        return similarity;
    }

    /**
     * Calculate preference score based on user listening history
     */
    private double calculatePreferenceScore(Track track,
                                            Map<String, Integer> genrePreference,
                                            Map<String, Integer> artistPreference,
                                            Map<String, Integer> moodPreference) {
        double score = 0.0;
        int totalPlays = genrePreference.values().stream().mapToInt(Integer::intValue).sum();

        if (totalPlays == 0) {
            return 0.5; // Neutral score for new users
        }

        // Genre match (40% weight)
        if (track.getGenre() != null && genrePreference.containsKey(track.getGenre())) {
            double genreScore = genrePreference.get(track.getGenre()) / (double) totalPlays;
            score += genreScore * 0.40;
        }

        // Artist match (30% weight)
        if (track.getArtist() != null && artistPreference.containsKey(track.getArtist())) {
            double artistScore = artistPreference.get(track.getArtist()) / (double) totalPlays;
            score += artistScore * 0.30;
        }

        // Mood match (30% weight)
        if (track.getMood() != null && moodPreference.containsKey(track.getMood())) {
            double moodScore = moodPreference.get(track.getMood()) / (double) totalPlays;
            score += moodScore * 0.30;
        }

        return Math.min(score, 1.0);
    }

    /**
     * Get recommendation reason based on match
     */
    private String getRecommendationReason(Track track,
                                           Map<String, Integer> genrePreference,
                                           Map<String, Integer> artistPreference) {
        if (track.getArtist() != null && artistPreference.containsKey(track.getArtist())) {
            return "More from " + track.getArtist();
        } else if (track.getGenre() != null && genrePreference.containsKey(track.getGenre())) {
            return "Because you like " + track.getGenre();
        } else {
            return "Recommended for you";
        }
    }

    /**
     * Get similarity reason
     */
    private String getSimilarityReason(Track reference, Track similar) {
        if (reference.getArtist() != null && similar.getArtist() != null &&
                reference.getArtist().equalsIgnoreCase(similar.getArtist())) {
            return "Same artist";
        } else if (reference.getGenre() != null && similar.getGenre() != null &&
                reference.getGenre().equalsIgnoreCase(similar.getGenre())) {
            return "Similar genre";
        } else if (reference.getMood() != null && similar.getMood() != null &&
                reference.getMood().equalsIgnoreCase(similar.getMood())) {
            return "Similar mood";
        } else {
            return "Similar to " + reference.getTitle();
        }
    }

    /**
     * Helper class for scoring
     */
    private static class ScoredTrack {
        Track track;
        double score;

        ScoredTrack(Track track, double score) {
            this.track = track;
            this.score = score;
        }
    }
}