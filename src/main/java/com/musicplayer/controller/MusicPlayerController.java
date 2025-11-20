package com.musicplayer.controller;

import com.musicplayer.model.Recommendation;
import com.musicplayer.model.Track;
import com.musicplayer.repository.DatabaseManager;
import com.musicplayer.service.*;
import com.musicplayer.view.NowPlayingPanel;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Controller everything

public class MusicPlayerController {
    private DatabaseManager dbManager;
    private YouTubeMusicService youtubeService;
    private AudioFingerprintService fingerprintService;
    private AudioRecordingService recordingService;
    private RecommendationService recommendationService;
    private AudioPlayerService audioPlayerService;
    private MetadataEnrichmentService enrichmentService;

    private NowPlayingPanel nowPlayingPanel;
    private File tempRecordingFile;

    public MusicPlayerController() {
        System.out.println("🔧 Initializing MusicPlayerController...");

        this.dbManager = DatabaseManager.getInstance();
        this.youtubeService = new YouTubeMusicService();
        this.fingerprintService = new AudioFingerprintService();
        this.recordingService = new AudioRecordingService();
        this.recommendationService = new RecommendationService();
        this.audioPlayerService = new AudioPlayerService();
        this.enrichmentService = new MetadataEnrichmentService();

        setupPlayerListeners();

        System.out.println("✅ MusicPlayerController initialized");
    }

    public void initialize() {
        System.out.println("🚀 Controller initialization completed");
    }

    private void setupPlayerListeners() {
        audioPlayerService.addPlayerStateListener(new AudioPlayerService.PlayerStateListener() {
            @Override
            public void onPlayerStateChanged(boolean isPlaying) {
                // Player state changed
            }

            @Override
            public void onTrackChanged(Track track) {
                if (nowPlayingPanel != null) {
                    nowPlayingPanel.updateNowPlaying(track);
                }

                if (track != null) {
                    try {
                        dbManager.recordPlay(track.getId());
                        System.out.println("📊 Play recorded for: " + track.getTitle());
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onTimeChanged(long seconds) {
                // Time update
            }

            @Override
            public void onError(String message) {
                System.err.println("❌ Player error: " + message);
            }
        });
    }

    public void setNowPlayingPanel(NowPlayingPanel panel) {
        this.nowPlayingPanel = panel;
    }

    // ========== YouTube Music Operations ==========

    /**
     * Search YouTube Music - IMPROVED with better logging
     */
    public List<Track> searchYouTubeMusic(String query) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🔍 SEARCH: " + query);
        System.out.println("=".repeat(60));

        try {
            // 1. Search via InnerTube API
            System.out.println("📡 Calling YouTube Music API...");
            List<Track> tracks = youtubeService.searchTracks(query);

            if (tracks == null) {
                System.err.println("❌ Search returned null");
                return new ArrayList<>();
            }

            if (tracks.isEmpty()) {
                System.out.println("⚠️ No results found for: " + query);
                return tracks;
            }

            System.out.println("✅ Found " + tracks.size() + " tracks:");
            for (int i = 0; i < Math.min(3, tracks.size()); i++) {
                Track t = tracks.get(i);
                System.out.println("   " + (i+1) + ". " + t.getArtist() + " - " + t.getTitle());
            }

            // 2. Save basic info to database
            System.out.println("💾 Saving tracks to database...");
            for (Track track : tracks) {
                try {
                    // Set default metadata
                    if (track.getGenre() == null || track.getGenre().isEmpty()) {
                        track.setGenre("Music");
                    }
                    if (track.getMood() == null || track.getMood().isEmpty()) {
                        track.setMood("Neutral");
                    }
                    if (track.getBpm() <= 0) {
                        track.setBpm(120);
                    }

                    dbManager.saveTrack(track);
                } catch (SQLException e) {
                    System.err.println("⚠️ Failed to save track: " + e.getMessage());
                }
            }

            // 3. Enrich metadata in background (non-blocking)
            // DISABLED for now to avoid duplicate tracks
            // System.out.println("🔄 Starting metadata enrichment in background...");
            // enrichmentService.enrichTracksAsync(tracks);

            System.out.println("=".repeat(60) + "\n");
            return tracks;

        } catch (Exception e) {
            System.err.println("❌ Search error: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // ========== Audio Fingerprinting Operations ==========

    public void startRecording() throws Exception {
        recordingService.startRecording();
    }

    public Track stopRecordingAndIdentify() throws Exception {
        tempRecordingFile = recordingService.stopRecording("temp_recording.wav");
        Track track = fingerprintService.identifyTrack(tempRecordingFile);

        if (track != null) {
            // Enrich metadata synchronously for identified tracks
            enrichmentService.enrichTrackSync(track);
        }

        if (tempRecordingFile != null && tempRecordingFile.exists()) {
            tempRecordingFile.delete();
        }

        return track;
    }

    public Track identifyTrack(File audioFile) throws Exception {
        Track track = fingerprintService.identifyTrack(audioFile);

        if (track != null) {
            enrichmentService.enrichTrackSync(track);
        }

        return track;
    }

    public void saveTrack(Track track) {
        try {
            dbManager.saveTrack(track);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ========== Recommendation Operations ==========

    public List<Recommendation> generateDailyMix(int limit) {
        try {
            System.out.println("🎵 Generating Daily Mix (" + limit + " tracks)...");
            List<Recommendation> mix = recommendationService.generateDailyMix(limit);
            System.out.println("✅ Daily Mix generated: " + mix.size() + " tracks");
            return mix;
        } catch (SQLException e) {
            System.err.println("❌ Failed to generate Daily Mix: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<Recommendation> getSimilarTracks(Track track, int limit) {
        try {
            return recommendationService.getSimilarTracks(track, limit);
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<Recommendation> getSimilarToRecent(int limit) {
        try {
            List<Track> recentTracks = dbManager.getMostPlayedTracks(1);
            if (recentTracks.isEmpty()) {
                System.out.println("⚠️ No play history for similar songs");
                return new ArrayList<>();
            }

            System.out.println("🔍 Finding songs similar to: " + recentTracks.get(0).getTitle());
            return recommendationService.getSimilarTracks(recentTracks.get(0), limit);
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    // ========== Playback Operations ==========

    public void setQueueAndPlay(List<Track> tracks, int startIndex) {
        System.out.println("▶️ Setting queue: " + tracks.size() + " tracks, starting at index " + startIndex);
        audioPlayerService.setQueueAndPlay(tracks, startIndex);
    }

    public void play() {
        audioPlayerService.play();
    }

    public void pause() {
        audioPlayerService.pause();
    }

    public void stop() {
        audioPlayerService.stop();
    }

    public void togglePlayPause() {
        audioPlayerService.togglePlayPause();
    }

    public void playNext() {
        audioPlayerService.playNext();
    }

    public void playPrevious() {
        audioPlayerService.playPrevious();
    }

    public void setVolume(int volume) {
        audioPlayerService.setVolume(volume);
    }

    public void seekTo(long seconds) {
        audioPlayerService.seekTo(seconds);
    }

    public void toggleShuffle() {
        audioPlayerService.toggleShuffle();
    }

    public void toggleRepeat() {
        audioPlayerService.toggleRepeat();
    }

    public boolean isPlayerPlaying() {
        return audioPlayerService.isPlaying();
    }

    public Track getCurrentTrack() {
        return audioPlayerService.getCurrentTrack();
    }

    public long getPlayerCurrentTime() {
        return audioPlayerService.getCurrentTime();
    }

    public long getPlayerDuration() {
        return audioPlayerService.getDuration();
    }

    public boolean isShuffleEnabled() {
        return audioPlayerService.isShuffle();
    }

    public boolean isRepeatEnabled() {
        return audioPlayerService.isRepeat();
    }

    public List<Track> getAllTracks() {
        try {
            return dbManager.getAllTracks();
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void shutdown() {
        System.out.println("🛑 Shutting down controller...");
        enrichmentService.shutdown();
        audioPlayerService.release();
        dbManager.close();
        System.out.println("✅ Controller shutdown complete");
    }
}