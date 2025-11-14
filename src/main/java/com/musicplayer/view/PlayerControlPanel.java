package com.musicplayer.view;

import com.musicplayer.controller.MusicPlayerController;
import com.musicplayer.model.Track;
import com.musicplayer.util.TimeFormatter;
import net.miginfocom.swing.MigLayout;
import com.musicplayer.util.IconLoader;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

// Player control di bawah
public class PlayerControlPanel extends JPanel {
    private MusicPlayerController controller;
    private JButton playPauseButton;
    private JButton previousButton;
    private JButton nextButton;
    private JButton queueButton;
    private JSlider volumeSlider;
    private JLabel currentTrackLabel;
    private JLabel timeLabel;
    private JLabel volumeLabel;

    private Timer updateTimer;
    private boolean isVolumeUpdating = false;

    public PlayerControlPanel(MusicPlayerController controller) {
        this.controller = controller;
        initializeComponents();
        setupUpdateTimer();
    }

    private void initializeComponents() {
        setLayout(new MigLayout("fillx, insets 15", "[grow][]10[]10[]10[]push[]10[]10[]10[]", ""));
        setBackground(new Color(25, 25, 28));
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(50, 50, 55)));

        // Current track info
        currentTrackLabel = new JLabel("No track playing");
        currentTrackLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        currentTrackLabel.setForeground(Color.WHITE);
        add(currentTrackLabel);

        // Time label
        timeLabel = new JLabel("0:00 / 0:00");
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        timeLabel.setForeground(new Color(150, 150, 150));
        add(timeLabel, "gapleft 15");

        // Ganti tombol-tombol control dengan ikon dari IconLoader
        previousButton = new JButton(IconLoader.loadButtonIcon(IconLoader.Icons.PREVIOUS));
        previousButton.setToolTipText("Previous");
        previousButton.setFocusPainted(false);
        previousButton.setBorderPainted(false);
        previousButton.setContentAreaFilled(false);
        previousButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        previousButton.addActionListener(e -> controller.playPrevious());
        add(previousButton, "w 50!, h 50!");

        playPauseButton = new JButton(IconLoader.loadButtonIcon(controller.isPlayerPlaying() ? IconLoader.Icons.PAUSE : IconLoader.Icons.PLAY));
        playPauseButton.setToolTipText("Play / Pause");
        playPauseButton.setFocusPainted(false);
        playPauseButton.setBorderPainted(false);
        playPauseButton.setContentAreaFilled(false);
        playPauseButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        playPauseButton.addActionListener(e -> controller.togglePlayPause());
        add(playPauseButton, "w 50!, h 50!");

        nextButton = new JButton(IconLoader.loadButtonIcon(IconLoader.Icons.NEXT));
        nextButton.setToolTipText("Next");
        nextButton.setFocusPainted(false);
        nextButton.setBorderPainted(false);
        nextButton.setContentAreaFilled(false);
        nextButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        nextButton.addActionListener(e -> controller.playNext());
        add(nextButton, "w 50!, h 50!");
        
        // Queue button
        queueButton = new JButton(IconLoader.loadButtonIcon(IconLoader.Icons.QUEUE));
        queueButton.setToolTipText("View Queue");
        queueButton.setFocusPainted(false);
        queueButton.setBorderPainted(false);
        queueButton.setContentAreaFilled(false);
        queueButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        queueButton.addActionListener(e -> showQueueDialog());
        add(queueButton, "w 50!, h 50!, gapleft 20");

        // Volume icon (clickable untuk mute/unmute)
        volumeLabel = new JLabel(IconLoader.loadButtonIcon(IconLoader.Icons.VOLUME));
        volumeLabel.setToolTipText("Click to mute/unmute");
        volumeLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Add click listener untuk toggle mute
        volumeLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            private int previousVolume = 70; // Simpan volume sebelum mute
            
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int currentVolume = volumeSlider.getValue();
                
                if (currentVolume > 0) {
                    // Mute
                    previousVolume = currentVolume;
                    setVolume(0);
                } else {
                    // Unmute - restore previous volume
                    setVolume(previousVolume);
                }
            }
            
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                volumeLabel.setForeground(new Color(100, 180, 255));
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                volumeLabel.setForeground(Color.WHITE);
            }
            
            public int getVolume() {
                return volumeSlider.getValue();
            }

            private void setVolume(int volume) {
                isVolumeUpdating = true;
                volumeSlider.setValue(volume);
                controller.setVolume(volume);
                updateVolumeIcon(volume);
                isVolumeUpdating = false;
            }
        });
        
        add(volumeLabel, "gapleft 20");
        
        // Volume slider
        volumeSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 70);
        volumeSlider.setOpaque(false);
        volumeSlider.setFocusable(false);
        volumeSlider.setCursor(new Cursor(Cursor.HAND_CURSOR));
        volumeSlider.setPreferredSize(new Dimension(120, 30));
        
        // Style the slider
        volumeSlider.setForeground(new Color(100, 180, 255)); // Track color
        volumeSlider.setBackground(new Color(25, 25, 28));
        
        // Set initial volume
        controller.setVolume(70);
        volumeSlider.setToolTipText("Volume: 70%");
        
        // ========== ADD HOVER PREVIEW TOOLTIP ==========
        volumeSlider.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                // Calculate preview volume based on mouse position
                int previewVolume = calculateVolumeFromPosition(e.getX());
                
                // Update tooltip with preview value
                volumeSlider.setToolTipText("Volume: " + previewVolume + "%");
                
                // Force tooltip to update immediately
                ToolTipManager.sharedInstance().mouseMoved(e);
            }
            
            @Override
            public void mouseDragged(MouseEvent e) {
                // Also update during drag
                int previewVolume = calculateVolumeFromPosition(e.getX());
                volumeSlider.setToolTipText("Volume: " + previewVolume + "%");
            }
        });
        // ========== END HOVER PREVIEW ==========
        
        // Change listener untuk actual volume change
        volumeSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (!isVolumeUpdating) {
                    int volume = volumeSlider.getValue();
                    controller.setVolume(volume);
                    updateVolumeIcon(volume);
                    volumeSlider.setToolTipText("Volume: " + volume + "%");
                }
            }
        });
        
        add(volumeSlider, "w 120!, gapleft 5");
    }

    private int calculateVolumeFromPosition(int mouseX) {
        // Get slider dimensions
        int sliderWidth = volumeSlider.getWidth();
        int sliderMin = volumeSlider.getMinimum();
        int sliderMax = volumeSlider.getMaximum();
        
        // Calculate the usable track width (excluding borders/padding)
        Insets insets = volumeSlider.getInsets();
        int trackWidth = sliderWidth - insets.left - insets.right;
        int trackX = mouseX - insets.left;
        
        // Clamp to valid range
        trackX = Math.max(0, Math.min(trackX, trackWidth));
        
        // Calculate volume value
        double ratio = (double) trackX / trackWidth;
        int volume = (int) Math.round(sliderMin + (ratio * (sliderMax - sliderMin)));
        
        // Clamp to slider range
        return Math.max(sliderMin, Math.min(volume, sliderMax));
    }

    private void updateVolumeIcon(int volume) {
        String iconName = (volume == 0) ? IconLoader.Icons.VOLUME_MUTE : IconLoader.Icons.VOLUME;
        
        try {
            volumeLabel.setIcon(IconLoader.loadButtonIcon(iconName));
            volumeLabel.setToolTipText(volume == 0 ? "Click to unmute" : "Click to mute");
        } catch (Exception e) {
            System.err.println("Volume icon not found: " + iconName);
            volumeLabel.setIcon(IconLoader.loadButtonIcon(IconLoader.Icons.VOLUME));
        }
    }

    private void setupUpdateTimer() {
        updateTimer = new Timer(500, e -> refreshUI());
        updateTimer.start();
    }

    private void refreshUI() {
        // Update play/pause icon (gunakan setIcon, bukan setText)
        boolean isPlaying = controller.isPlayerPlaying();
        Icon playPauseIcon = IconLoader.loadButtonIcon(isPlaying ? IconLoader.Icons.PAUSE : IconLoader.Icons.PLAY);
        playPauseButton.setIcon(playPauseIcon);
        playPauseButton.setText(null); // pastikan tidak ada teks yang tampil
        playPauseButton.setToolTipText(isPlaying ? "Pause" : "Play");

        // Update track label
        Track currentTrack = controller.getCurrentTrack();
        if (currentTrack != null) {
            String trackInfo = currentTrack.getArtist() + " - " + currentTrack.getTitle();
            currentTrackLabel.setText(trackInfo);

            // Update time
            long currentTime = controller.getPlayerCurrentTime();
            long duration = controller.getPlayerDuration();
            String timeText = TimeFormatter.formatDuration((int) currentTime) +
                    " / " +
                    TimeFormatter.formatDuration((int) duration);
            timeLabel.setText(timeText);
        } else {
            currentTrackLabel.setText("No track playing");
            timeLabel.setText("0:00 / 0:00");
        }

        // Pastikan UI di-refresh
        playPauseButton.revalidate();
        playPauseButton.repaint();
        currentTrackLabel.revalidate();
        currentTrackLabel.repaint();
    }

    private void showQueueDialog() {
        JDialog queueDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Play Queue", false);
        queueDialog.setSize(700, 500);
        queueDialog.setLocationRelativeTo(this);

        // Create QueuePanel
        QueuePanel queuePanel = new QueuePanel(controller);
        queuePanel.onPanelOpened();
        queueDialog.add(queuePanel);

        // Show dialog
        queueDialog.setVisible(true);

        System.out.println("📋 Queue dialog opened");
    }

}