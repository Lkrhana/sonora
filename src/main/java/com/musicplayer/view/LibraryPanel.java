package com.musicplayer.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.musicplayer.controller.MusicPlayerController;
import com.musicplayer.model.Track;
import com.musicplayer.util.IconLoader;
import com.musicplayer.util.ImageUtils;

import net.miginfocom.swing.MigLayout;


public class LibraryPanel extends JPanel {
    private MusicPlayerController controller;
    private MainFrame mainFrame;
    private JTextField searchField;
    private JButton searchButton;
    private JButton playDailyMixButton;

    private JPanel featuredTrackPanel;
    private JPanel trackListPanel;
    private JPanel initialContentPanel;
    private JPanel contentPanel;
    private JScrollPane scrollPane;

    private List<Track> currentSearchResults;
    private boolean showingSearchResults = false;

    // Popular songs untuk initial state (Intinya biar db ga kosong dan bisa langsung search, bisa diubah lagu apa aja)
    private static final String[] POPULAR_SONGS = {
            "APT. Rose Bruno Mars",
            "Die With A Smile Lady Gaga Bruno Mars",
            "Espresso Sabrina Carpenter",
            "Birds of a Feather Billie Eilish",
            "Beautiful Things Benson Boone",
            "Lose Control Teddy Swims",
            "I Like The Way You Kiss Me Artemas",
            "Cruel Summer Taylor Swift",
            "End of Beginning Djo",
            "Fortnight Taylor Swift"
    };

    public LibraryPanel(MusicPlayerController controller) {
        this.controller = controller;
        initializeComponents();
        loadInitialContent();
    }

    public void setMainFrame(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    private void initializeComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(30, 30, 35));

        // Search panel at top
        JPanel searchPanel = createSearchPanel();
        add(searchPanel, BorderLayout.NORTH);

        // Scrollable content area
        contentPanel = new JPanel(new MigLayout("fillx, wrap 1", "[grow]", "[]10[]10[]"));
        contentPanel.setOpaque(false);

        // Initial content (shown when no search)
        initialContentPanel = new JPanel(new MigLayout("fillx, wrap 1", "[grow]", "[]10[]"));
        initialContentPanel.setOpaque(false);
        contentPanel.add(initialContentPanel, "growx, hidemode 3");

        // Featured track (hasil pertama search - BESAR)
        featuredTrackPanel = new JPanel();
        featuredTrackPanel.setOpaque(false);
        featuredTrackPanel.setVisible(false);
        contentPanel.add(featuredTrackPanel, "growx, hidemode 3");

        // Track list (sisanya - list kecil)
        trackListPanel = new JPanel(new MigLayout("fillx, wrap 1, insets 0", "[grow]", "[]5"));
        trackListPanel.setOpaque(false);
        trackListPanel.setVisible(false);
        contentPanel.add(trackListPanel, "growx, hidemode 3");

        scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new MigLayout("fillx, insets 20", "[grow]10[]", "[]"));
        panel.setOpaque(false);

        JLabel titleLabel = new JLabel("Music Library");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setIcon(IconLoader.loadButtonIcon(IconLoader.Icons.MUSICLIBRARY));
        panel.add(titleLabel, "wrap, gapbottom 15");

        searchField = new JTextField();
        searchField.putClientProperty("JTextField.placeholderText", "Search songs from YouTube Music...");
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.addActionListener(e -> performSearch());
        panel.add(searchField, "growx, h 40!");

        searchButton = new JButton(IconLoader.loadButtonIcon(IconLoader.Icons.SEARCH));
        searchButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        searchButton.setFocusPainted(false);
        searchButton.addActionListener(e -> performSearch());
        panel.add(searchButton, "h 40!, w 100!");

        return panel;
    }

    /**
     * Load initial content (popular songs + daily mix banner)
     */
    private void loadInitialContent() {
        initialContentPanel.removeAll();

        // Daily Mix Banner
        JPanel dailyMixBanner = createDailyMixBanner();
        initialContentPanel.add(dailyMixBanner, "growx, h 150!, gapbottom 20");

        // Popular Songs Section
        JLabel popularLabel = new JLabel("Popular Right Now");
        popularLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        popularLabel.setForeground(Color.WHITE);
        initialContentPanel.add(popularLabel, "gapbottom 10");

        // Show popular songs as small cards
        for (String songQuery : POPULAR_SONGS) {
            JPanel songCard = createInitialSongCard(songQuery);
            initialContentPanel.add(songCard, "growx, h 60!");
        }

        initialContentPanel.revalidate();
        initialContentPanel.repaint();
    }

    /**
     * Create Daily Mix banner with play button
     */
    private JPanel createDailyMixBanner() {
        JPanel banner = new JPanel(new MigLayout("fill, insets 20", "[grow][]", "[][]"));
        banner.setBackground(new Color(45, 45, 55));
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 255, 100), 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // Title
        JLabel titleLabel = new JLabel("Your Daily Mix");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setIcon(IconLoader.loadLargeIcon(IconLoader.Icons.RECOMMENDATIONS));
        banner.add(titleLabel, "wrap");

        // Description
        JLabel descLabel = new JLabel("Personalized playlist just for you");
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        descLabel.setForeground(new Color(180, 180, 180));
        banner.add(descLabel);

        // Play Button
        playDailyMixButton = new JButton("Play Daily Mix", IconLoader.loadButtonIcon(IconLoader.Icons.PLAY));
        playDailyMixButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        playDailyMixButton.setFocusPainted(false);
        playDailyMixButton.setBackground(new Color(100, 100, 255));
        playDailyMixButton.setForeground(Color.WHITE);
        playDailyMixButton.setPreferredSize(new Dimension(200, 50));
        playDailyMixButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        playDailyMixButton.addActionListener(e -> switchToDailyMixAndPlay());
        banner.add(playDailyMixButton, "spany 2, w 200!, h 50!");

        return banner;
    }

    /**
     * Create initial song card (before search)
     */
    private JPanel createInitialSongCard(String songQuery) {
        JPanel card = new JPanel(new MigLayout("fillx, insets 10", "[]10[grow]10[]", ""));
        card.setBackground(new Color(40, 40, 45));
        card.setBorder(BorderFactory.createLineBorder(new Color(50, 50, 55), 1));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Icon
        JLabel icon = new JLabel(IconLoader.loadButtonIcon(IconLoader.Icons.MUSIC));
        icon.setPreferredSize(new Dimension(40, 40));
        card.add(icon);

        // Song name
        JLabel nameLabel = new JLabel(songQuery);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        nameLabel.setForeground(Color.WHITE);
        card.add(nameLabel, "grow");

        // Search button
        JButton searchBtn = new JButton(IconLoader.loadButtonIcon(IconLoader.Icons.SEARCH));
        searchBtn.setFocusPainted(false);
        searchBtn.setBorderPainted(false);
        searchBtn.setContentAreaFilled(false);
        searchBtn.setPreferredSize(new Dimension(40, 40));
        searchBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        searchBtn.addActionListener(e -> performSearchWithQuery(songQuery));
        card.add(searchBtn);

        // Click to search
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                card.setBackground(new Color(50, 50, 55));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                card.setBackground(new Color(40, 40, 45));
            }
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                performSearchWithQuery(songQuery);
            }
        });

        return card;
    }

    /**
     * Switch to Recommendations and play Daily Mix
     */
    private void switchToDailyMixAndPlay() {
        if (mainFrame != null) {
            mainFrame.switchToRecommendationsAndPlay();
        }
    }

    /**
     * Perform search dengan query
     */
    private void performSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a search term");
            return;
        }

        performSearchWithQuery(query);
    }

    /**
     * Public method untuk search dari komponen lain
     */
    public void performSearchWithQuery(String query) {
        System.out.println("🔍 LibraryPanel.performSearchWithQuery called with: " + query);

        searchField.setText(query);

        // Hide initial content, show search results
        initialContentPanel.setVisible(false);
        showingSearchResults = true;

        // Show loading
        searchButton.setEnabled(false);
        searchButton.setIcon(IconLoader.loadButtonIcon(IconLoader.Icons.LOADING));

        // Clear previous results
        featuredTrackPanel.removeAll();
        trackListPanel.removeAll();
        featuredTrackPanel.setVisible(false);

        System.out.println("🔄 Starting search worker...");

        // Search in background thread
        SwingWorker<List<Track>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Track> doInBackground() throws Exception {
                System.out.println("🔍 Calling controller.searchYouTubeMusic...");
                List<Track> results = controller.searchYouTubeMusic(query);
                System.out.println("✅ Search returned " + (results != null ? results.size() : 0) + " results");
                return results;
            }

            @Override
            protected void done() {
                try {
                    List<Track> tracks = get();
                    currentSearchResults = tracks;

                    System.out.println("📊 Displaying " + (tracks != null ? tracks.size() : 0) + " tracks");
                    displayTracks(tracks);

                } catch (Exception e) {
                    System.err.println("❌ Search error: " + e.getMessage());
                    e.printStackTrace();

                    JOptionPane.showMessageDialog(
                            LibraryPanel.this,
                            "Error searching: " + e.getMessage(),
                            "Search Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                } finally {
                    searchButton.setEnabled(true);
                    searchButton.setIcon(IconLoader.loadButtonIcon(IconLoader.Icons.SEARCH));
                    System.out.println("✅ Search completed");
                }
            }
        };
        worker.execute();
    }

    /**
     * Display tracks: pertama BESAR, sisanya list
     */
    private void displayTracks(List<Track> tracks) {
        System.out.println("📺 displayTracks called with " + (tracks != null ? tracks.size() : "null") + " tracks");

        featuredTrackPanel.removeAll();
        trackListPanel.removeAll();

        if (tracks == null || tracks.isEmpty()) {
            System.out.println("⚠️ No tracks to display");
            JLabel noResults = new JLabel("No results found");
            noResults.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            noResults.setForeground(new Color(150, 150, 150));
            trackListPanel.add(noResults);
            trackListPanel.setVisible(true);
        } else {
            System.out.println("✅ Creating featured track card for: " + tracks.get(0).getTitle());

            // Featured track (hasil pertama - BESAR)
            Track featuredTrack = tracks.get(0);
            JPanel featuredCard = createFeaturedTrackCard(featuredTrack, 0);
            featuredTrackPanel.setLayout(new BorderLayout());
            featuredTrackPanel.add(featuredCard, BorderLayout.CENTER);
            featuredTrackPanel.setVisible(true);

            // Sisanya sebagai list kecil
            System.out.println("📝 Creating " + (tracks.size() - 1) + " small track cards");
            for (int i = 1; i < tracks.size(); i++) {
                Track track = tracks.get(i);
                JPanel trackCard = createSmallTrackCard(track, i);
                trackListPanel.add(trackCard, "growx, h 60!");
            }
            trackListPanel.setVisible(true);
        }

        System.out.println("🔄 Revalidating and repainting panels...");
        contentPanel.revalidate();
        contentPanel.repaint();
        scrollPane.revalidate();
        scrollPane.repaint();
        scrollPane.getVerticalScrollBar().setValue(0);

        System.out.println("✅ Display tracks completed");
    }

    /**
     * Create FEATURED track card
     */
    private JPanel createFeaturedTrackCard(Track track, int index) {
        JPanel card = new JPanel(new MigLayout("fill, insets 20", "[200!]20[grow]", "[][grow][]"));
        card.setBackground(new Color(45, 45, 50));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(100, 100, 255, 100), 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        // Album art
        JLabel albumArt = new JLabel();
        albumArt.setPreferredSize(new Dimension(200, 200));
        albumArt.setHorizontalAlignment(SwingConstants.CENTER);
        albumArt.setOpaque(true);
        albumArt.setBackground(new Color(60, 60, 65));
        albumArt.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 75), 1));

        if (track.getThumbnailUrl() != null && !track.getThumbnailUrl().isEmpty()) {
            new Thread(() -> {
                ImageIcon thumbnail = ImageUtils.loadImageFromUrl(track.getThumbnailUrl(), 200, 200);
                SwingUtilities.invokeLater(() -> albumArt.setIcon(thumbnail));
            }).start();
        } else {
            albumArt.setIcon(ImageUtils.getPlaceholderIcon(200, 200));
        }

        card.add(albumArt, "spany 3");

        // Track info
        JLabel titleLabel = new JLabel(track.getTitle());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        card.add(titleLabel, "wrap");

        JLabel artistLabel = new JLabel(track.getArtist());
        artistLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        artistLabel.setForeground(new Color(180, 180, 180));
        card.add(artistLabel, "wrap");

        // Play button
        JButton playButton = new JButton("PLAY NOW", IconLoader.loadButtonIcon(IconLoader.Icons.PLAY));
        playButton.setFont(new Font("Segoe UI", Font.BOLD, 16));
        playButton.setFocusPainted(false);
        playButton.setPreferredSize(new Dimension(200, 50));
        playButton.setBackground(new Color(100, 100, 255));
        playButton.setForeground(Color.WHITE);
        playButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        playButton.addActionListener(e -> playTrack(track, index));
        card.add(playButton, "gaptop 10, width 200!, height 50!");

        return card;
    }

    /**
     * Create SMALL track card
     */
    private JPanel createSmallTrackCard(Track track, int index) {
        JPanel card = new JPanel(new MigLayout("fillx, insets 10", "[]10[grow]10[]", ""));
        card.setBackground(new Color(40, 40, 45));
        card.setBorder(BorderFactory.createLineBorder(new Color(50, 50, 55), 1));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

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

        JLabel artistLabel = new JLabel(track.getArtist());
        artistLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        artistLabel.setForeground(new Color(150, 150, 150));
        infoPanel.add(artistLabel);

        card.add(infoPanel, "grow");

        // Play button
        JButton playButton = new JButton(IconLoader.loadButtonIcon(IconLoader.Icons.PLAY));
        playButton.setFocusPainted(false);
        playButton.setBorderPainted(false);
        playButton.setContentAreaFilled(false);
        playButton.setPreferredSize(new Dimension(40, 40));
        playButton.addActionListener(e -> playTrack(track, index));
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
                    playTrack(track, index);
                }
            }
        });

        return card;
    }

    private void playTrack(Track track, int index) {
        controller.setQueueAndPlay(currentSearchResults, index);
    }

    /**
     * Show initial content again
     */
    public void showInitialContent() {
        showingSearchResults = false;
        initialContentPanel.setVisible(true);
        featuredTrackPanel.setVisible(false);
        trackListPanel.setVisible(false);
        trackListPanel.removeAll();
        scrollPane.revalidate();
        scrollPane.repaint();
        scrollPane.getVerticalScrollBar().setValue(0);
        searchField.setText("");
    }
}