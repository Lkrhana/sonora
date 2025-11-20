package com.musicplayer.controller;

import com.musicplayer.model.Recommendation;
import com.musicplayer.model.Track;
import com.musicplayer.repository.DatabaseManager;
import com.musicplayer.service.*;
import com.musicplayer.view.NowPlayingPanel;
import com.musicplayer.repository.AnalyticsDAO;
import com.musicplayer.repository.AnalyticsDatabaseManager;
import com.musicplayer.service.LyricsService;

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
    private AnalyticsDAO analyticsDAO;
    private NowPlayingPanel nowPlayingPanel;
    private File tempRecordingFile;
    private LyricsService lyricsService;

    public MusicPlayerController() {
        System.out.println("🔧 Initializing MusicPlayerController...");

        this.dbManager = DatabaseManager.getInstance();
        this.youtubeService = new YouTubeMusicService();
        this.fingerprintService = new AudioFingerprintService();
        this.recordingService = new AudioRecordingService();
        this.recommendationService = new RecommendationService();
        this.audioPlayerService = new AudioPlayerService();
        this.enrichmentService = new MetadataEnrichmentService();
        this.analyticsDAO = new AnalyticsDAO();
        this.lyricsService = new LyricsService();

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

                        // ✅ TAMBAHKAN INI: Track untuk analytics SQLite
                        analyticsDAO.recordPlay(
                            track.getId(),
                            track.getTitle(),
                            track.getArtist(),
                            track.getGenre(),
                            track.getDuration()
                        );

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

    public AudioPlayerService.RepeatMode getRepeatMode() {
        return audioPlayerService.getRepeatMode();
    }
    
    public List<Track> getAllTracks() {
        try {
            return dbManager.getAllTracks();
        } catch (SQLException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    public void setEqualizerEnabled(boolean enabled) {
        audioPlayerService.setEqualizerEnabled(enabled);
    }
    
    public boolean isEqualizerEnabled() {
        return audioPlayerService.isEqualizerEnabled();
    }
    
    public void setPreamp(float value) {
        audioPlayerService.setPreamp(value);
    }
    
    public float getPreamp() {
        return audioPlayerService.getPreamp();
    }
    
    public void setBandAmplitude(int bandIndex, float amplitude) {
        audioPlayerService.setBandAmplitude(bandIndex, amplitude);
    }
    
    public float getBandAmplitude(int bandIndex) {
        return audioPlayerService.getBandAmplitude(bandIndex);
    }
    
    public float[] getAllBandAmplitudes() {
        return audioPlayerService.getAllBandAmplitudes();
    }
    
    public void setAllBandAmplitudes(float[] amplitudes) {
        audioPlayerService.setAllBandAmplitudes(amplitudes);
    }
    
    public void resetEqualizer() {
        audioPlayerService.resetEqualizer();
    }
    
    public void loadPreset(String presetName) {
        audioPlayerService.loadPreset(presetName);
    }
    
    public String[] getAvailablePresets() {
        return audioPlayerService.getAvailablePresets();
    }
    
    public String[] getBandFrequencies() {
        return audioPlayerService.getBandFrequencies();
    }
    
    public List<Track> getQueue() {
        return audioPlayerService.getQueue();
    }
    
    public void clearQueue() {
        audioPlayerService.clearQueue();
    }
    
    public void addToQueue(Track track) {
        audioPlayerService.addToQueue(track);
        System.out.println("➕ Added to queue: " + track.getArtist() + " - " + track.getTitle());
    }
    
    public void removeFromQueue(int index) {
        audioPlayerService.removeFromQueue(index);
    }
    
    public void moveTrackUp(int index) {
        audioPlayerService.moveTrackUp(index);
    }
    
    public void moveTrackDown(int index) {
        audioPlayerService.moveTrackDown(index);
    }
    
    public int getCurrentQueueIndex() {
        return audioPlayerService.getCurrentIndex();
    }
    
    public long createPlaylist(String name, String description) {
        try {
            com.musicplayer.model.Playlist playlist = new com.musicplayer.model.Playlist(name, description);
            long playlistId = dbManager.createPlaylist(playlist);
            System.out.println("✅ Playlist created via controller: " + name);
            return playlistId;
        } catch (Exception e) {
            System.err.println("❌ Error creating playlist: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }
    
    public List<com.musicplayer.model.Playlist> getAllPlaylists() {
        try {
            return dbManager.getAllPlaylists();
        } catch (Exception e) {
            System.err.println("❌ Error getting playlists: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    public com.musicplayer.model.Playlist getPlaylist(long playlistId) {
        try {
            return dbManager.getPlaylist(playlistId);
        } catch (Exception e) {
            System.err.println("❌ Error getting playlist: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    public boolean updatePlaylist(com.musicplayer.model.Playlist playlist) {
        try {
            dbManager.updatePlaylist(playlist);
            System.out.println("✅ Playlist updated: " + playlist.getName());
            return true;
        } catch (Exception e) {
            System.err.println("❌ Error updating playlist: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean deletePlaylist(long playlistId) {
        try {
            dbManager.deletePlaylist(playlistId);
            System.out.println("🗑️ Playlist deleted");
            return true;
        } catch (Exception e) {
            System.err.println("❌ Error deleting playlist: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean addTrackToPlaylist(long playlistId, Track track) {
        try {
            dbManager.addTrackToPlaylist(playlistId, track);
            System.out.println("➕ Added track to playlist: " + track.getTitle());
            return true;
        } catch (Exception e) {
            System.err.println("❌ Error adding track to playlist: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean removeTrackFromPlaylist(long playlistId, String trackId) {
        try {
            System.out.println("🔍 Controller: Removing track");
            System.out.println("   Playlist ID: " + playlistId);
            System.out.println("   Track ID: " + trackId);

            dbManager.removeTrackFromPlaylist(playlistId, trackId);

            System.out.println("✅ Controller: Track removed successfully");
            return true;

        } catch (Exception e) {
            System.err.println("❌ Controller: Error removing track: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public List<Track> getPlaylistTracks(long playlistId) {
        try {
            return dbManager.getPlaylistTracks(playlistId);
        } catch (Exception e) {
            System.err.println("❌ Error getting playlist tracks: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    public void playPlaylist(com.musicplayer.model.Playlist playlist) {
        if (playlist == null || playlist.getTracks().isEmpty()) {
            System.out.println("⚠️ Playlist is empty");
            return;
        }

        List<Track> tracks = playlist.getTracks();
        setQueueAndPlay(tracks, 0);
        System.out.println("▶️ Playing playlist: " + playlist.getName() + " (" + tracks.size() + " tracks)");
    }
    
    public void playPlaylistFromTrack(com.musicplayer.model.Playlist playlist, int trackIndex) {
        if (playlist == null || playlist.getTracks().isEmpty()) {
            System.out.println("⚠️ Playlist is empty");
            return;
        }

        List<Track> tracks = playlist.getTracks();
        if (trackIndex >= 0 && trackIndex < tracks.size()) {
            setQueueAndPlay(tracks, trackIndex);
            System.out.println("▶️ Playing from playlist: " + playlist.getName() + " (track " + (trackIndex + 1) + ")");
        }
    }
    
    public LyricsService getLyricsService() {
        return lyricsService;
    }

    public AnalyticsDAO getAnalyticsDAO() {
        return analyticsDAO;
    }

    public void shutdown() {
        System.out.println("🛑 Shutting down controller...");
        enrichmentService.shutdown();
        audioPlayerService.release();
        dbManager.close();
        AnalyticsDatabaseManager.getInstance().close();
        System.out.println("✅ Controller shutdown complete");
    }
}