package com.musicplayer.model;



import java.time.LocalDateTime;



// Model buat track lagu

public class Track {
    private String id;
    private String title;
    private String artist;
    private String album;
    private String genre;
    private int duration; // dalam detik
    private int bpm;
    private String mood;
    private String youtubeId;
    private String thumbnailUrl;
    private LocalDateTime addedDate;

    public Track() {
        this.addedDate = LocalDateTime.now();
    }

    public Track(String id, String title, String artist) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.addedDate = LocalDateTime.now();
    }



    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }
    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public int getBpm() { return bpm; }
    public void setBpm(int bpm) { this.bpm = bpm; }
    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }


    public String getYoutubeId() { return youtubeId; }

    public void setYoutubeId(String youtubeId) { this.youtubeId = youtubeId; }



    public String getThumbnailUrl() { return thumbnailUrl; }

    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }



    public LocalDateTime getAddedDate() { return addedDate; }

    public void setAddedDate(LocalDateTime addedDate) { this.addedDate = addedDate; }



    @Override

    public String toString() {

        return artist + " - " + title;

    }

}

