package com.musicplayer.view;

import com.musicplayer.controller.MusicPlayerController;
import com.musicplayer.model.Track;
import com.musicplayer.util.IconLoader;
import com.musicplayer.util.ImageUtils;
import com.musicplayer.util.TimeFormatter;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public class NowPlayingPanel extends JPanel {
    private MusicPlayerController controller;
    private JLabel albumArtLabel;
    private JLabel titleLabel;
    private JLabel artistLabel;
    private JLabel albumLabel;
    private JSlider seekBar;
    private JLabel currentTimeLabel;
    private JLabel durationLabel;
    private JButton playPauseButton;
    private JButton previousButton;
    private JButton nextButton;
    private JButton shuffleButton;
    private JButton repeatButton;
    private JButton lyricsToggleButton;
    
    private LyricsPanel lyricsPanel;
    private JSplitPane splitPane;
    private boolean lyricsVisible = true;
    
    private boolean isDraggingSeekBar = false;
    private Timer updateTimer;

    public NowPlayingPanel(MusicPlayerController controller) {
        this.controller = controller;
        initializeComponents();
        setupUpdateTimer();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());
        setBackground(new Color(30, 30, 35));
        
        // Create split pane
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(650);
        splitPane.setDividerSize(2);
        splitPane.setBorder(null);
        splitPane.setBackground(new Color(30, 30, 35));
        
        // Left: Player controls
        JPanel playerPanel = createPlayerPanel();
        splitPane.setLeftComponent(playerPanel);
        
        // Right: Lyrics
        lyricsPanel = new LyricsPanel();
        splitPane.setRightComponent(lyricsPanel);
        
        add(splitPane, BorderLayout.CENTER);
    }
    
    private JPanel createPlayerPanel() {
        JPanel panel = new JPanel(new MigLayout("fill, insets 40", "[center]", "[][][][]30[]20[]10[]"));
        panel.setBackground(new Color(30, 30, 35));

        // Album art
        albumArtLabel = new JLabel();
        albumArtLabel.setPreferredSize(new Dimension(300, 300));
        albumArtLabel.setHorizontalAlignment(SwingConstants.CENTER);
        albumArtLabel.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 65), 1));
        albumArtLabel.setOpaque(true);
        albumArtLabel.setBackground(new Color(45, 45, 50));
        albumArtLabel.setIcon(ImageUtils.getPlaceholderIcon(300, 300));
        panel.add(albumArtLabel, "wrap, center, gapbottom 30");

        // Title
        titleLabel = new JLabel("No track playing");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(titleLabel, "wrap, center, gapbottom 10");

        // Artist
        artistLabel = new JLabel("");
        artistLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        artistLabel.setForeground(new Color(180, 180, 180));
        artistLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(artistLabel, "wrap, center, gapbottom 5");

        // Album
        albumLabel = new JLabel("");
        albumLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        albumLabel.setForeground(new Color(150, 150, 150));
        albumLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(albumLabel, "wrap, center, gapbottom 30");

        // Seek bar with time labels
        JPanel seekPanel = new JPanel(new MigLayout("fillx, insets 0", "[]10[grow]10[]", ""));
        seekPanel.setOpaque(false);

        currentTimeLabel = new JLabel("0:00");
        currentTimeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        currentTimeLabel.setForeground(new Color(150, 150, 150));
        seekPanel.add(currentTimeLabel);

        seekBar = new JSlider(0, 100, 0);
        seekBar.setOpaque(false);
        seekBar.addChangeListener(e -> {
            if (seekBar.getValueIsAdjusting()) {
                isDraggingSeekBar = true;
            } else if (isDraggingSeekBar) {
                // User finished dragging, seek to position
                long totalDuration = controller.getPlayerDuration();
                if (totalDuration > 0) {
                    long seekPosition = (long) ((seekBar.getValue() / 100.0) * totalDuration);
                    controller.seekTo(seekPosition);
                }
                isDraggingSeekBar = false;
            }
        });
        seekPanel.add(seekBar, "growx");

        durationLabel = new JLabel("0:00");
        durationLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        durationLabel.setForeground(new Color(150, 150, 150));
        seekPanel.add(durationLabel);

        panel.add(seekPanel, "wrap, growx, width 60%");

        // Playback controls
        JPanel controlsPanel = new JPanel(new MigLayout("insets 0", "[]10[]10[]10[]10[]", ""));
        controlsPanel.setOpaque(false);

        shuffleButton = new JButton(IconLoader.loadButtonIcon(IconLoader.Icons.SHUFFLE));
        shuffleButton.setToolTipText("Shuffle");
        shuffleButton.setFocusPainted(false);
        shuffleButton.setBorderPainted(false);
        shuffleButton.setContentAreaFilled(false);
        shuffleButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        shuffleButton.addActionListener(e -> toggleShuffle());
        controlsPanel.add(shuffleButton);

        previousButton = new JButton(IconLoader.loadButtonIcon(IconLoader.Icons.PREVIOUS));
        previousButton.setToolTipText("Previous");
        previousButton.setFocusPainted(false);
        previousButton.setBorderPainted(false);
        previousButton.setContentAreaFilled(false);
        previousButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        previousButton.addActionListener(e -> controller.playPrevious());
        controlsPanel.add(previousButton);

        playPauseButton = new JButton(IconLoader.loadButtonIcon(
            controller.isPlayerPlaying() ? IconLoader.Icons.PAUSE : IconLoader.Icons.PLAY));
        playPauseButton.setToolTipText("Play / Pause");
        playPauseButton.setFocusPainted(false);
        playPauseButton.setBorderPainted(false);
        playPauseButton.setContentAreaFilled(false);
        playPauseButton.setPreferredSize(new Dimension(60, 60));
        playPauseButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        playPauseButton.addActionListener(e -> controller.togglePlayPause());
        controlsPanel.add(playPauseButton);

        nextButton = new JButton(IconLoader.loadButtonIcon(IconLoader.Icons.NEXT));
        nextButton.setToolTipText("Next");
        nextButton.setFocusPainted(false);
        nextButton.setBorderPainted(false);
        nextButton.setContentAreaFilled(false);
        nextButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        nextButton.addActionListener(e -> controller.playNext());
        controlsPanel.add(nextButton);

        repeatButton = new JButton(IconLoader.loadButtonIcon(IconLoader.Icons.REPEAT));
        repeatButton.setToolTipText("Repeat");
        repeatButton.setFocusPainted(false);
        repeatButton.setBorderPainted(false);
        repeatButton.setContentAreaFilled(false);
        repeatButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        repeatButton.addActionListener(e -> toggleRepeat());
        controlsPanel.add(repeatButton);

        panel.add(controlsPanel, "wrap, center");
        
        // Lyrics toggle button
        lyricsToggleButton = new JButton("🎤 Hide Lyrics");
        lyricsToggleButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lyricsToggleButton.setForeground(new Color(180, 180, 180));
        lyricsToggleButton.setBackground(new Color(45, 45, 50));
        lyricsToggleButton.setFocusPainted(false);
        lyricsToggleButton.setBorderPainted(false);
        lyricsToggleButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lyricsToggleButton.addActionListener(e -> toggleLyrics());
        
        // Hover effect
        lyricsToggleButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                lyricsToggleButton.setBackground(new Color(60, 60, 65));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                lyricsToggleButton.setBackground(new Color(45, 45, 50));
            }
        });
        
        panel.add(lyricsToggleButton, "center");
        
        return panel;
    }

    private void setupUpdateTimer() {
        // Update UI setiap 500ms
        updateTimer = new Timer(500, e -> updatePlaybackUI());
        updateTimer.start();
    }

    private void updatePlaybackUI() {
        if (!isDraggingSeekBar) {
            long currentTime = controller.getPlayerCurrentTime();
            long duration = controller.getPlayerDuration();

            if (duration > 0) {
                int progress = (int) ((currentTime * 100.0) / duration);
                seekBar.setValue(progress);
                currentTimeLabel.setText(TimeFormatter.formatDuration((int) currentTime));
                durationLabel.setText(TimeFormatter.formatDuration((int) duration));
            }
        }

        // Update play/pause button
        boolean isPlaying = controller.isPlayerPlaying();
        playPauseButton.setIcon(
            IconLoader.loadButtonIcon(
                isPlaying ? IconLoader.Icons.PAUSE : IconLoader.Icons.PLAY
            )
        );
        playPauseButton.setText(null);

        // Update shuffle/repeat buttons
        updateShuffleButton();
        updateRepeatButton();
    }

    public void updateNowPlaying(Track track) {
        if (track != null) {
            titleLabel.setText(track.getTitle());
            artistLabel.setText(track.getArtist());
            albumLabel.setText(track.getAlbum() != null ? track.getAlbum() : "");

            // Load album art
            if (track.getThumbnailUrl() != null && !track.getThumbnailUrl().isEmpty()) {
                new Thread(() -> {
                    ImageIcon thumbnail = ImageUtils.loadImageFromUrl(track.getThumbnailUrl(), 300, 300);
                    SwingUtilities.invokeLater(() -> albumArtLabel.setIcon(thumbnail));
                }).start();
            } else {
                albumArtLabel.setIcon(ImageUtils.getPlaceholderIcon(300, 300));
            }

            // Reset seek bar
            seekBar.setValue(0);
            currentTimeLabel.setText("0:00");
            
            // Update lyrics panel
            if (lyricsPanel != null) {
                lyricsPanel.updateTrack(track);
            }
            
        } else {
            titleLabel.setText("No track playing");
            artistLabel.setText("");
            albumLabel.setText("");
            albumArtLabel.setIcon(ImageUtils.getPlaceholderIcon(300, 300));
            seekBar.setValue(0);
            currentTimeLabel.setText("0:00");
            durationLabel.setText("0:00");
            
            // Clear lyrics
            if (lyricsPanel != null) {
                lyricsPanel.clearLyrics();
            }
        }
    }

    private void toggleShuffle() {
        controller.toggleShuffle();
        updateShuffleButton();
    }

    private void toggleRepeat() {
        controller.toggleRepeat();
        updateRepeatButton();
    }
    
    private void toggleLyrics() {
        lyricsVisible = !lyricsVisible;
        
        if (lyricsVisible) {
            splitPane.setRightComponent(lyricsPanel);
            splitPane.setDividerLocation(650);
            lyricsToggleButton.setText("🎤 Hide Lyrics");
        } else {
            splitPane.setRightComponent(null);
            lyricsToggleButton.setText("🎤 Show Lyrics");
        }
        
        splitPane.revalidate();
        splitPane.repaint();
    }

    private void updateShuffleButton() {
        shuffleButton.setIcon(IconLoader.loadButtonIcon(
            controller.isShuffleEnabled()
                ? IconLoader.Icons.SHUFFLE_ON
                : IconLoader.Icons.SHUFFLE
        ));
    }

    private void updateRepeatButton() {
        repeatButton.setIcon(IconLoader.loadButtonIcon(
            controller.isRepeatEnabled()
                ? IconLoader.Icons.REPEAT_ON
                : IconLoader.Icons.REPEAT
        ));
    }
    
    /**
     * Get lyrics panel reference
     */
    public LyricsPanel getLyricsPanel() {
        return lyricsPanel;
    }
}