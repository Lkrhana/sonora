package com.musicplayer.repository;

import java.sql.*;
import java.util.*;

public class AnalyticsDAO {
    
    private AnalyticsDatabaseManager dbManager;
    private Connection connection;
    
    public AnalyticsDAO() {
        this.dbManager = AnalyticsDatabaseManager.getInstance();
        this.connection = dbManager.getConnection();
        System.out.println("✅ AnalyticsDAO initialized with SQLite");
    }
    
    /**
     * Record a track play
     */
    public void recordPlay(String trackId, String title, String artist, String genre, int duration) {
        String sql = "INSERT INTO play_history (track_id, track_title, artist, genre, duration) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, trackId);
            pstmt.setString(2, title);
            pstmt.setString(3, artist);
            pstmt.setString(4, genre);
            pstmt.setInt(5, duration);
            pstmt.executeUpdate();
            
            System.out.println("📊 Analytics recorded: " + artist + " - " + title);
        } catch (SQLException e) {
            System.err.println("❌ Error recording play: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get top tracks
     */
    public List<Map<String, Object>> getTopTracks(int limit) {
        String sql = """
            SELECT track_title, artist, COUNT(*) as play_count 
            FROM play_history 
            GROUP BY track_id, track_title, artist 
            ORDER BY play_count DESC 
            LIMIT ?
        """;
        
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> track = new HashMap<>();
                track.put("title", rs.getString("track_title"));
                track.put("artist", rs.getString("artist"));
                track.put("count", rs.getInt("play_count"));
                results.add(track);
            }
            
            System.out.println("✅ Loaded " + results.size() + " top tracks");
        } catch (SQLException e) {
            System.err.println("❌ Error getting top tracks: " + e.getMessage());
            e.printStackTrace();
        }
        return results;
    }
    
    /**
     * Get genre distribution
     */
    public Map<String, Integer> getGenreDistribution() {
        String sql = """
            SELECT genre, COUNT(*) as count 
            FROM play_history 
            WHERE genre IS NOT NULL AND genre != '' AND genre != 'Music'
            GROUP BY genre 
            ORDER BY count DESC
        """;
        
        Map<String, Integer> distribution = new LinkedHashMap<>();
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String genre = rs.getString("genre");
                if (genre != null && !genre.trim().isEmpty()) {
                    distribution.put(genre, rs.getInt("count"));
                }
            }
            
            // If empty, show generic "Music"
            if (distribution.isEmpty()) {
                String countSql = "SELECT COUNT(*) as count FROM play_history";
                try (ResultSet rs2 = stmt.executeQuery(countSql)) {
                    if (rs2.next() && rs2.getInt("count") > 0) {
                        distribution.put("Music", rs2.getInt("count"));
                    }
                }
            }
            
            System.out.println("✅ Loaded " + distribution.size() + " genres");
        } catch (SQLException e) {
            System.err.println("❌ Error getting genre distribution: " + e.getMessage());
            e.printStackTrace();
        }
        return distribution;
    }
    
    /**
     * Get plays per day
     */
    public Map<String, Integer> getPlaysPerDay(int days) {
        String sql = """
            SELECT DATE(played_at) as day, COUNT(*) as count 
            FROM play_history 
            WHERE played_at >= DATE('now', '-' || ? || ' days')
            GROUP BY DATE(played_at) 
            ORDER BY day
        """;
        
        Map<String, Integer> playsPerDay = new LinkedHashMap<>();
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, days);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                playsPerDay.put(rs.getString("day"), rs.getInt("count"));
            }
            
            System.out.println("✅ Loaded " + playsPerDay.size() + " days of play data");
        } catch (SQLException e) {
            System.err.println("❌ Error getting plays per day: " + e.getMessage());
            e.printStackTrace();
        }
        return playsPerDay;
    }
    
    /**
     * Get total play count
     */
    public int getTotalPlayCount() {
        String sql = "SELECT COUNT(*) as total FROM play_history";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                int count = rs.getInt("total");
                System.out.println("✅ Total plays: " + count);
                return count;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting total play count: " + e.getMessage());
        }
        return 0;
    }
    
    /**
     * Get total listening time
     */
    public int getTotalListeningTime() {
        String sql = "SELECT SUM(duration) as total FROM play_history";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                int total = rs.getInt("total");
                System.out.println("✅ Total listening time: " + total + " seconds");
                return total;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting total listening time: " + e.getMessage());
        }
        return 0;
    }
    
    /**
     * Get most active day of week
     */
    public String getMostActiveDay() {
        String sql = """
            SELECT 
                CASE CAST(strftime('%w', played_at) AS INTEGER)
                    WHEN 0 THEN 'Sunday'
                    WHEN 1 THEN 'Monday'
                    WHEN 2 THEN 'Tuesday'
                    WHEN 3 THEN 'Wednesday'
                    WHEN 4 THEN 'Thursday'
                    WHEN 5 THEN 'Friday'
                    WHEN 6 THEN 'Saturday'
                END as day_name,
                COUNT(*) as count 
            FROM play_history 
            GROUP BY strftime('%w', played_at) 
            ORDER BY count DESC 
            LIMIT 1
        """;
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                String day = rs.getString("day_name");
                System.out.println("✅ Most active day: " + day);
                return day;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting most active day: " + e.getMessage());
        }
        return "N/A";
    }
    
    /**
     * Get top artists
     */
    public List<Map<String, Object>> getTopArtists(int limit) {
        String sql = """
            SELECT artist, COUNT(*) as play_count 
            FROM play_history 
            WHERE artist IS NOT NULL AND artist != ''
            GROUP BY artist 
            ORDER BY play_count DESC 
            LIMIT ?
        """;
        
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Map<String, Object> artist = new HashMap<>();
                artist.put("name", rs.getString("artist"));
                artist.put("count", rs.getInt("play_count"));
                results.add(artist);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting top artists: " + e.getMessage());
        }
        return results;
    }
    
    /**
     * Clear all analytics data
     */
    public void clearAllData() {
        String sql = "DELETE FROM play_history";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println("🗑️ All analytics data cleared");
        } catch (SQLException e) {
            System.err.println("❌ Error clearing data: " + e.getMessage());
        }
    }
    
    /**
     * Debug: Print sample data
     */
    public void debugPrintData() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🔍 DEBUG: Analytics SQLite Data");
        System.out.println("=".repeat(60));
        
        try {
            // Total count
            String sql1 = "SELECT COUNT(*) as count FROM play_history";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql1)) {
                if (rs.next()) {
                    System.out.println("📊 Total records: " + rs.getInt("count"));
                }
            }
            
            // Recent plays
            String sql2 = "SELECT track_title, artist, played_at FROM play_history ORDER BY played_at DESC LIMIT 5";
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(sql2)) {
                System.out.println("\n🎵 Recent plays:");
                while (rs.next()) {
                    System.out.println("   - " + rs.getString("artist") + " - " + rs.getString("track_title") + " @ " + rs.getString("played_at"));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Debug error: " + e.getMessage());
        }
        
        System.out.println("=".repeat(60) + "\n");
    }
}