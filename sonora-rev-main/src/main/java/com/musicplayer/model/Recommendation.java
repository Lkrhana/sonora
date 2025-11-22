package com.musicplayer.model;

// Model buat rekomendasi lagu
public class Recommendation {
    private Track track;
    private double score;
    private String reason; // "Similar to X", "Based on your taste", etc.

    public Recommendation(Track track, double score, String reason) {
        this.track = track;
        this.score = score;
        this.reason = reason;
    }

    public Track getTrack() { return track; }
    public void setTrack(Track track) { this.track = track; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}