package com.musicplayer.repository;

import java.sql.*;

public class AnalyticsDatabaseManager {
    private static AnalyticsDatabaseManager instance;
    private Connection connection;
    private static final String DB_PATH = "analytics.db";

    private AnalyticsDatabaseManager() {
        initializeDatabase();
    }

    public static synchronized AnalyticsDatabaseManager getInstance() {
        if (instance == null) {
            instance = new AnalyticsDatabaseManager();
        }
        return instance;
    }

    private void initializeDatabase() {
        try {
            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            
            // Create connection
            connection = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
            
            System.out.println("✅ Analytics SQLite database connected: " + DB_PATH);
            
            // Create tables
            createTables();
            
        } catch (ClassNotFoundException e) {
            System.err.println("❌ SQLite JDBC driver not found!");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("❌ Failed to initialize analytics database");
            e.printStackTrace();
        }
    }

    private void createTables() {
        String createPlayHistoryTable = """
            CREATE TABLE IF NOT EXISTS play_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                track_id TEXT NOT NULL,
                track_title TEXT NOT NULL,
                artist TEXT,
                genre TEXT,
                duration INTEGER DEFAULT 0,
                played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createPlayHistoryTable);
            System.out.println("✅ Analytics table 'play_history' ready");
        } catch (SQLException e) {
            System.err.println("❌ Error creating analytics tables");
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("✅ Analytics database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error closing analytics database");
            e.printStackTrace();
        }
    }
}