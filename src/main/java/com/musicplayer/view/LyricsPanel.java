package com.musicplayer.view;

import com.musicplayer.model.Track;
import com.musicplayer.service.LyricsService;

import javax.swing.*;
import java.awt.*;

public class LyricsPanel extends JPanel {
    
    private LyricsService lyricsService;
    private JTextArea lyricsTextArea;
    private JLabel statusLabel;
    private JButton fetchButton;
    private Track currentTrack;
    
    public LyricsPanel() {
        this.lyricsService = new LyricsService();
        
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(18, 18, 18));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Header
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);
        
        // Lyrics display area
        lyricsTextArea = new JTextArea();
        lyricsTextArea.setEditable(false);
        lyricsTextArea.setLineWrap(true);
        lyricsTextArea.setWrapStyleWord(true);
        lyricsTextArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        lyricsTextArea.setBackground(new Color(25, 25, 28));
        lyricsTextArea.setForeground(new Color(230, 230, 230));
        lyricsTextArea.setCaretColor(Color.WHITE);
        lyricsTextArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        lyricsTextArea.setText("No song playing.\n\nPlay a song to see lyrics here.");
        
        JScrollPane scrollPane = new JScrollPane(lyricsTextArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 60), 1));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        add(scrollPane, BorderLayout.CENTER);
        
        // Footer with status
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(150, 150, 150));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        add(statusLabel, BorderLayout.SOUTH);
    }
    
    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        
        JLabel titleLabel = new JLabel("Lyrics");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        
        fetchButton = new JButton("Fetch Lyrics");
        fetchButton.setBackground(new Color(30, 215, 96));
        fetchButton.setForeground(Color.WHITE);
        fetchButton.setFocusPainted(false);
        fetchButton.setBorderPainted(false);
        fetchButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        fetchButton.setFont(new Font("Segoe UI", Font.BOLD, 11));
        fetchButton.setEnabled(false);
        fetchButton.addActionListener(e -> fetchLyricsForCurrentTrack());
        
        header.add(titleLabel, BorderLayout.WEST);
        header.add(fetchButton, BorderLayout.EAST);
        
        return header;
    }
    
    /**
     * Update panel dengan track baru
     */
    public void updateTrack(Track track) {
        this.currentTrack = track;
        
        if (track == null) {
            lyricsTextArea.setText("No song playing.\n\nPlay a song to see lyrics here.");
            statusLabel.setText("Ready");
            fetchButton.setEnabled(false);
            return;
        }
        
        fetchButton.setEnabled(true);
        lyricsTextArea.setText("Loading lyrics...");
        statusLabel.setText("Fetching lyrics for: " + track.getArtist() + " - " + track.getTitle());
        
        // Fetch lyrics in background thread
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return lyricsService.fetchLyrics(track.getArtist(), track.getTitle());
            }
            
            @Override
            protected void done() {
                try {
                    String lyrics = get();
                    if (lyrics != null && !lyrics.isEmpty()) {
                        lyricsTextArea.setText(lyrics);
                        lyricsTextArea.setCaretPosition(0); // Scroll to top
                        statusLabel.setText("Lyrics loaded");
                    } else {
                        lyricsTextArea.setText("Lyrics not found\n\n" +
                                "Could not find lyrics for:\n" +
                                track.getArtist() + " - " + track.getTitle() + "\n\n" +
                                "Try:\n" +
                                "• Check if song title and artist are correct\n" +
                                "• Search for lyrics online\n" +
                                "• Click 'Fetch Lyrics' to retry");
                        statusLabel.setText("Lyrics not found");
                    }
                } catch (Exception e) {
                    lyricsTextArea.setText("Error loading lyrics\n\n" + e.getMessage());
                    statusLabel.setText("Error loading lyrics");
                }
            }
        };
        
        worker.execute();
    }
    
    /**
     * Manual fetch untuk current track
     */
    private void fetchLyricsForCurrentTrack() {
        if (currentTrack != null) {
            updateTrack(currentTrack);
        }
    }
    
    /**
     * Clear lyrics
     */
    public void clearLyrics() {
        lyricsTextArea.setText("No song playing.\n\nPlay a song to see lyrics here.");
        statusLabel.setText("Ready");
        currentTrack = null;
        fetchButton.setEnabled(false);
    }
}