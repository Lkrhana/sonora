package com.musicplayer.view;

import com.musicplayer.controller.MusicPlayerController;
import com.musicplayer.repository.AnalyticsDAO;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.chart.renderer.category.BarRenderer;

public class AnalyticsPanel extends JPanel {
    private MusicPlayerController controller;
    private AnalyticsDAO analyticsDAO;
    private JPanel chartsPanel;
    
    // Color scheme
    private static final Color BG_PRIMARY = new Color(18, 18, 18);
    private static final Color BG_SECONDARY = new Color(30, 30, 30);
    private static final Color BG_CHART = new Color(40, 40, 40);
    private static final Color ACCENT_GREEN = new Color(30, 215, 96);
    private static final Color TEXT_PRIMARY = Color.WHITE;
    private static final Color TEXT_SECONDARY = new Color(180, 180, 180);
    private static final Color BORDER_COLOR = new Color(60, 60, 60);

    public AnalyticsPanel(MusicPlayerController controller) {
        this.controller = controller;
        
        // ✅ Initialize AnalyticsDAO with error handling
        try {
            this.analyticsDAO = new AnalyticsDAO();
            System.out.println("✅ AnalyticsDAO initialized successfully");
        } catch (Exception e) {
            System.err.println("❌ Failed to initialize AnalyticsDAO: " + e.getMessage());
            e.printStackTrace();
            this.analyticsDAO = null;
        }
        
        initializeUI();
        
        // ✅ Load analytics only once after UI is ready
        SwingUtilities.invokeLater(() -> loadAnalytics());
    }
    
    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(BG_PRIMARY);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header with refresh button
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Charts panel with scroll
        chartsPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        chartsPanel.setBackground(BG_PRIMARY);
        
        JScrollPane scrollPane = new JScrollPane(chartsPanel);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBackground(BG_PRIMARY);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JLabel title = new JLabel("📊 Analytics Dashboard");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(TEXT_PRIMARY);

        JButton refreshBtn = createStyledButton("🔄 Refresh");
        refreshBtn.addActionListener(e -> {
            refreshBtn.setEnabled(false);
            refreshBtn.setText("Loading...");
            
            // Load in background thread
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    loadAnalytics();
                    return null;
                }
                
                @Override
                protected void done() {
                    refreshBtn.setEnabled(true);
                    refreshBtn.setText("🔄 Refresh");
                    JOptionPane.showMessageDialog(
                        AnalyticsPanel.this,
                        "Analytics refreshed successfully!",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                }
            };
            worker.execute();
        });

        header.add(title, BorderLayout.WEST);
        header.add(refreshBtn, BorderLayout.EAST);

        return header;
    }
    
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(ACCENT_GREEN);
        button.setForeground(TEXT_PRIMARY);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setPreferredSize(new Dimension(120, 35));
        
        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(ACCENT_GREEN.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(ACCENT_GREEN);
            }
        });
        
        return button;
    }

    public void loadAnalytics() {
        // ✅ Check if AnalyticsDAO is initialized
        if (analyticsDAO == null) {
            System.err.println("❌ AnalyticsDAO is null. Cannot load analytics.");
            showErrorMessage("Analytics service is not available. Please restart the application.");
            return;
        }
        
        try {
            System.out.println("🔍 Loading analytics data...");
            
            // Clear existing charts
            SwingUtilities.invokeLater(() -> {
                chartsPanel.removeAll();
                
                // Add loading indicator
                JLabel loadingLabel = new JLabel("Loading analytics...", SwingConstants.CENTER);
                loadingLabel.setForeground(TEXT_SECONDARY);
                loadingLabel.setFont(new Font("Segoe UI", Font.ITALIC, 16));
                chartsPanel.add(loadingLabel);
                chartsPanel.revalidate();
                chartsPanel.repaint();
            });
            
            // Chart 1: Top 5 Most Played Tracks
            JFreeChart topTracksChart = createTopTracksChart();
            
            // Chart 2: Genre Distribution
            JFreeChart genreChart = createGenreChart();
            
            // Chart 3: Plays Per Day (Last 7 days)
            JFreeChart dailyPlaysChart = createDailyPlaysChart();
            
            // Chart 4: Summary Panel
            JPanel summaryPanel = createSummaryPanel();
            
            // Update UI on EDT
            SwingUtilities.invokeLater(() -> {
                chartsPanel.removeAll();
                chartsPanel.add(new ChartPanel(topTracksChart));
                chartsPanel.add(new ChartPanel(genreChart));
                chartsPanel.add(new ChartPanel(dailyPlaysChart));
                chartsPanel.add(summaryPanel);
                chartsPanel.revalidate();
                chartsPanel.repaint();
            });
            
            System.out.println("✅ Analytics dashboard loaded successfully");
            
        } catch (Exception e) {
            System.err.println("❌ Error loading analytics: " + e.getMessage());
            e.printStackTrace();
            showErrorMessage("Error loading analytics: " + e.getMessage());
        }
    }
    
    private JFreeChart createTopTracksChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        List<Map<String, Object>> topTracks = analyticsDAO.getTopTracks(5);
        
        if (topTracks == null || topTracks.isEmpty()) {
            dataset.addValue(0, "Plays", "No data yet");
        } else {
            for (Map<String, Object> track : topTracks) {
                String title = (String) track.get("title");
                String artist = (String) track.get("artist");
                Integer count = (Integer) track.get("count");
                
                if (title == null) title = "Unknown";
                if (artist == null) artist = "Unknown";
                if (count == null) count = 0;
                
                // Format: "Title - Artist"
                String label = title + " - " + artist;
                if (label.length() > 30) {
                    label = label.substring(0, 27) + "...";
                }
                dataset.addValue(count, "Plays", label);
            }
        }
        
        JFreeChart chart = ChartFactory.createBarChart(
            "🎵 Top 5 Most Played Tracks",
            "Track",
            "Plays",
            dataset,
            PlotOrientation.VERTICAL,
            false, true, false
        );
        
        styleChart(chart);
        
        // Custom bar colors
        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, ACCENT_GREEN);
        renderer.setBarPainter(new org.jfree.chart.renderer.category.StandardBarPainter());
        
        return chart;
    }
    
    private JFreeChart createGenreChart() {
        DefaultPieDataset dataset = new DefaultPieDataset();
        Map<String, Integer> genres = analyticsDAO.getGenreDistribution();
        
        if (genres == null || genres.isEmpty()) {
            dataset.setValue("No genre data", 1);
        } else {
            // Limit to top 6 genres + "Others"
            int count = 0;
            int othersCount = 0;
            
            for (Map.Entry<String, Integer> entry : genres.entrySet()) {
                if (count < 6) {
                    String displayGenre = (entry.getKey() != null && !entry.getKey().isEmpty()) 
                        ? entry.getKey() : "Unknown";
                    dataset.setValue(displayGenre, entry.getValue());
                    count++;
                } else {
                    othersCount += entry.getValue();
                }
            }
            
            if (othersCount > 0) {
                dataset.setValue("Others", othersCount);
            }
        }
        
        JFreeChart chart = ChartFactory.createPieChart(
            "🎸 Genre Distribution",
            dataset,
            true, true, false
        );
        
        styleChart(chart);
        
        // Custom pie colors
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setSectionPaint("Rock", new Color(231, 76, 60));
        plot.setSectionPaint("Pop", new Color(52, 152, 219));
        plot.setSectionPaint("Jazz", new Color(155, 89, 182));
        plot.setSectionPaint("Electronic", new Color(26, 188, 156));
        plot.setSectionPaint("Classical", new Color(241, 196, 15));
        plot.setSectionPaint("Hip Hop", new Color(230, 126, 34));
        plot.setSectionPaint("Others", new Color(149, 165, 166));
        
        return chart;
    }
    
    private JFreeChart createDailyPlaysChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        Map<String, Integer> dailyPlays = analyticsDAO.getPlaysPerDay(7);
        
        if (dailyPlays == null || dailyPlays.isEmpty()) {
            dataset.addValue(0, "Plays", "No recent activity");
        } else {
            dailyPlays.forEach((day, count) -> {
                // Format date: YYYY-MM-DD -> MM/DD
                String shortDate = day;
                try {
                    String[] parts = day.split("-");
                    if (parts.length == 3) {
                        shortDate = parts[1] + "/" + parts[2];
                    }
                } catch (Exception e) {
                    // Keep original format if parsing fails
                }
                dataset.addValue(count, "Plays", shortDate);
            });
        }
        
        JFreeChart chart = ChartFactory.createLineChart(
            "📅 Plays Per Day (Last 7 Days)",
            "Date",
            "Plays",
            dataset,
            PlotOrientation.VERTICAL,
            false, true, false
        );
        
        styleChart(chart);
        
        // Custom line color
        CategoryPlot plot = chart.getCategoryPlot();
        plot.getRenderer().setSeriesPaint(0, ACCENT_GREEN);
        
        return chart;
    }

    private void styleChart(JFreeChart chart) {
        chart.setBackgroundPaint(BG_SECONDARY);
        chart.getTitle().setPaint(TEXT_PRIMARY);
        chart.getTitle().setFont(new Font("Segoe UI", Font.BOLD, 16));
        
        Plot plot = chart.getPlot();
        plot.setBackgroundPaint(BG_CHART);
        plot.setOutlinePaint(BORDER_COLOR);
        
        if (plot instanceof CategoryPlot) {
            CategoryPlot categoryPlot = (CategoryPlot) plot;
            categoryPlot.setDomainGridlinePaint(BORDER_COLOR);
            categoryPlot.setRangeGridlinePaint(BORDER_COLOR);
            
            // Domain axis (X)
            categoryPlot.getDomainAxis().setLabelPaint(TEXT_PRIMARY);
            categoryPlot.getDomainAxis().setTickLabelPaint(TEXT_SECONDARY);
            categoryPlot.getDomainAxis().setLabelFont(new Font("Segoe UI", Font.PLAIN, 12));
            categoryPlot.getDomainAxis().setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 10));
            categoryPlot.getDomainAxis().setCategoryLabelPositions(
                org.jfree.chart.axis.CategoryLabelPositions.UP_45
            );
            
            // Range axis (Y)
            categoryPlot.getRangeAxis().setLabelPaint(TEXT_PRIMARY);
            categoryPlot.getRangeAxis().setTickLabelPaint(TEXT_SECONDARY);
            categoryPlot.getRangeAxis().setLabelFont(new Font("Segoe UI", Font.PLAIN, 12));
            categoryPlot.getRangeAxis().setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 10));
            
        } else if (plot instanceof PiePlot) {
            PiePlot piePlot = (PiePlot) plot;
            piePlot.setLabelBackgroundPaint(BG_CHART);
            piePlot.setLabelPaint(TEXT_PRIMARY);
            piePlot.setLabelOutlinePaint(null);
            piePlot.setLabelShadowPaint(null);
            piePlot.setLabelFont(new Font("Segoe UI", Font.PLAIN, 11));
            piePlot.setSimpleLabels(true);
        }
    }

    private JPanel createSummaryPanel() {
        JPanel summary = new JPanel();
        summary.setLayout(new BoxLayout(summary, BoxLayout.Y_AXIS));
        summary.setBackground(BG_SECONDARY);
        summary.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(25, 20, 25, 20)
        ));

        // Title
        JLabel titleLabel = new JLabel("📈 Summary Statistics");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        summary.add(titleLabel);
        
        summary.add(Box.createVerticalStrut(20));
        summary.add(createSeparator());
        summary.add(Box.createVerticalStrut(15));

        // Stats
        try {
            int totalTime = analyticsDAO.getTotalListeningTime();
            int hours = totalTime / 3600;
            int minutes = (totalTime % 3600) / 60;
            String timeStr = String.format("%dh %dm", hours, minutes);

            int totalTracks = analyticsDAO.getTotalPlayCount();
            int uniqueTracks = analyticsDAO.getUniqueTracksPlayed();
            String mostActiveDay = analyticsDAO.getMostActiveDay();
            int avgBPM = analyticsDAO.getAverageBPM();

            summary.add(createStatRow("⏱️", "Total Listening Time", timeStr));
            summary.add(Box.createVerticalStrut(12));
            
            summary.add(createStatRow("🎵", "Total Plays", String.valueOf(totalTracks)));
            summary.add(Box.createVerticalStrut(12));
            
            summary.add(createStatRow("🎼", "Unique Tracks", String.valueOf(uniqueTracks)));
            summary.add(Box.createVerticalStrut(12));
            
            summary.add(createStatRow("📅", "Most Active Day", mostActiveDay));
            summary.add(Box.createVerticalStrut(12));
            
            if (avgBPM > 0) {
                summary.add(createStatRow("🎚️", "Average BPM", String.valueOf(avgBPM)));
            }
            
        } catch (Exception e) {
            JLabel errorLabel = new JLabel("Error loading stats");
            errorLabel.setForeground(new Color(231, 76, 60));
            errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            summary.add(errorLabel);
        }

        return summary;
    }
    
    private JPanel createStatRow(String emoji, String label, String value) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setOpaque(false);
        row.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel emojiLabel = new JLabel(emoji + " " + label);
        emojiLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        emojiLabel.setForeground(TEXT_SECONDARY);
        emojiLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        valueLabel.setForeground(TEXT_PRIMARY);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        row.add(emojiLabel);
        row.add(Box.createVerticalStrut(3));
        row.add(valueLabel);
        
        return row;
    }
    
    private JSeparator createSeparator() {
        JSeparator separator = new JSeparator();
        separator.setForeground(BORDER_COLOR);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return separator;
    }
    
    private void showErrorMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(
                this,
                message,
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        });
    }
}