package com.musicplayer.view;

import com.musicplayer.controller.MusicPlayerController;
import com.musicplayer.model.Track;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.List;

public class QueuePanel extends JPanel {
    private MusicPlayerController controller;
    
    // UI Components
    private JTable queueTable;
    private DefaultTableModel tableModel;
    private JLabel queueInfoLabel;
    private JButton clearQueueButton;
    private JButton removeSelectedButton;
    private JButton moveUpButton;
    private JButton moveDownButton;
    
    private Timer refreshTimer;
    private boolean hasScrolledToCurrentTrack = false;

    public QueuePanel(MusicPlayerController controller) {
        this.controller = controller;
        initializeComponents();
        setupRefreshTimer();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(18, 18, 20));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Top panel - Header & controls
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        // Center panel - Queue table
        JPanel centerPanel = createCenterPanel();
        add(centerPanel, BorderLayout.CENTER);

        // Bottom panel - Actions
        JPanel bottomPanel = createBottomPanel();
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new MigLayout("fillx", "[]push[]", ""));
        panel.setOpaque(false);

        // Title
        JLabel titleLabel = new JLabel("PLAY QUEUE");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);

        // Queue info
        queueInfoLabel = new JLabel("0 tracks in queue");
        queueInfoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        queueInfoLabel.setForeground(new Color(150, 150, 150));

        panel.add(titleLabel);
        panel.add(queueInfoLabel);

        return panel;
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        // Table model
        String[] columnNames = {"#", "Title", "Artist", "Album", "Duration"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Read-only
            }
        };

        // Table
        queueTable = new JTable(tableModel);
        queueTable.setBackground(new Color(30, 30, 35));
        queueTable.setForeground(Color.WHITE);
        queueTable.setSelectionBackground(new Color(60, 60, 70));
        queueTable.setSelectionForeground(Color.WHITE);
        queueTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        queueTable.setRowHeight(35);
        queueTable.setShowGrid(false);
        queueTable.setIntercellSpacing(new Dimension(0, 0));
        queueTable.getTableHeader().setBackground(new Color(40, 40, 45));
        queueTable.getTableHeader().setForeground(Color.WHITE);
        queueTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        
        queueTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

                java.awt.Component c = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

                int currentIndex = controller.getCurrentQueueIndex();

                // Current playing track - different style
                if (row == currentIndex) {
                    if (isSelected) {
                        // Selected + Current = Purple/Blue blend
                        c.setBackground(new Color(80, 100, 150));
                    } else {
                        // Current but not selected = Green highlight
                        c.setBackground(new Color(50, 100, 80));
                    }
                    c.setForeground(Color.WHITE);
                    setFont(getFont().deriveFont(Font.BOLD));
                } 
                // Selected (not current)
                else if (isSelected) {
                    c.setBackground(new Color(60, 60, 70));
                    c.setForeground(Color.WHITE);
                    setFont(getFont().deriveFont(Font.PLAIN));
                } 
                // Normal
                else {
                    c.setBackground(new Color(30, 30, 35));
                    c.setForeground(Color.WHITE);
                    setFont(getFont().deriveFont(Font.PLAIN));
                }

                return c;
            }
        });

        // Column widths
        TableColumn col0 = queueTable.getColumnModel().getColumn(0);
        col0.setPreferredWidth(40);
        col0.setMaxWidth(50);

        TableColumn col4 = queueTable.getColumnModel().getColumn(4);
        col4.setPreferredWidth(80);
        col4.setMaxWidth(100);

        // Double-click to play
        queueTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    playSelectedTrack();
                }
            }
        });

        // Scroll pane
        JScrollPane scrollPane = new JScrollPane(queueTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(50, 50, 55)));
        scrollPane.getViewport().setBackground(new Color(30, 30, 35));

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new MigLayout("fillx", "[]10[]push[]10[]10[]", ""));
        panel.setOpaque(false);

        // Remove selected button
        removeSelectedButton = new JButton("Remove Selected");
        removeSelectedButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        removeSelectedButton.setFocusPainted(false);
        removeSelectedButton.addActionListener(e -> removeSelected());
        styleButton(removeSelectedButton);

        // Clear queue button
        clearQueueButton = new JButton("Clear Queue");
        clearQueueButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        clearQueueButton.setFocusPainted(false);
        clearQueueButton.addActionListener(e -> clearQueue());
        styleButton(clearQueueButton);

        // Move up button
        moveUpButton = new JButton("↑ Move Up");
        moveUpButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        moveUpButton.setFocusPainted(false);
        moveUpButton.addActionListener(e -> moveUp());
        styleButton(moveUpButton);

        // Move down button
        moveDownButton = new JButton("↓ Move Down");
        moveDownButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        moveDownButton.setFocusPainted(false);
        moveDownButton.addActionListener(e -> moveDown());
        styleButton(moveDownButton);

        panel.add(removeSelectedButton);
        panel.add(clearQueueButton);
        panel.add(moveUpButton);
        panel.add(moveDownButton);

        return panel;
    }

    private void styleButton(JButton button) {
        button.setBackground(new Color(60, 60, 70));
        button.setForeground(Color.WHITE);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(130, 35));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(80, 80, 90));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(60, 60, 70));
            }
        });
    }

    private void setupRefreshTimer() {
        refreshTimer = new Timer(1000, e -> refreshQueue());
        refreshTimer.start();
    }

    public void refreshQueue() {
        List<Track> queue = controller.getQueue();
        int currentIndex = controller.getCurrentQueueIndex();

        // Update info label
        queueInfoLabel.setText(queue.size() + " track" + (queue.size() != 1 ? "s" : "") + " in queue");

        // Save current selection
        int selectedRow = queueTable.getSelectedRow();

        // Clear table
        tableModel.setRowCount(0);

        // Populate table
        for (int i = 0; i < queue.size(); i++) {
            Track track = queue.get(i);

            Object[] rowData = {
                (i + 1),
                track.getTitle(),
                track.getArtist(),
                track.getAlbum() != null ? track.getAlbum() : "-",
                formatDuration(track.getDuration())
            };

            tableModel.addRow(rowData);
        }

        // ========== RESTORE USER SELECTION - UPDATED ==========
        // Do NOT auto-select current track, let user control selection
        if (selectedRow >= 0 && selectedRow < queue.size()) {
            queueTable.setRowSelectionInterval(selectedRow, selectedRow);
        }

        // Auto-scroll to current track ONLY on first open
        if (!hasScrolledToCurrentTrack && currentIndex >= 0 && currentIndex < queue.size()) {
            queueTable.scrollRectToVisible(queueTable.getCellRect(currentIndex, 0, true));
            hasScrolledToCurrentTrack = true;
        }
        // ========== END ==========

        // Enable/disable buttons based on actual selection
        boolean hasSelection = queueTable.getSelectedRow() != -1;
        boolean hasQueue = queue.size() > 0;

        removeSelectedButton.setEnabled(hasSelection);
        clearQueueButton.setEnabled(hasQueue);
        moveUpButton.setEnabled(hasSelection && queueTable.getSelectedRow() > 0);
        moveDownButton.setEnabled(hasSelection && queueTable.getSelectedRow() < queue.size() - 1);
    }
    
    public void onPanelOpened() {
        hasScrolledToCurrentTrack = false; // Reset flag
        refreshQueue(); // Refresh and scroll to current
    }

    private String formatDuration(int seconds) {
        if (seconds <= 0) return "--:--";
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", minutes, secs);
    }

    private void playSelectedTrack() {
        int selectedRow = queueTable.getSelectedRow();
        if (selectedRow != -1) {
            List<Track> queue = controller.getQueue();
            if (selectedRow < queue.size()) {
                controller.setQueueAndPlay(queue, selectedRow);
                System.out.println("▶️ Playing track from queue at index: " + selectedRow);
            }
        }
    }

    private void removeSelected() {
        int selectedRow = queueTable.getSelectedRow();
        if (selectedRow != -1) {
            List<Track> queue = controller.getQueue();
            if (selectedRow < queue.size()) {
                queue.remove(selectedRow);
                System.out.println("🗑️ Removed track at index: " + selectedRow);
                refreshQueue();
            }
        }
    }

    private void clearQueue() {
        int result = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to clear the entire queue?",
            "Clear Queue",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (result == JOptionPane.YES_OPTION) {
            controller.clearQueue();
            refreshQueue();
            System.out.println("🗑️ Queue cleared");
        }
    }

    private void moveUp() {
        int selectedRow = queueTable.getSelectedRow();
        if (selectedRow > 0) {
            controller.moveTrackUp(selectedRow);
            refreshQueue();

            // Keep selection on moved track
            queueTable.setRowSelectionInterval(selectedRow - 1, selectedRow - 1);
            System.out.println("⬆️ Moved track up from index " + selectedRow + " to " + (selectedRow - 1));
        }
    }

    private void moveDown() {
        int selectedRow = queueTable.getSelectedRow();
        List<Track> queue = controller.getQueue();

        if (selectedRow >= 0 && selectedRow < queue.size() - 1) {
            controller.moveTrackDown(selectedRow);
            refreshQueue();

            // Keep selection on moved track
            queueTable.setRowSelectionInterval(selectedRow + 1, selectedRow + 1);
            System.out.println("⬇️ Moved track down from index " + selectedRow + " to " + (selectedRow + 1));
        }
    }
}
