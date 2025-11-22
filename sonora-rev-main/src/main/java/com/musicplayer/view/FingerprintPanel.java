package com.musicplayer.view;

import com.musicplayer.controller.MusicPlayerController;
import com.musicplayer.model.Track;
import com.musicplayer.util.IconLoader;
import com.musicplayer.util.ImageUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class FingerprintPanel extends JPanel {
    private MusicPlayerController controller;
    private MainFrame mainFrame;

    private JButton recordButton;
    private JButton uploadButton;
    private JPanel resultPanel;
    private JLabel statusLabel;
    private JLabel statusIconLabel;
    private boolean isRecording = false;

    private Track identifiedTrack;

    public FingerprintPanel(MusicPlayerController controller, MainFrame mainFrame) {
        this.controller = controller;
        this.mainFrame = mainFrame;
        initializeComponents();
    }

    private void initializeComponents() {
        setLayout(new MigLayout("fill, insets 40", "[center]", "[]30[]30[]20[]push"));
        setBackground(new Color(30, 30, 35));

        // Title
        JLabel titleLabel = new JLabel("Identify Song");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setIcon(IconLoader.loadLargeIcon(IconLoader.Icons.FINGERPRINT));
        add(titleLabel, "wrap, center");

        JLabel subtitleLabel = new JLabel("Record audio or upload a file to identify the song");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        subtitleLabel.setForeground(new Color(180, 180, 180));
        add(subtitleLabel, "wrap, center, gapbottom 20");

        // Buttons panel
        JPanel buttonsPanel = new JPanel(new MigLayout("insets 0", "[]20[]", "[]"));
        buttonsPanel.setOpaque(false);

        recordButton = new JButton("Record from Mic", IconLoader.loadButtonIcon(IconLoader.Icons.RECORD));
        recordButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        recordButton.setPreferredSize(new Dimension(200, 60));
        recordButton.setFocusPainted(false);
        recordButton.addActionListener(e -> toggleRecording());
        buttonsPanel.add(recordButton);

        uploadButton = new JButton("Upload Audio File", IconLoader.loadButtonIcon(IconLoader.Icons.UPLOAD));
        uploadButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        uploadButton.setPreferredSize(new Dimension(200, 60));
        uploadButton.setFocusPainted(false);
        uploadButton.addActionListener(e -> uploadFile());
        buttonsPanel.add(uploadButton);

        add(buttonsPanel, "wrap, center");

        // Status panel with icon and text
        JPanel statusPanel = new JPanel(new MigLayout("insets 0", "[]10[]", ""));
        statusPanel.setOpaque(false);

        statusIconLabel = new JLabel();
        statusPanel.add(statusIconLabel);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        statusLabel.setForeground(new Color(150, 150, 150));
        statusPanel.add(statusLabel);

        add(statusPanel, "wrap, center, gaptop 10");

        // Result panel
        resultPanel = new JPanel();
        resultPanel.setOpaque(false);
        add(resultPanel, "grow, width 60%, center");
    }

    private void toggleRecording() {
        if (!isRecording) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void startRecording() {
        isRecording = true;
        recordButton.setText("Stop Recording");
        recordButton.setIcon(IconLoader.loadButtonIcon(IconLoader.Icons.STOP));
        recordButton.setBackground(new Color(200, 50, 50));

        statusIconLabel.setIcon(IconLoader.loadButtonIcon(IconLoader.Icons.RECORD));
        statusLabel.setText("Recording... (play music near your microphone)");
        statusLabel.setForeground(new Color(255, 100, 100));

        resultPanel.removeAll();
        resultPanel.revalidate();
        resultPanel.repaint();

        // Start recording in background
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                controller.startRecording();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (Exception e) {
                    showError("Failed to start recording: " + e.getMessage());
                    resetRecording();
                }
            }
        };
        worker.execute();
    }

    private void stopRecording() {
        isRecording = false;
        recordButton.setEnabled(false);
        recordButton.setText("Processing...");
        recordButton.setIcon(IconLoader.loadButtonIcon(IconLoader.Icons.LOADING));
        recordButton.setBackground(null);

        statusIconLabel.setIcon(IconLoader.loadButtonIcon(IconLoader.Icons.LOADING));
        statusLabel.setText("Processing audio and identifying...");
        statusLabel.setForeground(new Color(255, 200, 100));

        // Stop recording and identify
        SwingWorker<Track, Void> worker = new SwingWorker<>() {
            @Override
            protected Track doInBackground() throws Exception {
                return controller.stopRecordingAndIdentify();
            }

            @Override
            protected void done() {
                try {
                    Track track = get();
                    displayResult(track);
                } catch (Exception e) {
                    showError("Failed to identify: " + e.getMessage());
                } finally {
                    resetRecording();
                }
            }
        };
        worker.execute();
    }

    private void uploadFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                return f.isDirectory() ||
                        f.getName().toLowerCase().endsWith(".mp3") ||
                        f.getName().toLowerCase().endsWith(".wav") ||
                        f.getName().toLowerCase().endsWith(".m4a");
            }
            public String getDescription() {
                return "Audio Files (*.mp3, *.wav, *.m4a)";
            }
        });

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            identifyFile(file);
        }
    }

    private void identifyFile(File file) {
        uploadButton.setEnabled(false);
        recordButton.setEnabled(false);

        statusIconLabel.setIcon(IconLoader.loadButtonIcon(IconLoader.Icons.LOADING));
        statusLabel.setText("Identifying song...");
        statusLabel.setForeground(new Color(255, 200, 100));

        resultPanel.removeAll();
        resultPanel.revalidate();
        resultPanel.repaint();

        SwingWorker<Track, Void> worker = new SwingWorker<>() {
            @Override
            protected Track doInBackground() throws Exception {
                return controller.identifyTrack(file);
            }

            @Override
            protected void done() {
                try {
                    Track track = get();
                    displayResult(track);
                } catch (Exception e) {
                    showError("Failed to identify: " + e.getMessage());
                } finally {
                    uploadButton.setEnabled(true);
                    recordButton.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void displayResult(Track track) {
        resultPanel.removeAll();

        if (track != null) {
            identifiedTrack = track;
            statusIconLabel.setIcon(IconLoader.loadButtonIcon(IconLoader.Icons.SUCCESS));
            statusLabel.setText("Song Identified!");
            statusLabel.setForeground(new Color(100, 255, 100));

            // Create result card
            JPanel resultCard = createResultCard(track);
            resultPanel.setLayout(new BorderLayout());
            resultPanel.add(resultCard, BorderLayout.CENTER);

            // Save to database
            controller.saveTrack(track);
        } else {
            statusIconLabel.setIcon(IconLoader.loadButtonIcon(IconLoader.Icons.ERROR));
            statusLabel.setText("Song Not Found");
            statusLabel.setForeground(new Color(255, 100, 100));

            JPanel noResultPanel = new JPanel(new MigLayout("fillx, wrap 1", "[center]", "[]10[]"));
            noResultPanel.setOpaque(false);

            JLabel noResultLabel = new JLabel("Sorry, we couldn't identify this song.");
            noResultLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            noResultLabel.setForeground(new Color(200, 200, 200));
            noResultLabel.setHorizontalAlignment(SwingConstants.CENTER);
            noResultPanel.add(noResultLabel, "center");

            JLabel tipsLabel = new JLabel("<html><center>" +
                    "<b>Tips:</b><br>" +
                    "• Make sure the audio is clear<br>" +
                    "• Try recording for at least 5-10 seconds<br>" +
                    "• Reduce background noise if possible" +
                    "</center></html>");
            tipsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            tipsLabel.setForeground(new Color(150, 150, 150));
            tipsLabel.setHorizontalAlignment(SwingConstants.CENTER);
            noResultPanel.add(tipsLabel, "center, gaptop 10");

            resultPanel.setLayout(new BorderLayout());
            resultPanel.add(noResultPanel, BorderLayout.CENTER);
        }

        resultPanel.revalidate();
        resultPanel.repaint();
    }

    private JPanel createResultCard(Track track) {
        JPanel card = new JPanel(new MigLayout("fill, insets 20", "[150!]20[grow]", "[][][]20[]"));
        card.setBackground(new Color(45, 45, 50));
        card.setBorder(BorderFactory.createLineBorder(new Color(100, 255, 100), 2));

        // Album art
        JLabel albumArt = new JLabel();
        albumArt.setPreferredSize(new Dimension(150, 150));
        albumArt.setHorizontalAlignment(SwingConstants.CENTER);
        albumArt.setOpaque(true);
        albumArt.setBackground(new Color(60, 60, 65));
        albumArt.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 75), 1));

        if (track.getThumbnailUrl() != null && !track.getThumbnailUrl().isEmpty()) {
            new Thread(() -> {
                ImageIcon thumbnail = ImageUtils.loadImageFromUrl(track.getThumbnailUrl(), 150, 150);
                SwingUtilities.invokeLater(() -> albumArt.setIcon(thumbnail));
            }).start();
        } else {
            albumArt.setIcon(ImageUtils.getPlaceholderIcon(150, 150));
        }

        card.add(albumArt, "spany 4");

        // Track info
        JLabel titleLabel = new JLabel(track.getTitle());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        card.add(titleLabel, "wrap");

        JLabel artistLabel = new JLabel("by " + track.getArtist());
        artistLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        artistLabel.setForeground(new Color(180, 180, 180));
        card.add(artistLabel, "wrap");

        if (track.getAlbum() != null && !track.getAlbum().isEmpty()) {
            JLabel albumLabel = new JLabel("Album: " + track.getAlbum());
            albumLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            albumLabel.setForeground(new Color(150, 150, 150));
            card.add(albumLabel, "wrap");
        }

        // "Search in Player" button
        JButton searchInPlayerButton = new JButton("Search in Player",
                IconLoader.loadButtonIcon(IconLoader.Icons.SEARCH));
        searchInPlayerButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        searchInPlayerButton.setFocusPainted(false);
        searchInPlayerButton.setBackground(new Color(100, 100, 255));
        searchInPlayerButton.setForeground(Color.WHITE);
        searchInPlayerButton.setPreferredSize(new Dimension(250, 50));
        searchInPlayerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        searchInPlayerButton.addActionListener(e -> searchInPlayer(track));
        card.add(searchInPlayerButton, "gaptop 10, width 250!, height 50!");

        return card;
    }

    private void searchInPlayer(Track track) {
        String searchQuery = track.getArtist() + " " + track.getTitle();
        mainFrame.showLibraryAndSearch(searchQuery);

        JOptionPane.showMessageDialog(this,
                "Searching for: " + searchQuery,
                "Search in Player",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String message) {
        statusIconLabel.setIcon(IconLoader.loadButtonIcon(IconLoader.Icons.ERROR));
        statusLabel.setText("Error");
        statusLabel.setForeground(new Color(255, 100, 100));

        JLabel errorLabel = new JLabel("<html><center>Error: " + message + "</center></html>");
        errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        errorLabel.setForeground(new Color(255, 100, 100));
        errorLabel.setHorizontalAlignment(SwingConstants.CENTER);

        resultPanel.removeAll();
        resultPanel.setLayout(new BorderLayout());
        resultPanel.add(errorLabel, BorderLayout.CENTER);
        resultPanel.revalidate();
        resultPanel.repaint();
    }

    private void resetRecording() {
        isRecording = false;
        recordButton.setEnabled(true);
        recordButton.setText("Record from Mic");
        recordButton.setIcon(IconLoader.loadButtonIcon(IconLoader.Icons.RECORD));
        recordButton.setBackground(null);
    }
}