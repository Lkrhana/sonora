package com.musicplayer.view;

import com.musicplayer.controller.MusicPlayerController;
import com.musicplayer.model.Playlist;
import com.musicplayer.model.Track;
import com.musicplayer.util.IconLoader;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class PlaylistPanel extends JPanel {
    private MusicPlayerController controller;
    private MainFrame mainFrame;
    
    // Left sidebar components
    private JPanel playlistsSidebar;
    private JList<Playlist> playlistList;
    private DefaultListModel<Playlist> playlistListModel;
    private JButton createPlaylistButton;
    private JButton deletePlaylistButton;
    
    // Main content components
    private JPanel mainContent;
    private JLabel playlistNameLabel;
    private JLabel playlistInfoLabel;
    private JTable tracksTable;
    private DefaultTableModel tracksTableModel;
    private JButton playAllButton;
    private JButton editPlaylistButton;
    private JButton removeTrackButton;
    
    private Playlist currentPlaylist;
    private Timer refreshTimer;

    public PlaylistPanel(MusicPlayerController controller) {
        this.controller = controller;
        initializeComponents();
        loadPlaylists();
        setupRefreshTimer();
    }
    
    public void setMainFrame(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    private void initializeComponents() {
        setLayout(new BorderLayout(0, 0));
        setBackground(new Color(18, 18, 20));

        // Left sidebar - Playlists list
        playlistsSidebar = createPlaylistsSidebar();
        add(playlistsSidebar, BorderLayout.WEST);

        // Main content - Selected playlist tracks
        mainContent = createMainContent();
        add(mainContent, BorderLayout.CENTER);
    }

    private JPanel createPlaylistsSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout(0, 10));
        sidebar.setPreferredSize(new Dimension(280, 0));
        sidebar.setBackground(new Color(25, 25, 28));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(50, 50, 55)));

        // Top panel - Title & Create button
        JPanel topPanel = new JPanel(new MigLayout("fillx, insets 15", "[grow][]", ""));
        topPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("MY PLAYLISTS");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        topPanel.add(titleLabel, "grow");

        createPlaylistButton = new JButton("+");
        createPlaylistButton.setFont(new Font("Segoe UI", Font.BOLD, 20));
        createPlaylistButton.setFocusPainted(false);
        createPlaylistButton.setPreferredSize(new Dimension(40, 40));
        createPlaylistButton.setBackground(new Color(100, 100, 255));
        createPlaylistButton.setForeground(Color.WHITE);
        createPlaylistButton.setBorderPainted(false);
        createPlaylistButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        createPlaylistButton.setToolTipText("Create New Playlist");
        createPlaylistButton.addActionListener(e -> createNewPlaylist());
        topPanel.add(createPlaylistButton);

        sidebar.add(topPanel, BorderLayout.NORTH);

        // Center - Playlists list
        playlistListModel = new DefaultListModel<>();
        playlistList = new JList<>(playlistListModel);
        playlistList.setBackground(new Color(25, 25, 28));
        playlistList.setForeground(Color.WHITE);
        playlistList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        playlistList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        playlistList.setCellRenderer(new PlaylistCellRenderer());
        playlistList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectPlaylist();
            }
        });

        JScrollPane scrollPane = new JScrollPane(playlistList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(new Color(25, 25, 28));
        sidebar.add(scrollPane, BorderLayout.CENTER);

        // Bottom - Delete button
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setOpaque(false);

        deletePlaylistButton = new JButton("Delete Playlist");
        deletePlaylistButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        deletePlaylistButton.setFocusPainted(false);
        deletePlaylistButton.setBackground(new Color(180, 50, 50));
        deletePlaylistButton.setForeground(Color.WHITE);
        deletePlaylistButton.setBorderPainted(false);
        deletePlaylistButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        deletePlaylistButton.setEnabled(false);
        deletePlaylistButton.addActionListener(e -> deleteCurrentPlaylist());
        bottomPanel.add(deletePlaylistButton);

        sidebar.add(bottomPanel, BorderLayout.SOUTH);

        return sidebar;
    }

    private JPanel createMainContent() {
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBackground(new Color(18, 18, 20));
        content.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Top panel - Playlist info & actions
        JPanel topPanel = createTopPanel();
        content.add(topPanel, BorderLayout.NORTH);

        // Center - Tracks table
        JPanel centerPanel = createTracksTable();
        content.add(centerPanel, BorderLayout.CENTER);

        // Bottom - Track actions
        JPanel bottomPanel = createBottomPanel();
        content.add(bottomPanel, BorderLayout.SOUTH);

        return content;
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new MigLayout("fillx", "[]push[]10[]", "[]10[]"));
        panel.setOpaque(false);

        // Playlist name
        playlistNameLabel = new JLabel("Select a playlist");
        playlistNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        playlistNameLabel.setForeground(Color.WHITE);
        panel.add(playlistNameLabel, "wrap");

        // Playlist info
        playlistInfoLabel = new JLabel("No playlist selected");
        playlistInfoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        playlistInfoLabel.setForeground(new Color(150, 150, 150));
        panel.add(playlistInfoLabel);

        // Play All button
        playAllButton = new JButton("Play All", IconLoader.loadButtonIcon(IconLoader.Icons.PLAY));
        playAllButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
        playAllButton.setFocusPainted(false);
        playAllButton.setBackground(new Color(100, 100, 255));
        playAllButton.setForeground(Color.WHITE);
        playAllButton.setBorderPainted(false);
        playAllButton.setPreferredSize(new Dimension(140, 45));
        playAllButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        playAllButton.setEnabled(false);
        playAllButton.addActionListener(e -> playCurrentPlaylist());
        panel.add(playAllButton, "spany 2");

        // Edit button
        editPlaylistButton = new JButton("Edit");
        editPlaylistButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        editPlaylistButton.setFocusPainted(false);
        editPlaylistButton.setBackground(new Color(60, 60, 70));
        editPlaylistButton.setForeground(Color.WHITE);
        editPlaylistButton.setBorderPainted(false);
        editPlaylistButton.setPreferredSize(new Dimension(80, 35));
        editPlaylistButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        editPlaylistButton.setEnabled(false);
        editPlaylistButton.addActionListener(e -> editCurrentPlaylist());
        panel.add(editPlaylistButton, "spany 2");

        return panel;
    }


    private JPanel createTracksTable() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        String[] columnNames = {"#", "Title", "Artist", "Album", "Duration"};
        tracksTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tracksTable = new JTable(tracksTableModel);
        tracksTable.setBackground(new Color(30, 30, 35));
        tracksTable.setForeground(Color.WHITE);
        tracksTable.setSelectionBackground(new Color(60, 60, 70));
        tracksTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tracksTable.setRowHeight(35);
        tracksTable.setShowGrid(false);
        tracksTable.getTableHeader().setBackground(new Color(40, 40, 45));
        tracksTable.getTableHeader().setForeground(Color.WHITE);
        tracksTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Column widths
        tracksTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        tracksTable.getColumnModel().getColumn(0).setMaxWidth(50);
        tracksTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        tracksTable.getColumnModel().getColumn(4).setMaxWidth(100);

        tracksTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                removeTrackButton.setEnabled(tracksTable.getSelectedRow() != -1);
            }
        });

        // Double-click to play
        tracksTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    playSelectedTrack();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(tracksTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(50, 50, 55)));
        scrollPane.getViewport().setBackground(new Color(30, 30, 35));

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setOpaque(false);

        removeTrackButton = new JButton("Remove from Playlist");
        removeTrackButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        removeTrackButton.setFocusPainted(false);
        removeTrackButton.setBackground(new Color(60, 60, 70));
        removeTrackButton.setForeground(Color.WHITE);
        removeTrackButton.setBorderPainted(false);
        removeTrackButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        removeTrackButton.setEnabled(false);
        removeTrackButton.addActionListener(e -> removeSelectedTrack());
        panel.add(removeTrackButton);

        return panel;
    }

    /**
     * Custom cell renderer for playlist list
     */
    private class PlaylistCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof Playlist) {
                Playlist playlist = (Playlist) value;
                setText("<html><b>" + playlist.getName() + "</b><br>" +
                        "<small style='color: #888;'>" + playlist.getTrackCount() + " tracks</small></html>");
            }

            if (isSelected) {
                setBackground(new Color(50, 50, 60));
            } else {
                setBackground(new Color(25, 25, 28));
            }

            setForeground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(40, 40, 45)),
                    BorderFactory.createEmptyBorder(10, 15, 10, 15)
            ));

            return this;
        }
    }

    /**
     * Load all playlists from database
     */
    private void loadPlaylists() {
        playlistListModel.clear();
        List<Playlist> playlists = controller.getAllPlaylists();

        for (Playlist playlist : playlists) {
            playlistListModel.addElement(playlist);
        }

        System.out.println("📋 Loaded " + playlists.size() + " playlists");
    }

    /**
     * Select playlist and load its tracks
     */
    private void selectPlaylist() {
        currentPlaylist = playlistList.getSelectedValue();

        if (currentPlaylist != null) {
            displayPlaylist(currentPlaylist);
            deletePlaylistButton.setEnabled(true);
            playAllButton.setEnabled(currentPlaylist.getTrackCount() > 0);
            editPlaylistButton.setEnabled(true);
        } else {
            clearPlaylistDisplay();
            deletePlaylistButton.setEnabled(false);
            playAllButton.setEnabled(false);
            editPlaylistButton.setEnabled(false);
        }
    }

    private void displayPlaylist(Playlist playlist) {
        playlistNameLabel.setText(playlist.getName());

        String info = playlist.getDescription() != null ? playlist.getDescription() + " • " : "";
        info += playlist.getTrackCount() + " tracks";
        playlistInfoLabel.setText(info);

        // Load tracks
        tracksTableModel.setRowCount(0);
        List<Track> tracks = playlist.getTracks();

        for (int i = 0; i < tracks.size(); i++) {
            Track track = tracks.get(i);
            Object[] rowData = {
                    (i + 1),
                    track.getTitle(),
                    track.getArtist(),
                    track.getAlbum() != null ? track.getAlbum() : "-",
                    formatDuration(track.getDuration())
            };
            tracksTableModel.addRow(rowData);
        }

        removeTrackButton.setEnabled(false);
    }

    private void clearPlaylistDisplay() {
        playlistNameLabel.setText("Select a playlist");
        playlistInfoLabel.setText("No playlist selected");
        tracksTableModel.setRowCount(0);
        removeTrackButton.setEnabled(false);
    }

    private void createNewPlaylist() {
        JTextField nameField = new JTextField(20);
        JTextArea descField = new JTextArea(3, 20);
        descField.setLineWrap(true);
        descField.setWrapStyleWord(true);

        JPanel panel = new JPanel(new MigLayout("fillx, wrap 2", "[][grow]", ""));
        panel.add(new JLabel("Name:"));
        panel.add(nameField, "growx");
        panel.add(new JLabel("Description:"));
        panel.add(new JScrollPane(descField), "growx");

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Create New Playlist", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String description = descField.getText().trim();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Playlist name cannot be empty",
                        "Invalid Input", JOptionPane.WARNING_MESSAGE);
                return;
            }

            long playlistId = controller.createPlaylist(name, description);

            if (playlistId > 0) {
                loadPlaylists();
                JOptionPane.showMessageDialog(this, "Playlist created successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to create playlist",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void editCurrentPlaylist() {
        if (currentPlaylist == null) return;

        JTextField nameField = new JTextField(currentPlaylist.getName(), 20);
        JTextArea descField = new JTextArea(currentPlaylist.getDescription() != null ? currentPlaylist.getDescription() : "", 3, 20);
        descField.setLineWrap(true);
        descField.setWrapStyleWord(true);

        JPanel panel = new JPanel(new MigLayout("fillx, wrap 2", "[][grow]", ""));
        panel.add(new JLabel("Name:"));
        panel.add(nameField, "growx");
        panel.add(new JLabel("Description:"));
        panel.add(new JScrollPane(descField), "growx");

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Edit Playlist", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String description = descField.getText().trim();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Playlist name cannot be empty",
                        "Invalid Input", JOptionPane.WARNING_MESSAGE);
                return;
            }

            currentPlaylist.setName(name);
            currentPlaylist.setDescription(description);

            if (controller.updatePlaylist(currentPlaylist)) {
                loadPlaylists();
                displayPlaylist(currentPlaylist);
                JOptionPane.showMessageDialog(this, "Playlist updated successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void deleteCurrentPlaylist() {
        if (currentPlaylist == null) return;

        int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete playlist \"" + currentPlaylist.getName() + "\"?",
                "Delete Playlist", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            if (controller.deletePlaylist(currentPlaylist.getId())) {
                loadPlaylists();
                clearPlaylistDisplay();
                currentPlaylist = null;
                JOptionPane.showMessageDialog(this, "Playlist deleted successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void playCurrentPlaylist() {
        if (currentPlaylist != null && currentPlaylist.getTrackCount() > 0) {
            controller.playPlaylist(currentPlaylist);
        }
    }

    private void playSelectedTrack() {
        int selectedRow = tracksTable.getSelectedRow();
        if (selectedRow != -1 && currentPlaylist != null) {
            controller.playPlaylistFromTrack(currentPlaylist, selectedRow);
        }
    }

    private void removeSelectedTrack() {
        int selectedRow = tracksTable.getSelectedRow();
        if (selectedRow == -1 || currentPlaylist == null) {
            System.out.println("⚠️ No track selected or no playlist");
            return;
        }

        Track track = currentPlaylist.getTracks().get(selectedRow);

        int result = JOptionPane.showConfirmDialog(this,
                "Remove \"" + track.getTitle() + "\" from playlist?",
                "Remove Track", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            boolean success = controller.removeTrackFromPlaylist(currentPlaylist.getId(), track.getId());

            if (success) {
                // ========== IMMEDIATE REFRESH - UPDATED ==========
                // Remove from UI immediately (optimistic update)
                tracksTableModel.removeRow(selectedRow);

                // Update track count in sidebar
                currentPlaylist.getTracks().remove(selectedRow);
                playlistList.repaint();

                // Update info label
                String info = currentPlaylist.getDescription() != null ? currentPlaylist.getDescription() + " • " : "";
                info += currentPlaylist.getTrackCount() + " tracks";
                playlistInfoLabel.setText(info);

                // Reload from database (sync with DB)
                SwingUtilities.invokeLater(() -> {
                    Playlist reloadedPlaylist = controller.getPlaylist(currentPlaylist.getId());
                    if (reloadedPlaylist != null) {
                        currentPlaylist = reloadedPlaylist;
                        System.out.println("✅ Playlist synced with database");
                    }
                });
                // ========== END IMMEDIATE REFRESH ==========

                System.out.println("✅ Track removed from UI");

            } else {
                System.err.println("❌ Failed to remove track");
                JOptionPane.showMessageDialog(this, 
                    "Failed to remove track from playlist",
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String formatDuration(int seconds) {
        if (seconds <= 0) return "--:--";
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", minutes, secs);
    }

    private void setupRefreshTimer() {
        refreshTimer = new Timer(3000, e -> { // 3 seconds
            // Only refresh if playlist count might be out of sync
            // This is a backup for optimistic updates
            if (currentPlaylist != null) {
                com.musicplayer.model.Playlist reloadedPlaylist = controller.getPlaylist(currentPlaylist.getId());
                if (reloadedPlaylist != null) {
                    // Check if counts match
                    if (reloadedPlaylist.getTrackCount() != currentPlaylist.getTrackCount()) {
                        // Counts don't match, do full refresh
                        currentPlaylist = reloadedPlaylist;
                        displayPlaylist(currentPlaylist);

                        // Update sidebar
                        for (int i = 0; i < playlistListModel.getSize(); i++) {
                            if (playlistListModel.getElementAt(i).getId() == currentPlaylist.getId()) {
                                playlistListModel.set(i, currentPlaylist);
                                playlistList.repaint();
                                break;
                            }
                        }

                        System.out.println("🔄 Background sync: playlist updated");
                    }
                }
            }
        });
        refreshTimer.start();
    }

    public void refreshPlaylists() {
        int selectedIndex = playlistList.getSelectedIndex();
        long selectedPlaylistId = currentPlaylist != null ? currentPlaylist.getId() : -1;

        // Reload all playlists from database
        loadPlaylists();

        // Restore selection
        if (selectedPlaylistId > 0) {
            for (int i = 0; i < playlistListModel.getSize(); i++) {
                if (playlistListModel.getElementAt(i).getId() == selectedPlaylistId) {
                    playlistList.setSelectedIndex(i);

                    // Force reload current playlist to show new tracks
                    currentPlaylist = controller.getPlaylist(selectedPlaylistId);
                    if (currentPlaylist != null) {
                        displayPlaylist(currentPlaylist);
                    }
                    break;
                }
            }
        }

        // Force repaint sidebar
        playlistList.repaint();
        playlistList.revalidate();

        System.out.println("🔄 Playlists refreshed with updated counts");
    }
    
    public void addTrackToCurrentPlaylistUI(Track track, long playlistId) {
        // Check if this is the currently displayed playlist
        if (currentPlaylist != null && currentPlaylist.getId() == playlistId) {
            // Add to tracks list
            currentPlaylist.getTracks().add(track);

            // Add to table
            int newIndex = currentPlaylist.getTracks().size();
            Object[] rowData = {
                newIndex,
                track.getTitle(),
                track.getArtist(),
                track.getAlbum() != null ? track.getAlbum() : "-",
                formatDuration(track.getDuration())
            };
            tracksTableModel.addRow(rowData);

            // Update info label
            String info = currentPlaylist.getDescription() != null ? currentPlaylist.getDescription() + " • " : "";
            info += currentPlaylist.getTrackCount() + " tracks";
            playlistInfoLabel.setText(info);

            System.out.println("✅ Track added to UI immediately");
        }

        // Update sidebar count (find and update the playlist in list)
        for (int i = 0; i < playlistListModel.getSize(); i++) {
            com.musicplayer.model.Playlist p = playlistListModel.getElementAt(i);
            if (p.getId() == playlistId) {
                // Reload this specific playlist to get updated count
                com.musicplayer.model.Playlist updated = controller.getPlaylist(playlistId);
                if (updated != null) {
                    playlistListModel.set(i, updated);
                    playlistList.repaint();
                }
                break;
            }
        }
    }
}
