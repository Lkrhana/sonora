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
        
        String createPlaylistsTable = """
            CREATE TABLE IF NOT EXISTS playlists (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(500) NOT NULL,
                description VARCHAR(2000),
                cover_image_url VARCHAR(1000),
                created_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP NOT NULL
            )
        """;

        String createPlaylistTracksTable = """
            CREATE TABLE IF NOT EXISTS playlist_tracks (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                playlist_id BIGINT NOT NULL,
                track_id VARCHAR(255) NOT NULL,
                position INT NOT NULL,
                added_at TIMESTAMP NOT NULL,
                FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,
                FOREIGN KEY (track_id) REFERENCES tracks(id) ON DELETE CASCADE,
                UNIQUE(playlist_id, track_id)
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTracksTable);
            stmt.execute(createPlayHistoryTable);
            stmt.execute(createPlaylistsTable);
            stmt.execute(createPlaylistTracksTable);
        }
    }

    public void saveTrack(Track track) throws SQLException {
        
        if (track.getId() == null || track.getId().isEmpty()) {
            track.setId(generateTrackId(track));
        }
        
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
            System.out.println("✅ Track saved with ID: " + track.getId());
        }
    }
    
    private String generateTrackId(Track track) {
        if (track.getYoutubeId() != null && !track.getYoutubeId().isEmpty()) {
            return "yt_" + track.getYoutubeId();
        } else {
            // Fallback: hash of title + artist
            String combined = (track.getTitle() != null ? track.getTitle() : "") + "_" + (track.getArtist() != null ? track.getArtist() : "");
            return "track_" + Math.abs(combined.hashCode());
        }
    }
    
    private String ensureTrackSaved(Track track) throws SQLException {
        // Generate ID if not exists
        if (track.getId() == null || track.getId().isEmpty()) {
            String trackId = generateTrackId(track);
            track.setId(trackId);
        }

        // Save track (existing method)
        saveTrack(track);

        return track.getId();
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
    
    public long createPlaylist(com.musicplayer.model.Playlist playlist) throws SQLException {
        String sql = """
            INSERT INTO playlists (name, description, cover_image_url, created_at, updated_at) 
            VALUES (?, ?, ?, ?, ?)
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, playlist.getName());
            pstmt.setString(2, playlist.getDescription());
            pstmt.setString(3, playlist.getCoverImageUrl());
            pstmt.setTimestamp(4, Timestamp.valueOf(playlist.getCreatedAt()));
            pstmt.setTimestamp(5, Timestamp.valueOf(playlist.getUpdatedAt()));

            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long playlistId = generatedKeys.getLong(1);
                    playlist.setId((int) playlistId);
                    System.out.println("✅ Playlist created: " + playlist.getName() + " (ID: " + playlistId + ")");
                    return playlistId;
                }
            }
        }

        return -1;
    }
    
    public List<com.musicplayer.model.Playlist> getAllPlaylists() throws SQLException {
        List<com.musicplayer.model.Playlist> playlists = new ArrayList<>();
        String sql = "SELECT * FROM playlists ORDER BY updated_at DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                com.musicplayer.model.Playlist playlist = mapPlaylist(rs);
                playlists.add(playlist);
            }
        }

        return playlists;
    }
    
    public com.musicplayer.model.Playlist getPlaylist(long playlistId) throws SQLException {
        String sql = "SELECT * FROM playlists WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, playlistId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapPlaylist(rs);
                }
            }
        }

        return null;
    }
    
    public void updatePlaylist(com.musicplayer.model.Playlist playlist) throws SQLException {
        String sql = """
            UPDATE playlists 
            SET name = ?, description = ?, cover_image_url = ?, updated_at = ? 
            WHERE id = ?
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playlist.getName());
            pstmt.setString(2, playlist.getDescription());
            pstmt.setString(3, playlist.getCoverImageUrl());
            pstmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setInt(5, playlist.getId());

            pstmt.executeUpdate();
            System.out.println("✅ Playlist updated: " + playlist.getName());
        }
    }
    
    public void deletePlaylist(long playlistId) throws SQLException {
        String sql = "DELETE FROM playlists WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, playlistId);
            pstmt.executeUpdate();
            System.out.println("🗑️ Playlist deleted (ID: " + playlistId + ")");
        }
    }
    
    public void addTrackToPlaylist(long playlistId, Track track) throws SQLException {
        // Ensure track has ID and is saved
        String trackId = ensureTrackSaved(track); // ← Use helper method

        // Get next position
        int position = getPlaylistTrackCount(playlistId) + 1;

        String sql = """
            INSERT INTO playlist_tracks (playlist_id, track_id, position, added_at) 
            VALUES (?, ?, ?, ?)
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, playlistId);
            pstmt.setString(2, trackId);
            pstmt.setInt(3, position);
            pstmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));

            pstmt.executeUpdate();

            // Update playlist timestamp
            updatePlaylistTimestamp(playlistId);

            System.out.println("➕ Track added to playlist: " + track.getTitle() + " (ID: " + trackId + ")");

        } catch (SQLException e) {
            if (e.getMessage().contains("Unique index or primary key violation")) {
                System.out.println("⚠️ Track already in playlist");
            } else {
                throw e;
            }
        }
    }
    
    public void removeTrackFromPlaylist(long playlistId, String trackId) throws SQLException {
        String sql = "DELETE FROM playlist_tracks WHERE playlist_id = ? AND track_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, playlistId);
            pstmt.setString(2, trackId);

            pstmt.executeUpdate();

            // Reorder remaining tracks
            reorderPlaylistTracks(playlistId);

            // Update playlist timestamp
            updatePlaylistTimestamp(playlistId);

            System.out.println("🗑️ Track removed from playlist");
        }
    }
    
    public List<Track> getPlaylistTracks(long playlistId) throws SQLException {
        List<Track> tracks = new ArrayList<>();

        String sql = """
            SELECT t.*, pt.position 
            FROM tracks t
            JOIN playlist_tracks pt ON t.id = pt.track_id
            WHERE pt.playlist_id = ?
            ORDER BY pt.position ASC
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, playlistId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tracks.add(mapTrack(rs));
                }
            }
        }

        return tracks;
    }
    
    private com.musicplayer.model.Playlist mapPlaylist(ResultSet rs) throws SQLException {
        com.musicplayer.model.Playlist playlist = new com.musicplayer.model.Playlist();
        playlist.setId(rs.getInt("id"));
        playlist.setName(rs.getString("name"));
        playlist.setDescription(rs.getString("description"));
        playlist.setCoverImageUrl(rs.getString("cover_image_url"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            playlist.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            playlist.setUpdatedAt(updatedAt.toLocalDateTime());
        }

        // Load tracks
        try {
            playlist.setTracks(getPlaylistTracks(playlist.getId()));
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return playlist;
    }
    
    private int getPlaylistTrackCount(long playlistId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM playlist_tracks WHERE playlist_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, playlistId);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }
    
    private void reorderPlaylistTracks(long playlistId) throws SQLException {
        String selectSql = """
            SELECT id FROM playlist_tracks 
            WHERE playlist_id = ? 
            ORDER BY position ASC
        """;

        try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
            selectStmt.setLong(1, playlistId);

            try (ResultSet rs = selectStmt.executeQuery()) {
                int position = 1;

                String updateSql = "UPDATE playlist_tracks SET position = ? WHERE id = ?";
                try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                    while (rs.next()) {
                        updateStmt.setInt(1, position++);
                        updateStmt.setLong(2, rs.getLong("id"));
                        updateStmt.executeUpdate();
                    }
                }
            }
        }
    }
    
    private void updatePlaylistTimestamp(long playlistId) throws SQLException {
        String sql = "UPDATE playlists SET updated_at = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setLong(2, playlistId);
            pstmt.executeUpdate();
        }
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