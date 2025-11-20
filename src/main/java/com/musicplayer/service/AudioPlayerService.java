package com.musicplayer.service;

import com.musicplayer.model.Track;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

// Service utk play audio
public class AudioPlayerService {

    private AudioPlayerComponent audioPlayer;
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

    public AudioPlayerService() {
        try {
            // Check VLC installation first
            System.out.println("🔍 Checking VLC installation...");

            // Try native discovery
            uk.co.caprica.vlcj.factory.discovery.NativeDiscovery discovery =
                    new uk.co.caprica.vlcj.factory.discovery.NativeDiscovery();
            boolean vlcFound = discovery.discover();

            if (!vlcFound) {
                System.err.println("⚠️ VLC not found via native discovery");
                System.err.println("   Please install VLC Media Player from https://www.videolan.org/");
            } else {
                System.out.println("✅ VLC found");
            }

            this.audioPlayer = new AudioPlayerComponent();
            System.out.println("✅ Audio player component initialized");

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

        setupPlayerListeners();
    }

    private void setupPlayerListeners() {
        audioPlayer.mediaPlayer().events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
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
                        audioPlayer.mediaPlayer().media().play(newUrl);
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
            audioPlayer.mediaPlayer().controls().stop();
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
                            boolean success = audioPlayer.mediaPlayer().media().play(streamUrl);

                            if (success) {
                                System.out.println("✅ Playing: " + track.getArtist() + " - " + track.getTitle());
                                retryCount = 0; // Reset on success
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
            audioPlayer.mediaPlayer().controls().play();
        }
    }

    public void pause() {
        audioPlayer.mediaPlayer().controls().pause();
    }

    public void stop() {
        audioPlayer.mediaPlayer().controls().stop();
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
        audioPlayer.mediaPlayer().controls().setTime(seconds * 1000);
    }

    public void setVolume(int volume) {
        int normalizedVolume = Math.max(0, Math.min(100, volume));
        audioPlayer.mediaPlayer().audio().setVolume(normalizedVolume);
    }

    public int getVolume() {
        return audioPlayer.mediaPlayer().audio().volume();
    }

    public long getCurrentTime() {
        return audioPlayer.mediaPlayer().status().time() / 1000;
    }

    public long getDuration() {
        return audioPlayer.mediaPlayer().status().length() / 1000;
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

    public void clearQueue() {
        queue.clear();
        currentIndex = -1;
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
            if (audioPlayer != null) {
                audioPlayer.mediaPlayer().controls().stop();
                audioPlayer.release();
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