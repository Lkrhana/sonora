package com.musicplayer.repository;

import com.musicplayer.model.Track;
import com.musicplayer.model.PlayHistory;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


// Manager database lokal H2, simpen tracks, play history, preferences
public class DatabaseManager {
    private static final String DB_URL = "jdbc:h2:./data/musicplayer;AUTO_SERVER=TRUE";

    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        try {
            connection = DriverManager.getConnection(DB_URL, "as", "");
            initializeTables();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }


    private void initializeTables() throws SQLException {
        String createTracksTable = """
            CREATE TABLE IF NOT EXISTS tracks (
                id VARCHAR(255) PRIMARY KEY,
                title VARCHAR(500),
                artist VARCHAR(500),
                album VARCHAR(500),
                genre VARCHAR(100),
                duration INT,
                bpm INT,
                mood VARCHAR(100),
                youtube_id VARCHAR(255),
                thumbnail_url VARCHAR(1000),
                added_date TIMESTAMP
            )
        """;

        String createPlayHistoryTable = """
            CREATE TABLE IF NOT EXISTS play_history (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                track_id VARCHAR(255),
                played_at TIMESTAMP,
                play_count INT DEFAULT 1,
                liked BOOLEAN DEFAULT FALSE,
                FOREIGN KEY (track_id) REFERENCES tracks(id) ON DELETE CASCADE
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTracksTable);
            stmt.execute(createPlayHistoryTable);
        }
    }

    public void saveTrack(Track track) throws SQLException {
        String sql = """
            MERGE INTO tracks (id, title, artist, album, genre, duration, 
                             bpm, mood, youtube_id, thumbnail_url, added_date)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, track.getId());
            pstmt.setString(2, track.getTitle());
            pstmt.setString(3, track.getArtist());
            pstmt.setString(4, track.getAlbum());
            pstmt.setString(5, track.getGenre());
            pstmt.setInt(6, track.getDuration());
            pstmt.setInt(7, track.getBpm());
            pstmt.setString(8, track.getMood());
            pstmt.setString(9, track.getYoutubeId());
            pstmt.setString(10, track.getThumbnailUrl());
            pstmt.setTimestamp(11, Timestamp.valueOf(track.getAddedDate()));
            pstmt.executeUpdate();
        }
    }

    public Track getTrack(String trackId) throws SQLException {
        String sql = "SELECT * FROM tracks WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, trackId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapTrack(rs);
            }
        }
        return null;
    }


    public List<Track> getAllTracks() throws SQLException {
        List<Track> tracks = new ArrayList<>();
        String sql = "SELECT * FROM tracks ORDER BY added_date DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tracks.add(mapTrack(rs));
            }
        }
        return tracks;
    }

    public void recordPlay(String trackId) throws SQLException {
        String sql = """
            INSERT INTO play_history (track_id, played_at, play_count)
            VALUES (?, ?, 1)
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, trackId);
            pstmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.executeUpdate();
        }
    }


    public List<PlayHistory> getPlayHistory(int limit) throws SQLException {
        List<PlayHistory> history = new ArrayList<>();
        String sql = "SELECT * FROM play_history ORDER BY played_at DESC LIMIT ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                PlayHistory ph = new PlayHistory();
                ph.setId(rs.getLong("id"));
                ph.setTrackId(rs.getString("track_id"));
                ph.setPlayedAt(rs.getTimestamp("played_at").toLocalDateTime());
                ph.setPlayCount(rs.getInt("play_count"));
                ph.setLiked(rs.getBoolean("liked"));
                history.add(ph);
            }
        }
        return history;
    }


    public List<Track> getMostPlayedTracks(int limit) throws SQLException {
        List<Track> tracks = new ArrayList<>();
        String sql = """
            SELECT t.*, COUNT(ph.id) as play_count
            FROM tracks t
            JOIN play_history ph ON t.id = ph.track_id
            GROUP BY t.id
            ORDER BY play_count DESC
            LIMIT ?
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                tracks.add(mapTrack(rs));
            }
        }
        return tracks;
    }


    private Track mapTrack(ResultSet rs) throws SQLException {
        Track track = new Track();
        track.setId(rs.getString("id"));
        track.setTitle(rs.getString("title"));
        track.setArtist(rs.getString("artist"));
        track.setAlbum(rs.getString("album"));
        track.setGenre(rs.getString("genre"));
        track.setDuration(rs.getInt("duration"));
        track.setBpm(rs.getInt("bpm"));
        track.setMood(rs.getString("mood"));
        track.setYoutubeId(rs.getString("youtube_id"));
        track.setThumbnailUrl(rs.getString("thumbnail_url"));

        Timestamp timestamp = rs.getTimestamp("added_date");
        if (timestamp != null) {
            track.setAddedDate(timestamp.toLocalDateTime());
        }

        return track;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}