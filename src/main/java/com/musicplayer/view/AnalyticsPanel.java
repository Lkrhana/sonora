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

public class AnalyticsPanel extends JPanel {
    private MusicPlayerController controller;
    private AnalyticsDAO analyticsDAO;
    private JPanel chartsPanel;

    public AnalyticsPanel(MusicPlayerController controller) {
        this.controller = controller;
        this.analyticsDAO = controller.getAnalyticsDAO();
        
        setLayout(new BorderLayout());
        setBackground(new Color(18, 18, 18));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header with refresh button
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Charts panel
        chartsPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        chartsPanel.setBackground(new Color(18, 18, 18));
        add(chartsPanel, BorderLayout.CENTER);

        // Debug data saat pertama kali load
        analyticsDAO.debugPrintData();
        
        loadAnalytics();
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JLabel title = new JLabel("Analytics Dashboard");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Color.WHITE);

        JButton refreshBtn = new JButton("🔄 Refresh");
        refreshBtn.setBackground(new Color(30, 215, 96));
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.setBorderPainted(false);
        refreshBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        refreshBtn.addActionListener(e -> {
            System.out.println("\n🔄 Manual refresh triggered...");
            analyticsDAO.debugPrintData();
            loadAnalytics();
        });

        header.add(title, BorderLayout.WEST);
        header.add(refreshBtn, BorderLayout.EAST);

        return header;
    }

    public void loadAnalytics() {
        System.out.println("\n📊 Loading Analytics Dashboard...");
        chartsPanel.removeAll();

        // Chart 1: Top 5 Most Played Tracks
        System.out.println("\n1️⃣ Loading Top Tracks...");
        DefaultCategoryDataset topTracksData = new DefaultCategoryDataset();
        List<Map<String, Object>> topTracks = analyticsDAO.getTopTracks(5);
        
        if (topTracks.isEmpty()) {
            System.out.println("⚠️ No top tracks data found");
            topTracksData.addValue(1, "Plays", "No data - Start listening!");
        } else {
            System.out.println("✅ Adding " + topTracks.size() + " tracks to chart");
            for (Map<String, Object> track : topTracks) {
                String title = (String) track.get("title");
                String artist = (String) track.get("artist");
                int count = (int) track.get("count");
                
                // Format: "Artist - Title"
                String label = artist + " - " + title;
                if (label.length() > 30) {
                    label = label.substring(0, 27) + "...";
                }
                topTracksData.addValue(count, "Plays", label);
                System.out.println("   ✓ Added: " + label + " (" + count + " plays)");
            }
        }
        
        JFreeChart barChart = ChartFactory.createBarChart(
                "Top 5 Most Played Tracks",
                "Track",
                "Plays",
                topTracksData,
                PlotOrientation.VERTICAL,
                false, true, false
        );
        styleChart(barChart);
        chartsPanel.add(new ChartPanel(barChart));

        // Chart 2: Genre Distribution
        System.out.println("\n2️⃣ Loading Genre Distribution...");
        DefaultPieDataset genreData = new DefaultPieDataset();
        Map<String, Integer> genres = analyticsDAO.getGenreDistribution();
        
        if (genres.isEmpty()) {
            System.out.println("⚠️ No genre data found");
            genreData.setValue("No genre data", 1);
        } else {
            System.out.println("✅ Adding " + genres.size() + " genres to chart");
            genres.forEach((genre, count) -> {
                String displayGenre = (genre != null && !genre.isEmpty()) ? genre : "Unknown";
                genreData.setValue(displayGenre, count);
                System.out.println("   ✓ " + displayGenre + ": " + count);
            });
        }
        
        JFreeChart pieChart = ChartFactory.createPieChart(
                "Genre Distribution",
                genreData,
                true, true, false
        );
        styleChart(pieChart);
        chartsPanel.add(new ChartPanel(pieChart));

        // Chart 3: Plays Per Day (Last 7 days)
        System.out.println("\n3️⃣ Loading Plays Per Day...");
        DefaultCategoryDataset playsPerDayData = new DefaultCategoryDataset();
        Map<String, Integer> dailyPlays = analyticsDAO.getPlaysPerDay(7);
        
        if (dailyPlays.isEmpty()) {
            System.out.println("⚠️ No daily plays data found");
            playsPerDayData.addValue(1, "Plays", "No recent activity");
        } else {
            System.out.println("✅ Adding " + dailyPlays.size() + " days to chart");
            dailyPlays.forEach((day, count) -> {
                // Format date: YYYY-MM-DD -> MM/DD
                String[] parts = day.split("-");
                String shortDate = parts.length == 3 ? parts[1] + "/" + parts[2] : day;
                playsPerDayData.addValue(count, "Plays", shortDate);
                System.out.println("   ✓ " + shortDate + ": " + count + " plays");
            });
        }
        
        JFreeChart lineChart = ChartFactory.createLineChart(
                "Plays Per Day (Last 7 Days)",
                "Date",
                "Plays",
                playsPerDayData,
                PlotOrientation.VERTICAL,
                false, true, false
        );
        styleChart(lineChart);
        chartsPanel.add(new ChartPanel(lineChart));

        // Summary Panel
        System.out.println("\n4️⃣ Loading Summary Statistics...");
        JPanel summary = createSummaryPanel();
        chartsPanel.add(summary);

        chartsPanel.revalidate();
        chartsPanel.repaint();
        
        System.out.println("\n✅ Analytics dashboard loaded successfully!\n");
    }

    private void styleChart(JFreeChart chart) {
        chart.setBackgroundPaint(new Color(30, 30, 30));
        chart.getTitle().setPaint(Color.WHITE);
        chart.getTitle().setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        Plot plot = chart.getPlot();
        plot.setBackgroundPaint(new Color(40, 40, 40));
        plot.setOutlinePaint(new Color(60, 60, 60));
        
        if (plot instanceof CategoryPlot) {
            CategoryPlot categoryPlot = (CategoryPlot) plot;
            categoryPlot.setDomainGridlinePaint(new Color(60, 60, 60));
            categoryPlot.setRangeGridlinePaint(new Color(60, 60, 60));
            categoryPlot.getDomainAxis().setLabelPaint(Color.WHITE);
            categoryPlot.getDomainAxis().setTickLabelPaint(Color.LIGHT_GRAY);
            categoryPlot.getDomainAxis().setLabelFont(new Font("Segoe UI", Font.PLAIN, 11));
            categoryPlot.getDomainAxis().setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 9));
            categoryPlot.getRangeAxis().setLabelPaint(Color.WHITE);
            categoryPlot.getRangeAxis().setTickLabelPaint(Color.LIGHT_GRAY);
            categoryPlot.getRangeAxis().setLabelFont(new Font("Segoe UI", Font.PLAIN, 11));
            categoryPlot.getRangeAxis().setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 10));
            
            // Rotate labels if needed
            categoryPlot.getDomainAxis().setCategoryLabelPositions(
                org.jfree.chart.axis.CategoryLabelPositions.UP_45
            );
        } else if (plot instanceof PiePlot) {
            PiePlot piePlot = (PiePlot) plot;
            piePlot.setLabelBackgroundPaint(new Color(40, 40, 40));
            piePlot.setLabelPaint(Color.WHITE);
            piePlot.setLabelOutlinePaint(null);
            piePlot.setLabelShadowPaint(null);
            piePlot.setLabelFont(new Font("Segoe UI", Font.PLAIN, 10));
        }
    }

    private JPanel createSummaryPanel() {
        JPanel summary = new JPanel(new GridLayout(4, 1, 10, 10));
        summary.setBackground(new Color(30, 30, 30));
        summary.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 60), 1),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel titleLabel = new JLabel("Summary Statistics");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        int totalTime = analyticsDAO.getTotalListeningTime();
        int hours = totalTime / 3600;
        int minutes = (totalTime % 3600) / 60;
        String timeStr = String.format("%d hrs %d min", hours, minutes);
        System.out.println("   ⏱️ Total Time: " + timeStr);

        int totalTracks = analyticsDAO.getTotalPlayCount();
        System.out.println("   🎵 Total Plays: " + totalTracks);
        
        String mostActiveDay = analyticsDAO.getMostActiveDay();
        System.out.println("   📅 Most Active: " + mostActiveDay);

        JLabel listeningTime = createStyledLabel("⏱️ Total Time: " + timeStr);
        JLabel tracksPlayed = createStyledLabel("🎵 Total Plays: " + totalTracks);
        JLabel mostActive = createStyledLabel("📅 Most Active: " + mostActiveDay);

        summary.add(titleLabel);
        summary.add(listeningTime);
        summary.add(tracksPlayed);
        summary.add(mostActive);

        return summary;
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        label.setForeground(new Color(200, 200, 200));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }
}