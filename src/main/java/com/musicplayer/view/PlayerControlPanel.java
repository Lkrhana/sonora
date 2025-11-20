package com.musicplayer.view;

import com.musicplayer.controller.MusicPlayerController;
import com.musicplayer.model.Track;
import com.musicplayer.util.TimeFormatter;
import net.miginfocom.swing.MigLayout;
import com.musicplayer.util.IconLoader;

import javax.swing.*;
import java.awt.*;

// Player control di bawah
public class PlayerControlPanel extends JPanel {
    private MusicPlayerController controller;
    private JButton playPauseButton;
    private JButton previousButton;
    private JButton nextButton;
    private JSlider volumeSlider;
    private JLabel currentTrackLabel;
    private JLabel timeLabel;

    private Timer updateTimer;

    public PlayerControlPanel(MusicPlayerController controller) {
        this.controller = controller;
        initializeComponents();
        setupUpdateTimer();
    }

    private void initializeComponents() {
        setLayout(new MigLayout("fillx, insets 15", "[grow][]10[]10[]10[]push[]20[]", ""));
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

// Untuk volume label, bisa juga diganti ikon
        JLabel volumeLabel = new JLabel(IconLoader.loadButtonIcon(IconLoader.Icons.VOLUME));
        volumeLabel.setToolTipText("Volume");
        add(volumeLabel, "gapleft 20");

    }

    private JButton createControlButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setForeground(Color.WHITE);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setForeground(new Color(100, 180, 255));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setForeground(Color.WHITE);
            }
        });

        return button;
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

}