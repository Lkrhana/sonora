package com.musicplayer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.musicplayer.model.Track;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

// Audio fingerprinting pake Audd
public class AudioFingerprintService {
    private static final String AUDD_API_URL = "https://api.audd.io/";
    private static final String API_TOKEN = "d26a042b439531b5792f03a15af5c9cb";

    private final OkHttpClient client;
    private final ObjectMapper mapper;

    public AudioFingerprintService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.mapper = new ObjectMapper();
    }

// Identifkasi lagu dari file audio
    public Track identifyTrack(File audioFile) throws IOException {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("api_token", API_TOKEN)
                .addFormDataPart("return", "apple_music,spotify")
                .addFormDataPart("file", audioFile.getName(),
                        RequestBody.create(audioFile, MediaType.parse("audio/*")))
                .build();

        Request request = new Request.Builder()
                .url(AUDD_API_URL)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response: " + response);
            }

            String jsonResponse = response.body().string();
            return parseAuddResponse(jsonResponse);
        }
    }

// Parse rersponse dari Audd
    private Track parseAuddResponse(String jsonResponse) throws IOException {
        JsonNode root = mapper.readTree(jsonResponse);

        if (root.has("result") && root.get("result") != null) {
            JsonNode result = root.get("result");

            Track track = new Track();
            track.setId(generateTrackId(result));
            track.setTitle(result.path("title").asText("Unknown"));
            track.setArtist(result.path("artist").asText("Unknown"));
            track.setAlbum(result.path("album").asText(""));

            // Extract additional metadata if available
            if (result.has("release_date")) {
                // Could parse release date
            }

            // Generate thumbnail URL from Spotify/Apple Music if available
            if (result.has("spotify")) {
                JsonNode spotify = result.get("spotify");
                if (spotify.has("album") && spotify.get("album").has("images")) {
                    JsonNode images = spotify.get("album").get("images");
                    if (images.isArray() && images.size() > 0) {
                        track.setThumbnailUrl(images.get(0).path("url").asText());
                    }
                }
            }

            return track;
        }

        return null; // Lagu tidak ditemukan
    }

// Generate track id
    private String generateTrackId(JsonNode result) {
        String artist = result.path("artist").asText("");
        String title = result.path("title").asText("");
        return (artist + "_" + title).replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
    }
}