package com.musicplayer.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Model untuk Playlist
 */
public class Playlist {
    private int id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Track> tracks;
    private String coverImageUrl; // Optional: URL for playlist cover

    // Constructors
    public Playlist() {
        this.tracks = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Playlist(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.tracks = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Playlist(String name, String description) {
        this.name = name;
        this.description = description;
        this.tracks = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<Track> getTracks() {
        return tracks;
    }

    public void setTracks(List<Track> tracks) {
        this.tracks = tracks;
        this.updatedAt = LocalDateTime.now();
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    // Utility methods
    public void addTrack(Track track) {
        if (!tracks.contains(track)) {
            tracks.add(track);
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void removeTrack(Track track) {
        tracks.remove(track);
        this.updatedAt = LocalDateTime.now();
    }

    public void removeTrackAt(int index) {
        if (index >= 0 && index < tracks.size()) {
            tracks.remove(index);
            this.updatedAt = LocalDateTime.now();
        }
    }

    public int getTrackCount() {
        return tracks.size();
    }

    public int getTotalDuration() {
        return tracks.stream()
                .mapToInt(Track::getDuration)
                .sum();
    }

    public boolean containsTrack(Track track) {
        return tracks.contains(track);
    }

    public void clearTracks() {
        tracks.clear();
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return name + " (" + getTrackCount() + " tracks)";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Playlist playlist = (Playlist) o;
        return id == playlist.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
