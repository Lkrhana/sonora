package com.musicplayer.view;

import com.musicplayer.controller.MusicPlayerController;
import com.musicplayer.model.Recommendation;
import com.musicplayer.model.Track;
import com.musicplayer.util.IconLoader;
import com.musicplayer.util.ImageUtils;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

// Recom
public class RecommendationPanel extends JPanel {
    private MusicPlayerController controller;
    private JPanel dailyMixPanel;
    private JPanel similarSongsPanel;
    private JButton generateButton;
    private JButton playAllDailyMixButton;
    private JButton playAllSimilarButton;

    private List<Recommendation> currentDailyMix;
    private List<Recommendation> currentSimilar;

    public RecommendationPanel(MusicPlayerController controller) {
        this.controller = controller;
        initializeComponents();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(30, 30, 35));

        // Header
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Content with tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // Daily Mix tab
        dailyMixPanel = new JPanel(new MigLayout("fillx, wrap 1", "[grow]", "[]10"));
        dailyMixPanel.setBackground(new Color(30, 30, 35));
        JScrollPane dailyMixScroll = new JScrollPane(dailyMixPanel);
        dailyMixScroll.setBorder(BorderFactory.createEmptyBorder());
        tabbedPane.addTab("Daily Mix", dailyMixScroll);

        // Similar Songs tab
        similarSongsPanel = new JPanel(new MigLayout("fillx, wrap 1", "[grow]", "[]10"));
        similarSongsPanel.setBackground(new Color(30, 30, 35));
        JScrollPane similarScroll = new JScrollPane(similarSongsPanel);
        similarScroll.setBorder(BorderFactory.createEmptyBorder());
        tabbedPane.addTab("Similar Songs", similarScroll);

        add(tabbedPane, BorderLayout.CENTER);

        loadRecommendations();
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new MigLayout("fillx", "[]push[]", ""));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        JLabel titleLabel = new JLabel("Recommendations");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        panel.add(titleLabel);

        generateButton = new JButton("Refresh", IconLoader.loadButtonIcon(IconLoader.Icons.REFRESH));
        generateButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        generateButton.setFocusPainted(false);
        generateButton.addActionListener(e -> loadRecommendations());
        panel.add(generateButton, "h 35!");

        return panel;
    }

    private void loadRecommendations() {
        generateButton.setEnabled(false);
        generateButton.setText("Loading...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            List<Recommendation> dailyMix;
            List<Recommendation> similar;

            @Override
            protected Void doInBackground() throws Exception {
                dailyMix = controller.generateDailyMix(20);
                similar = controller.getSimilarToRecent(15);
                return null;
            }

            @Override
            protected void done() {
                try {
                    currentDailyMix = dailyMix;
                    currentSimilar = similar;
                    displayDailyMix(dailyMix);
                    displaySimilarSongs(similar);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(
                            RecommendationPanel.this,
                            "Error loading recommendations: " + e.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                } finally {
                    generateButton.setEnabled(true);
                    generateButton.setText("Refresh");
                }
            }
        };
        worker.execute();
    }

    private void displayDailyMix(List<Recommendation> recommendations) {
        dailyMixPanel.removeAll();

        if (recommendations == null || recommendations.isEmpty()) {
            JLabel noDataLabel = new JLabel("No recommendations yet. Play some songs first!");
            noDataLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            noDataLabel.setForeground(new Color(150, 150, 150));
            dailyMixPanel.add(noDataLabel, "center, gaptop 40");
        } else {
            // Play All button at top
            JPanel playAllPanel = new JPanel(new MigLayout("fillx, insets 10", "[]20[]", ""));
            playAllPanel.setOpaque(false);

            JLabel countLabel = new JLabel(recommendations.size() + " songs");
            countLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            countLabel.setForeground(Color.WHITE);
            playAllPanel.add(countLabel);

            playAllDailyMixButton = new JButton("Play All", IconLoader.loadButtonIcon(IconLoader.Icons.PLAY));
            playAllDailyMixButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
            playAllDailyMixButton.setFocusPainted(false);
            playAllDailyMixButton.setBackground(new Color(100, 100, 255));
            playAllDailyMixButton.setForeground(Color.WHITE);
            playAllDailyMixButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            playAllDailyMixButton.addActionListener(e -> playAllDaily());
            playAllPanel.add(playAllDailyMixButton, "w 150!, h 40!");

            dailyMixPanel.add(playAllPanel, "growx, gapbottom 10");

            // Track cards
            for (int i = 0; i < recommendations.size(); i++) {
                Recommendation rec = recommendations.get(i);
                JPanel trackCard = createTrackCard(rec, i, true);
                dailyMixPanel.add(trackCard, "growx, h 70!");
            }
        }

        dailyMixPanel.revalidate();
        dailyMixPanel.repaint();
    }

    private void displaySimilarSongs(List<Recommendation> recommendations) {
        similarSongsPanel.removeAll();

        if (recommendations == null || recommendations.isEmpty()) {
            JLabel noDataLabel = new JLabel("Play a song to get similar recommendations");
            noDataLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            noDataLabel.setForeground(new Color(150, 150, 150));
            similarSongsPanel.add(noDataLabel, "center, gaptop 40");
        } else {
            // Play All button at top
            JPanel playAllPanel = new JPanel(new MigLayout("fillx, insets 10", "[]20[]", ""));
            playAllPanel.setOpaque(false);

            JLabel countLabel = new JLabel(recommendations.size() + " songs");
            countLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            countLabel.setForeground(Color.WHITE);
            playAllPanel.add(countLabel);

            playAllSimilarButton = new JButton("Play All", IconLoader.loadButtonIcon(IconLoader.Icons.PLAY));
            playAllSimilarButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
            playAllSimilarButton.setFocusPainted(false);
            playAllSimilarButton.setBackground(new Color(100, 100, 255));
            playAllSimilarButton.setForeground(Color.WHITE);
            playAllSimilarButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            playAllSimilarButton.addActionListener(e -> playAllSimilar());
            playAllPanel.add(playAllSimilarButton, "w 150!, h 40!");

            similarSongsPanel.add(playAllPanel, "growx, gapbottom 10");

            // Track cards
            for (int i = 0; i < recommendations.size(); i++) {
                Recommendation rec = recommendations.get(i);
                JPanel trackCard = createTrackCard(rec, i, false);
                similarSongsPanel.add(trackCard, "growx, h 70!");
            }
        }

        similarSongsPanel.revalidate();
        similarSongsPanel.repaint();
    }

    private JPanel createTrackCard(Recommendation rec, int index, boolean isDailyMix) {
        Track track = rec.getTrack();

        JPanel card = new JPanel(new MigLayout("fillx, insets 10", "[]10[]10[grow]10[]", ""));
        card.setBackground(new Color(40, 40, 45));
        card.setBorder(BorderFactory.createLineBorder(new Color(50, 50, 55), 1));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Index number
        JLabel indexLabel = new JLabel(String.valueOf(index + 1));
        indexLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        indexLabel.setForeground(new Color(150, 150, 150));
        indexLabel.setPreferredSize(new Dimension(30, 30));
        indexLabel.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(indexLabel);

        // Thumbnail
        JLabel thumbnail = new JLabel();
        thumbnail.setPreferredSize(new Dimension(50, 50));
        thumbnail.setHorizontalAlignment(SwingConstants.CENTER);
        thumbnail.setOpaque(true);
        thumbnail.setBackground(new Color(60, 60, 65));

        if (track.getThumbnailUrl() != null && !track.getThumbnailUrl().isEmpty()) {
            new Thread(() -> {
                ImageIcon img = ImageUtils.loadImageFromUrl(track.getThumbnailUrl(), 50, 50);
                SwingUtilities.invokeLater(() -> thumbnail.setIcon(img));
            }).start();
        } else {
            thumbnail.setIcon(IconLoader.loadButtonIcon(IconLoader.Icons.MUSIC));
        }
        card.add(thumbnail);

        // Track info
        JPanel infoPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow]", "[]5[]"));
        infoPanel.setOpaque(false);

        JLabel titleLabel = new JLabel(track.getTitle());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(Color.WHITE);
        infoPanel.add(titleLabel, "wrap");

        String subtitle = track.getArtist();
        if (rec.getReason() != null && !rec.getReason().isEmpty()) {
            subtitle += " • " + rec.getReason();
        }
        JLabel artistLabel = new JLabel(subtitle);
        artistLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        artistLabel.setForeground(new Color(150, 150, 150));
        infoPanel.add(artistLabel);

        card.add(infoPanel, "grow");

        // Play button
        JButton playButton = new JButton(IconLoader.loadButtonIcon(IconLoader.Icons.PLAY));
        playButton.setFocusPainted(false);
        playButton.setPreferredSize(new Dimension(40, 40));
        playButton.setBorderPainted(false);
        playButton.setContentAreaFilled(false);
        playButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        playButton.addActionListener(e -> playFromIndex(index, isDailyMix));
        card.add(playButton);

        // Hover effect
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                card.setBackground(new Color(50, 50, 55));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                card.setBackground(new Color(40, 40, 45));
            }
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    playFromIndex(index, isDailyMix);
                }
            }
        });

        return card;
    }

    /**
     * Play all Daily Mix as playlist
     */
    private void playAllDaily() {
        if (currentDailyMix != null && !currentDailyMix.isEmpty()) {
            List<Track> tracks = currentDailyMix.stream()
                    .map(Recommendation::getTrack)
                    .collect(Collectors.toList());
            controller.setQueueAndPlay(tracks, 0);
        }
    }

    /**
     * Play all Similar songs as playlist
     */
    private void playAllSimilar() {
        if (currentSimilar != null && !currentSimilar.isEmpty()) {
            List<Track> tracks = currentSimilar.stream()
                    .map(Recommendation::getTrack)
                    .collect(Collectors.toList());
            controller.setQueueAndPlay(tracks, 0);
        }
    }

    /**
     * Play from specific index
     */
    private void playFromIndex(int index, boolean isDailyMix) {
        List<Recommendation> recommendations = isDailyMix ? currentDailyMix : currentSimilar;

        if (recommendations != null && !recommendations.isEmpty()) {
            List<Track> tracks = recommendations.stream()
                    .map(Recommendation::getTrack)
                    .collect(Collectors.toList());
            controller.setQueueAndPlay(tracks, index);
        }
    }

    /**
     * Public method to trigger play from external sources
     */
    public void playDailyMix() {
        playAllDaily();
    }
}