package com.musicplayer.repository;

import java.sql.Date;
import java.sql.*;
import java.util.*;

public class AnalyticsDAO {
    
    private DatabaseManager dbManager;
    private Connection connection;
    
    public AnalyticsDAO() {
        this.dbManager = DatabaseManager.getInstance();
        // Ambil connection dari DatabaseManager menggunakan reflection atau direct access
        try {
            // Get connection field using reflection karena private
            java.lang.reflect.Field connField = DatabaseManager.class.getDeclaredField("connection");
            connField.setAccessible(true);
            this.connection = (Connection) connField.get(dbManager);
        } catch (Exception e) {
            System.err.println("❌ Error accessing connection: " + e.getMessage());
        }
    }
    
    /**
     * Get top tracks berdasarkan PLAY_HISTORY
     */
    public List<Map<String, Object>> getTopTracks(int limit) {
        String sql = """
            SELECT t.title, t.artist, COUNT(ph.id) as play_count 
            FROM play_history ph
            JOIN tracks t ON ph.track_id = t.id
            GROUP BY t.id, t.title, t.artist
            ORDER BY play_count DESC 
            LIMIT ?
        """;
        
        List<Map<String, Object>> results = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> track = new HashMap<>();
                    track.put("title", rs.getString("title"));
                    track.put("artist", rs.getString("artist"));
                    track.put("count", rs.getInt("play_count"));
                    results.add(track);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting top tracks: " + e.getMessage());
            e.printStackTrace();
        }
        return results;
    }
    
    /**
     * Get genre distribution dari TRACKS yang pernah diputar
     */
    public Map<String, Integer> getGenreDistribution() {
        String sql = """
            SELECT t.genre, COUNT(ph.id) as count 
            FROM play_history ph
            JOIN tracks t ON ph.track_id = t.id
            WHERE t.genre IS NOT NULL AND t.genre <> ''
            GROUP BY t.genre
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
        } catch (SQLException e) {
            System.err.println("❌ Error getting genre distribution: " + e.getMessage());
            e.printStackTrace();
        }
        return distribution;
    }
    
    /**
     * Get plays per day untuk N hari terakhir
     */
    public Map<String, Integer> getPlaysPerDay(int days) {
        String sql = """
            SELECT FORMATDATETIME(played_at, 'yyyy-MM-dd') as day, COUNT(*) as count 
            FROM play_history 
            WHERE played_at >= DATEADD(DAY, ?, CURRENT_DATE)
            GROUP BY FORMATDATETIME(played_at, 'yyyy-MM-dd')
            ORDER BY day
        """;
        
        Map<String, Integer> playsPerDay = new LinkedHashMap<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            
            pstmt.setInt(1, -days);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    playsPerDay.put(rs.getString("day"), rs.getInt("count"));
                }
            }
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
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting total play count: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }
    
    /**
     * Get total listening time (estimasi dari durasi track)
     */
    public int getTotalListeningTime() {
        String sql = """
            SELECT COALESCE(SUM(t.duration), 0) as total 
            FROM play_history ph
            JOIN tracks t ON ph.track_id = t.id
        """;
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting total listening time: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }
    
    /**
     * Get most active day of week
     */
    public String getMostActiveDay() {
        String sql = """
            SELECT FORMATDATETIME(played_at, 'EEEE') as day_name, COUNT(*) as count 
            FROM play_history 
            GROUP BY FORMATDATETIME(played_at, 'EEEE'), DAYOFWEEK(played_at)
            ORDER BY count DESC 
            LIMIT 1
        """;
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getString("day_name");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting most active day: " + e.getMessage());
            e.printStackTrace();
        }
        return "N/A";
    }
    
    /**
     * Get listening stats per artist
     */
    public List<Map<String, Object>> getTopArtists(int limit) {
        String sql = """
            SELECT t.artist, COUNT(ph.id) as play_count
            FROM play_history ph
            JOIN tracks t ON ph.track_id = t.id
            WHERE t.artist IS NOT NULL AND t.artist <> ''
            GROUP BY t.artist
            ORDER BY play_count DESC
            LIMIT ?
        """;
        
        List<Map<String, Object>> results = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> artist = new HashMap<>();
                    artist.put("name", rs.getString("artist"));
                    artist.put("count", rs.getInt("play_count"));
                    results.add(artist);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting top artists: " + e.getMessage());
            e.printStackTrace();
        }
        return results;
    }
    
    /**
     * Get most active hour of the day
     */
    public Map<Integer, Integer> getPlaysByHour() {
        String sql = """
            SELECT HOUR(played_at) as hour, COUNT(*) as count 
            FROM play_history 
            GROUP BY HOUR(played_at)
            ORDER BY hour
        """;
        
        Map<Integer, Integer> playsByHour = new LinkedHashMap<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                playsByHour.put(rs.getInt("hour"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting plays by hour: " + e.getMessage());
            e.printStackTrace();
        }
        return playsByHour;
    }
    
    /**
     * Get average BPM of played tracks
     */
    public int getAverageBPM() {
        String sql = """
            SELECT AVG(t.bpm) as avg_bpm 
            FROM play_history ph
            JOIN tracks t ON ph.track_id = t.id
            WHERE t.bpm > 0
        """;
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt("avg_bpm");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting average BPM: " + e.getMessage());
        }
        return 0;
    }
    
    /**
     * Get mood distribution
     */
    public Map<String, Integer> getMoodDistribution() {
        String sql = """
            SELECT t.mood, COUNT(ph.id) as count 
            FROM play_history ph
            JOIN tracks t ON ph.track_id = t.id
            WHERE t.mood IS NOT NULL AND t.mood <> ''
            GROUP BY t.mood
            ORDER BY count DESC
        """;
        
        Map<String, Integer> moodDist = new LinkedHashMap<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                String mood = rs.getString("mood");
                if (mood != null && !mood.trim().isEmpty()) {
                    moodDist.put(mood, rs.getInt("count"));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting mood distribution: " + e.getMessage());
        }
        return moodDist;
    }
    
    /**
     * Get liked tracks count
     */
    public int getLikedTracksCount() {
        String sql = "SELECT COUNT(*) as count FROM play_history WHERE liked = TRUE";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting liked tracks count: " + e.getMessage());
        }
        return 0;
    }
    
    /**
     * Get total unique tracks played
     */
    public int getUniqueTracksPlayed() {
        String sql = "SELECT COUNT(DISTINCT track_id) as count FROM play_history";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt("count");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error getting unique tracks played: " + e.getMessage());
        }
        return 0;
    }
    
    /**
     * Get listening streak (consecutive days with plays)
     */
    public int getCurrentStreak() {
        String sql = """
            SELECT DISTINCT CAST(played_at AS DATE) as play_date
            FROM play_history
            ORDER BY play_date DESC
        """;
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            int streak = 0;
            Date previousDate = null;
            
            while (rs.next()) {
                Date currentDate = rs.getDate("play_date");
                
                if (previousDate == null) {
                    // First date
                    streak = 1;
                    previousDate = currentDate;
                } else {
                    // Check if consecutive
                    long diffInDays = (previousDate.getTime() - currentDate.getTime()) / (1000 * 60 * 60 * 24);
                    
                    if (diffInDays == 1) {
                        streak++;
                        previousDate = currentDate;
                    } else {
                        break; // Streak broken
                    }
                }
            }
            
            return streak;
        } catch (SQLException e) {
            System.err.println("❌ Error getting current streak: " + e.getMessage());
        }
        return 0;
    }
}