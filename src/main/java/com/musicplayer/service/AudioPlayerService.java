package com.musicplayer.service;

import com.musicplayer.model.Track;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;



import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

// Service utk play audio
public class AudioPlayerService {

    private MediaPlayerFactory mediaPlayerFactory;
    private EmbeddedMediaPlayer mediaPlayer;
    private YouTubeMusicService youtubeService;

    private Track currentTrack;
    private List<Track> queue;
    private int currentIndex;
    private boolean isPlaying;
    private boolean isRepeat;
    private boolean isShuffle;

    private List<PlayerStateListener> listeners;
    private volatile boolean isProcessingNext = false;
    private volatile int retryCount = 0;
    private static final int MAX_RETRIES = 2; // Maximum retry attempts
    private uk.co.caprica.vlcj.player.base.Equalizer equalizer; // VLC Equalizer object
    private boolean equalizerEnabled = false;
    private float preamp = 0.0f;
    private float[] bandAmplitudes = new float[10];
    
    private static final String[] BAND_FREQUENCIES = {
        "60 Hz", "170 Hz", "310 Hz", "600 Hz", "1 kHz",
        "3 kHz", "6 kHz", "12 kHz", "14 kHz", "16 kHz"
    };

    public AudioPlayerService() {
        try {
            // Check VLC installation first
            System.out.println("🔍 Checking VLC installation...");

            // Try native discovery
            uk.co.caprica.vlcj.factory.discovery.NativeDiscovery discovery = new uk.co.caprica.vlcj.factory.discovery.NativeDiscovery();
            boolean vlcFound = discovery.discover();

            if (!vlcFound) {
                System.err.println("⚠️ VLC not found via native discovery");
                System.err.println("   Please install VLC Media Player from https://www.videolan.org/");
            } else {
                System.out.println("✅ VLC found");
            }

            this.mediaPlayerFactory = new MediaPlayerFactory();
            this.mediaPlayer = mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();
            System.out.println("✅ Media player initialized");

        } catch (Exception e) {
            System.err.println("❌ Failed to initialize audio player: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Cannot initialize audio player. Please install VLC Media Player.", e);
        }

        this.youtubeService = new YouTubeMusicService();
        this.queue = new ArrayList<>();
        this.currentIndex = -1;
        this.isPlaying = false;
        this.listeners = new ArrayList<>();
        
        initializeEqualizer();

        setupPlayerListeners();
    }
    
    // Equalizer
    private void initializeEqualizer() {
        try {
            // Create equalizer using factory
            equalizer = mediaPlayerFactory.equalizer().newEqualizer();

            if (equalizer != null) {
                // Initialize all bands to 0
                int bandCount = equalizer.bandCount();
                System.out.println("✅ Equalizer created with " + bandCount + " bands");

                for (int i = 0; i < Math.min(10, bandCount); i++) {
                    bandAmplitudes[i] = 0.0f;
                    equalizer.setAmp(i, 0.0f);
                    System.out.println("   Band " + i + ": " + " Hz");
                }

                preamp = 0.0f;
                equalizer.setPreamp(0.0f);

                System.out.println("✅ Equalizer initialized");
            } else {
                System.err.println("⚠️ Could not create equalizer");
            }
        } catch (Exception e) {
            System.err.println("❌ Equalizer init error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void setEqualizerEnabled(boolean enabled) {
        this.equalizerEnabled = enabled;

        if (equalizer == null) {
            System.err.println("⚠️ Equalizer not initialized");
            return;
        }

        if (enabled) {
            applyEqualizerSettings();
            System.out.println("✅ Equalizer ENABLED");
        } else {
            mediaPlayer.audio().setEqualizer(null);
            System.out.println("❌ Equalizer DISABLED");
        }
    }
    
    public boolean isEqualizerEnabled() {
        return equalizerEnabled;
    }
    
    public void setPreamp(float value) {
        this.preamp = Math.max(-20.0f, Math.min(20.0f, value));

        if (equalizerEnabled) {
            applyEqualizerSettings();
        }

        System.out.println("🎚️ Preamp set to: " + this.preamp + " dB");
    }
    
    public float getPreamp() {
        return preamp;
    }
    
    public void setBandAmplitude(int bandIndex, float amplitude) {
        if (bandIndex < 0 || bandIndex >= bandAmplitudes.length) {
            System.err.println("⚠️ Invalid band index: " + bandIndex);
            return;
        }

        this.bandAmplitudes[bandIndex] = Math.max(-20.0f, Math.min(20.0f, amplitude));

        if (equalizerEnabled) {
            applyEqualizerSettings();
        }

        System.out.println("🎛️ Band " + bandIndex + " (" + BAND_FREQUENCIES[bandIndex] + ") set to: " + this.bandAmplitudes[bandIndex] + " dB");
    }
    
    public float getBandAmplitude(int bandIndex) {
        if (bandIndex < 0 || bandIndex >= bandAmplitudes.length) {
            return 0.0f;
        }
        return bandAmplitudes[bandIndex];
    }
    
    public float[] getAllBandAmplitudes() {
        return bandAmplitudes.clone();
    }
    
    public void setAllBandAmplitudes(float[] amplitudes) {
        if (amplitudes == null || amplitudes.length != 10) {
            System.err.println("⚠️ Invalid amplitudes array");
            return;
        }

        for (int i = 0; i < amplitudes.length; i++) {
            bandAmplitudes[i] = Math.max(-20.0f, Math.min(20.0f, amplitudes[i]));
        }

        if (equalizerEnabled) {
            applyEqualizerSettings();
        }

        System.out.println("🎛️ All bands updated");
    }

    private void applyEqualizerSettings() {
        if (!equalizerEnabled || equalizer == null) {
            return;
        }

        try {
            // Set preamp
            equalizer.setPreamp(preamp);
            System.out.println("🎚️ Setting preamp: " + preamp + " dB");

            // Set all bands
            int maxBands = Math.min(bandAmplitudes.length, equalizer.bandCount());
            for (int i = 0; i < maxBands; i++) {
                equalizer.setAmp(i, bandAmplitudes[i]);
                System.out.println("   Band " + i + " (" + BAND_FREQUENCIES[i] + "): " + bandAmplitudes[i] + " dB");
            }
            
            mediaPlayer.audio().setEqualizer(equalizer);
            System.out.println("✅ Equalizer settings applied");
        } catch (Exception e) {
            System.err.println("❌ Failed to apply equalizer: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void resetEqualizer() {
        for (int i = 0; i < bandAmplitudes.length; i++) {
            bandAmplitudes[i] = 0.0f;
        }
        preamp = 0.0f;
        
        if (equalizerEnabled) {
            applyEqualizerSettings();
        }
        
        System.out.println("🔄 Equalizer reset to flat");
    }
    
    public void loadPreset(String presetName) {
        float[] preset = getPresetValues(presetName);
        if (preset != null) {
            setAllBandAmplitudes(preset);
            System.out.println("🎵 Loaded preset: " + presetName);
        }
    }
    
    private float[] getPresetValues(String presetName) {
        switch (presetName.toLowerCase()) {
            case "flat":
                return new float[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

            case "rock":
                return new float[]{5, 3, -3, -5, -2, 2, 5, 7, 7, 7};

            case "pop":
                return new float[]{-1, 3, 5, 5, 3, 0, -1, -1, -1, -1};

            case "jazz":
                return new float[]{4, 3, 1, 2, -2, -2, 0, 2, 4, 5};

            case "classical":
                return new float[]{5, 4, 3, 2, -1, -1, 0, 2, 4, 5};

            case "bass boost":
                return new float[]{8, 6, 4, 2, 0, 0, 0, 0, 0, 0};

            case "treble boost":
                return new float[]{0, 0, 0, 0, 0, 2, 4, 6, 8, 9};

            case "vocal boost":
                return new float[]{-2, -2, -1, 1, 4, 4, 3, 1, 0, -1};

            default:
                System.err.println("⚠️ Unknown preset: " + presetName);
                return null;
        }
    }
    
    public String[] getAvailablePresets() {
        return new String[]{
            "Flat", "Rock", "Pop", "Jazz", "Classical", 
            "Bass Boost", "Treble Boost", "Vocal Boost"
        };
    }
    
    public String[] getBandFrequencies() {
        return BAND_FREQUENCIES.clone();
    }
    
    public void debugEqualizer() {
        System.out.println("\n🔍 ===== EQUALIZER DEBUG =====");
        System.out.println("Enabled: " + equalizerEnabled);
        System.out.println("Equalizer object: " + (equalizer != null ? "EXISTS" : "NULL"));

        if (equalizer != null) {
            System.out.println("Preamp: " + equalizer.preamp() + " dB");
            System.out.println("Band count: " + equalizer.bandCount());

            for (int i = 0; i < Math.min(10, equalizer.bandCount()); i++) {
                System.out.println("  Band " + i + " (" + BAND_FREQUENCIES[i] + "): " + 
                                 equalizer.amp(i) + " dB (stored: " + bandAmplitudes[i] + " dB)");
            }

            // Check if equalizer is applied to player
            uk.co.caprica.vlcj.player.base.Equalizer currentEq = 
                mediaPlayer.audio().equalizer();
            System.out.println("Applied to player: " + (currentEq != null ? "YES" : "NO"));
        }
        System.out.println("===========================\n");
    }

    private void setupPlayerListeners() {
        mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void playing(MediaPlayer mediaPlayer) {
                isPlaying = true;
                SwingUtilities.invokeLater(() -> notifyStateChanged());
            }

            @Override
            public void paused(MediaPlayer mediaPlayer) {
                isPlaying = false;
                SwingUtilities.invokeLater(() -> notifyStateChanged());
            }

            @Override
            public void stopped(MediaPlayer mediaPlayer) {
                isPlaying = false;
                SwingUtilities.invokeLater(() -> notifyStateChanged());
            }

            @Override
            public void finished(MediaPlayer mediaPlayer) {
                // CRITICAL FIX: Run in background thread to prevent freeze
                if (!isProcessingNext) {
                    isProcessingNext = true;
                    new Thread(() -> {
                        try {
                            Thread.sleep(500); // Small delay
                            playNext();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            isProcessingNext = false;
                        }
                    }).start();
                }
            }

            @Override
            public void error(MediaPlayer mediaPlayer) {
                System.err.println("❌ Player error occurred");

                // Limit retry attempts
                if (retryCount >= MAX_RETRIES) {
                    System.err.println("❌ Max retries reached, skipping to next track");
                    retryCount = 0;
                    SwingUtilities.invokeLater(() -> {
                        notifyError("Playback failed after " + MAX_RETRIES + " attempts");
                        // Skip to next track
                        Timer skipTimer = new Timer(1000, e -> playNext());
                        skipTimer.setRepeats(false);
                        skipTimer.start();
                    });
                    return;
                }

                retryCount++;
                System.err.println("🔄 Retry attempt " + retryCount + "/" + MAX_RETRIES);

                SwingUtilities.invokeLater(() -> {
                    if (currentTrack != null) {
                        System.out.println("🔄 Attempting to refresh stream URL...");
                        refreshStreamAndRetry();
                    }
                });
            }

            @Override
            public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
                // Don't notify every time to reduce UI updates
                if (newTime % 1000 < 100) { // Every second approximately
                    SwingUtilities.invokeLater(() -> notifyTimeChanged(newTime));
                }
            }
        });
    }

    private void refreshStreamAndRetry() {
        new Thread(() -> {
            try {
                String newUrl = youtubeService.getStreamUrl(currentTrack.getYoutubeId());
                if (newUrl != null) {
                    SwingUtilities.invokeLater(() -> {
                        mediaPlayer.media().play(newUrl);
                    });
                } else {
                    // Skip to next if refresh fails
                    playNext();
                }
            } catch (Exception e) {
                e.printStackTrace();
                playNext();
            }
        }).start();
    }

    public void playTrack(Track track) {
        if (track == null || track.getYoutubeId() == null) {
            System.err.println("❌ Invalid track or missing YouTube ID");
            notifyError("Invalid track");
            return;
        }

        // Reset retry counter for new track
        retryCount = 0;

        // Stop current playback (non-blocking)
        if (isPlaying) {
            mediaPlayer.controls().stop();
        }

        currentTrack = track;
        SwingUtilities.invokeLater(() -> notifyTrackChanged(track));

        // Fetch stream URL in background thread
        new Thread(() -> {
            try {
                System.out.println("🔄 Fetching stream for: " + track.getArtist() + " - " + track.getTitle());

                String streamUrl = youtubeService.getStreamUrl(track.getYoutubeId());

                if (streamUrl != null && !streamUrl.isEmpty()) {
                    System.out.println("📡 Stream URL obtained: " + streamUrl.substring(0, Math.min(100, streamUrl.length())) + "...");

                    // Play in EDT to prevent freeze
                    SwingUtilities.invokeLater(() -> {
                        try {
                            System.out.println("▶️ Starting playback...");
                            boolean success = mediaPlayer.media().play(streamUrl);

                            if (success) {
                                System.out.println("✅ Playing: " + track.getArtist() + " - " + track.getTitle());
                                retryCount = 0; // Reset on success
                                Timer equalizerTimer = new Timer(500, e -> {
                                    if (equalizerEnabled && equalizer != null) {
                                        try {
                                            mediaPlayer.audio().setEqualizer(equalizer); // ✅ Fixed - void method
                                            System.out.println("🎛️ Equalizer reapplied to new track");
                                        } catch (Exception ex) {
                                            System.err.println("⚠️ Failed to reapply equalizer: " + ex.getMessage());
                                        }
                                    }
                                });
                                equalizerTimer.setRepeats(false);
                                equalizerTimer.start();
                                equalizerTimer.setRepeats(false);
                                equalizerTimer.start();
                            } else {
                                System.err.println("❌ Failed to start playback (play() returned false)");
                                notifyError("Failed to start playback");
                                // Auto skip if queue has more tracks
                                if (queue.size() > 1) {
                                    Timer timer = new Timer(2000, e -> playNext());
                                    timer.setRepeats(false);
                                    timer.start();
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("❌ Exception during playback: " + e.getMessage());
                            e.printStackTrace();
                            notifyError("Playback error");
                        }
                    });
                } else {
                    System.err.println("❌ Could not get stream URL for: " + track.getTitle());
                    SwingUtilities.invokeLater(() -> {
                        notifyError("Could not get stream URL");
                        // Auto skip after delay
                        if (queue.size() > 1) {
                            Timer timer = new Timer(2000, e -> playNext());
                            timer.setRepeats(false);
                            timer.start();
                        }
                    });
                }
            } catch (Exception e) {
                System.err.println("❌ Error playing track: " + e.getMessage());
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    notifyError("Error: " + e.getMessage());
                    // Auto skip
                    if (queue.size() > 1) {
                        Timer timer = new Timer(2000, e2 -> playNext());
                        timer.setRepeats(false);
                        timer.start();
                    }
                });
            }
        }).start();
    }

    public void play() {
        if (currentTrack == null && !queue.isEmpty()) {
            playTrack(queue.get(0));
        } else {
            mediaPlayer.controls().play();
        }
    }

    public void pause() {
        mediaPlayer.controls().pause();
    }

    public void stop() {
        mediaPlayer.controls().stop();
        currentTrack = null;
        SwingUtilities.invokeLater(() -> notifyTrackChanged(null));
    }

    public void togglePlayPause() {
        if (isPlaying) {
            pause();
        } else {
            play();
        }
    }

    public void playNext() {
        if (queue.isEmpty()) {
            return;
        }

        if (isShuffle) {
            currentIndex = (int) (Math.random() * queue.size());
        } else {
            currentIndex++;
            if (currentIndex >= queue.size()) {
                if (isRepeat) {
                    currentIndex = 0;
                } else {
                    stop();
                    return;
                }
            }
        }

        playTrack(queue.get(currentIndex));
    }

    public void playPrevious() {
        if (queue.isEmpty()) {
            return;
        }

        currentIndex--;
        if (currentIndex < 0) {
            currentIndex = queue.size() - 1;
        }

        playTrack(queue.get(currentIndex));
    }

    public void seekTo(long seconds) {
        mediaPlayer.controls().setTime(seconds * 1000);
    }
    
    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setVolume(int volume) {
        int normalizedVolume = Math.max(0, Math.min(100, volume));
        mediaPlayer.audio().setVolume(normalizedVolume);
    }

    public int getVolume() {
        return mediaPlayer.audio().volume();
    }

    public long getCurrentTime() {
        return mediaPlayer.status().time() / 1000;
    }

    public long getDuration() {
        return mediaPlayer.status().length() / 1000;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public Track getCurrentTrack() {
        return currentTrack;
    }

    public void setQueueAndPlay(List<Track> tracks, int startIndex) {
        this.queue = new ArrayList<>(tracks);
        this.currentIndex = startIndex;
        if (!queue.isEmpty() && startIndex >= 0 && startIndex < queue.size()) {
            playTrack(queue.get(startIndex));
        }
    }

    public void addToQueue(Track track) {
        queue.add(track);
        if (queue.size() == 1) {
            currentIndex = 0;
            playTrack(track);
        }
    }
    
    public List<Track> getQueueMutable() {
        return queue; // Return actual list, not copy
    }
    
    public void swapTracksInQueue(int index1, int index2) {
        if (index1 < 0 || index1 >= queue.size() || 
            index2 < 0 || index2 >= queue.size()) {
            return;
        }

        // Swap tracks
        Track temp = queue.get(index1);
        queue.set(index1, queue.get(index2));
        queue.set(index2, temp);

        // Update current index if needed
        if (currentIndex == index1) {
            currentIndex = index2;
        } else if (currentIndex == index2) {
            currentIndex = index1;
        }

        System.out.println("🔄 Swapped queue positions: " + index1 + " ↔ " + index2);
    }
    
    public void moveTrackUp(int index) {
        if (index > 0 && index < queue.size()) {
            swapTracksInQueue(index, index - 1);
        }
    }
    
    public void moveTrackDown(int index) {
        if (index >= 0 && index < queue.size() - 1) {
            swapTracksInQueue(index, index + 1);
        }
    }

    public void clearQueue() {
        queue.clear();
        currentIndex = -1;
    }
    
    public void removeFromQueue(int index) {
        if (index < 0 || index >= queue.size()) {
            System.out.println("⚠️ Invalid index: " + index + " (queue size: " + queue.size() + ")");
            return;
        }

        Track removedTrack = queue.get(index);
        queue.remove(index);

        // Adjust current index if needed
        if (index < currentIndex) {
            // Track before current was removed
            currentIndex--;
        } else if (index == currentIndex) {
            // Current track was removed
            if (queue.isEmpty()) {
                stop();
                currentIndex = -1;
            } else {
                // Play next track (which is now at same index)
                if (currentIndex >= queue.size()) {
                    currentIndex = queue.size() - 1;
                }
            }
        }

        System.out.println("✅ Removed from queue: " + removedTrack.getTitle() + " (index: " + index + ", new queue size: " + queue.size() + ", current index: " + currentIndex + ")");
    }

    public List<Track> getQueue() {
        return new ArrayList<>(queue);
    }

    public void toggleRepeat() {
        isRepeat = !isRepeat;
    }

    public void toggleShuffle() {
        isShuffle = !isShuffle;
    }

    public boolean isRepeat() {
        return isRepeat;
    }

    public boolean isShuffle() {
        return isShuffle;
    }

    public void addPlayerStateListener(PlayerStateListener listener) {
        listeners.add(listener);
    }

    public void removePlayerStateListener(PlayerStateListener listener) {
        listeners.remove(listener);
    }

    private void notifyStateChanged() {
        for (PlayerStateListener listener : listeners) {
            listener.onPlayerStateChanged(isPlaying);
        }
    }

    private void notifyTrackChanged(Track track) {
        for (PlayerStateListener listener : listeners) {
            listener.onTrackChanged(track);
        }
    }

    private void notifyTimeChanged(long timeMs) {
        for (PlayerStateListener listener : listeners) {
            listener.onTimeChanged(timeMs / 1000);
        }
    }

    private void notifyError(String message) {
        for (PlayerStateListener listener : listeners) {
            listener.onError(message);
        }
    }

    // Safe shutdown biar ga error
    public void release() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.controls().stop();
                mediaPlayer.release();
            } if (mediaPlayerFactory != null) {
                mediaPlayerFactory.release();
            }
        } catch (Exception e) {
            System.err.println("Error releasing player: " + e.getMessage());
        }
    }

    public interface PlayerStateListener {
        void onPlayerStateChanged(boolean isPlaying);
        void onTrackChanged(Track track);
        void onTimeChanged(long seconds);

        default void onError(String message) {
            System.out.println("Something Wrong Happened");
        }
    }
}