package com.musicplayer.model;
import java.time.LocalDateTime;

// Model buat play lagu

public class PlayHistory {
    private Long id;
    private String trackId;
    private LocalDateTime playedAt;
    private int playCount;
    private boolean liked;



    public PlayHistory() {
        this.playedAt = LocalDateTime.now();
        this.playCount = 1;
    }



    public PlayHistory(String trackId) {
        this.trackId = trackId;
        this.playedAt = LocalDateTime.now();
        this.playCount = 1;
    }



    // Getters and Setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTrackId() { return trackId; }
    public void setTrackId(String trackId) { this.trackId = trackId; }
    public LocalDateTime getPlayedAt() { return playedAt; }
    public void setPlayedAt(LocalDateTime playedAt) { this.playedAt = playedAt; }
    public int getPlayCount() { return playCount; }
    public void setPlayCount(int playCount) { this.playCount = playCount; }
    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }

}