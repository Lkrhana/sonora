package com.musicplayer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.musicplayer.model.Track;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * YouTube Music Service dengan debug logging
 */
public class YouTubeMusicService {
    private static final String INNERTUBE_API_URL = "https://music.youtube.com/youtubei/v1/";
    private static final String API_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30";
    private static final String CLIENT_NAME = "WEB_REMIX";
    private static final String CLIENT_VERSION = "1.20231115.01.00";

    private final OkHttpClient client;
    private final ObjectMapper mapper;
    private final YtDlpService ytDlpService;

    public YouTubeMusicService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
        this.mapper = new ObjectMapper();
        this.ytDlpService = new YtDlpService();

        System.out.println("✅ YouTubeMusicService initialized");
    }

    /**
     * Search lagu di YouTube Music
     */
    public List<Track> searchTracks(String query) throws IOException {
        System.out.println("🔍 YouTubeMusicService.searchTracks: " + query);

        List<Track> tracks = new ArrayList<>();

        ObjectNode context = createContext();
        ObjectNode requestBody = mapper.createObjectNode();
        requestBody.set("context", context);
        requestBody.put("query", query);
        requestBody.put("params", "EgWKAQIIAWoMEAMQBBAJEAoQBRAV");

        String url = INNERTUBE_API_URL + "search?key=" + API_KEY;

        System.out.println("📡 Sending request to: " + url);

        RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .addHeader("Accept-Language", "en-US,en;q=0.9")
                .addHeader("Origin", "https://music.youtube.com")
                .build();

        try (Response response = client.newCall(request).execute()) {
            System.out.println("📥 Response code: " + response.code());

            if (response.isSuccessful() && response.body() != null) {
                String jsonResponse = response.body().string();
                System.out.println("📄 Response length: " + jsonResponse.length() + " chars");

                // Save response to file for debugging (optional)
                // Files.writeString(Paths.get("search_response.json"), jsonResponse);

                tracks = parseSearchResponse(jsonResponse);
                System.out.println("✅ Parsed " + tracks.size() + " tracks");

            } else {
                System.err.println("❌ Search failed: " + response.code());
                if (response.body() != null) {
                    System.err.println("   Response: " + response.body().string());
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Exception during search: " + e.getMessage());
            e.printStackTrace();
        }

        return tracks;
    }

    public String getStreamUrl(String videoId) {
        if (videoId == null || videoId.isEmpty()) {
            System.err.println("❌ Invalid videoId");
            return null;
        }
        return ytDlpService.getStreamUrl(videoId);
    }

    public JsonNode getTrackInfo(String videoId) {
        if (videoId == null || videoId.isEmpty()) {
            System.err.println("❌ Invalid videoId");
            return null;
        }
        return ytDlpService.getTrackInfo(videoId);
    }

    private ObjectNode createContext() {
        ObjectNode context = mapper.createObjectNode();
        ObjectNode clientNode = mapper.createObjectNode();

        clientNode.put("clientName", CLIENT_NAME);
        clientNode.put("clientVersion", CLIENT_VERSION);
        clientNode.put("hl", "en");
        clientNode.put("gl", "US");

        context.set("client", clientNode);
        return context;
    }

    /**
     * Parse search response dengan logging
     */
    private List<Track> parseSearchResponse(String jsonResponse) throws IOException {
        List<Track> tracks = new ArrayList<>();

        try {
            JsonNode root = mapper.readTree(jsonResponse);
            System.out.println("📊 Parsing JSON response...");

            JsonNode contents = root.path("contents")
                    .path("tabbedSearchResultsRenderer")
                    .path("tabs");

            if (contents.isMissingNode()) {
                System.err.println("❌ Missing tabbedSearchResultsRenderer in response");
                return tracks;
            }

            if (!contents.isArray() || contents.size() == 0) {
                System.err.println("❌ No tabs in response");
                return tracks;
            }

            System.out.println("📋 Found " + contents.size() + " tabs");

            JsonNode sectionList = contents.get(0)
                    .path("tabRenderer")
                    .path("content")
                    .path("sectionListRenderer")
                    .path("contents");

            if (sectionList.isMissingNode()) {
                System.err.println("❌ Missing sectionListRenderer");
                return tracks;
            }

            System.out.println("📑 Found " + sectionList.size() + " sections");

            for (JsonNode section : sectionList) {
                JsonNode musicShelf = section.path("musicShelfRenderer");
                if (musicShelf.isMissingNode()) {
                    System.out.println("⏭️ Skipping non-music section");
                    continue;
                }

                JsonNode contentsList = musicShelf.path("contents");
                System.out.println("🎵 Music shelf has " + contentsList.size() + " items");

                for (JsonNode item : contentsList) {
                    Track track = parseMusicItem(item);
                    if (track != null) {
                        tracks.add(track);
                        System.out.println("   ✅ " + track.getArtist() + " - " + track.getTitle());
                    } else {
                        System.out.println("   ⏭️ Skipped item (not a valid track)");
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error parsing response: " + e.getMessage());
            e.printStackTrace();
        }

        return tracks;
    }

    /**
     * Parse individual music item
     */
    private Track parseMusicItem(JsonNode item) {
        try {
            JsonNode renderer = item.path("musicResponsiveListItemRenderer");
            if (renderer.isMissingNode()) {
                return null;
            }

            Track track = new Track();

            // Get video ID
            JsonNode playlistItemData = renderer.path("playlistItemData");
            if (!playlistItemData.isMissingNode()) {
                String videoId = playlistItemData.path("videoId").asText("");
                if (!videoId.isEmpty()) {
                    track.setYoutubeId(videoId);
                    track.setId(videoId);
                } else {
                    System.out.println("   ⚠️ Item has no videoId");
                    return null;
                }
            } else {
                System.out.println("   ⚠️ Item has no playlistItemData");
                return null;
            }

            // Get title and artist
            JsonNode flexColumns = renderer.path("flexColumns");
            if (flexColumns.isArray() && flexColumns.size() > 0) {
                // Title
                JsonNode titleRuns = flexColumns.get(0)
                        .path("musicResponsiveListItemFlexColumnRenderer")
                        .path("text")
                        .path("runs");
                if (titleRuns.isArray() && titleRuns.size() > 0) {
                    track.setTitle(titleRuns.get(0).path("text").asText("Unknown"));
                }

                // Artist and album
                if (flexColumns.size() > 1) {
                    JsonNode detailRuns = flexColumns.get(1)
                            .path("musicResponsiveListItemFlexColumnRenderer")
                            .path("text")
                            .path("runs");
                    if (detailRuns.isArray() && detailRuns.size() > 0) {
                        track.setArtist(detailRuns.get(0).path("text").asText("Unknown"));
                        if (detailRuns.size() > 2) {
                            track.setAlbum(detailRuns.get(2).path("text").asText(""));
                        }
                    }
                }
            }

            // Get thumbnail
            JsonNode thumbnail = renderer.path("thumbnail")
                    .path("musicThumbnailRenderer")
                    .path("thumbnail")
                    .path("thumbnails");
            if (thumbnail.isArray() && thumbnail.size() > 0) {
                track.setThumbnailUrl(thumbnail.get(0).path("url").asText(""));
            }

            return track;

        } catch (Exception e) {
            System.err.println("❌ Error parsing music item: " + e.getMessage());
            return null;
        }
    }
}