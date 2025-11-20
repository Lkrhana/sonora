package com.musicplayer.service;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class LyricsService {
    
    private static final String API_BASE_URL = "https://api.lyrics.ovh/v1";
    
    /**
     * Fetch lyrics untuk lagu berdasarkan artist dan title
     * @param artist Nama artist
     * @param title Judul lagu
     * @return Lyrics sebagai String, atau null jika tidak ditemukan
     */
    public String fetchLyrics(String artist, String title) {
        if (artist == null || title == null || artist.isEmpty() || title.isEmpty()) {
            System.err.println("❌ Artist or title is empty");
            return null;
        }
        
        try {
            // Clean up artist and title
            String cleanArtist = cleanString(artist);
            String cleanTitle = cleanString(title);
            
            // Encode untuk URL
            String encodedArtist = URLEncoder.encode(cleanArtist, StandardCharsets.UTF_8);
            String encodedTitle = URLEncoder.encode(cleanTitle, StandardCharsets.UTF_8);
            
            // Build URL
            String urlString = String.format("%s/%s/%s", API_BASE_URL, encodedArtist, encodedTitle);
            
            System.out.println("🔍 Fetching lyrics from: " + urlString);
            
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            
            int responseCode = conn.getResponseCode();
            System.out.println("📡 Response code: " + responseCode);
            
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                
                // Parse JSON response
                JSONObject jsonResponse = new JSONObject(response.toString());
                
                if (jsonResponse.has("lyrics")) {
                    String lyrics = jsonResponse.getString("lyrics");
                    System.out.println("✅ Lyrics fetched successfully!");
                    return lyrics;
                } else {
                    System.err.println("❌ No 'lyrics' field in response");
                    return null;
                }
                
            } else if (responseCode == 404) {
                System.out.println("⚠️ Lyrics not found for: " + cleanArtist + " - " + cleanTitle);
                return "Lyrics not found for this song.\n\nTry searching online or check the song details.";
            } else {
                System.err.println("❌ HTTP Error: " + responseCode);
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error fetching lyrics: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Clean string untuk query API (remove featuring, extra info, etc)
     */
    private String cleanString(String input) {
        if (input == null) return "";
        
        // Remove common patterns
        String cleaned = input
            .replaceAll("(?i)\\s*\\(.*?\\)", "")  // Remove parentheses content
            .replaceAll("(?i)\\s*\\[.*?\\]", "")  // Remove brackets content
            .replaceAll("(?i)\\s*ft\\.?.*", "")   // Remove featuring
            .replaceAll("(?i)\\s*feat\\.?.*", "") // Remove featuring
            .replaceAll("(?i)\\s*featuring.*", "") // Remove featuring
            .trim();
        
        return cleaned;
    }
    
    /**
     * Test method untuk debugging
     */
    public void testFetch(String artist, String title) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🧪 Testing Lyrics Fetch");
        System.out.println("Artist: " + artist);
        System.out.println("Title: " + title);
        System.out.println("=".repeat(60));
        
        String lyrics = fetchLyrics(artist, title);
        
        if (lyrics != null) {
            System.out.println("\n📝 Lyrics Preview:");
            System.out.println(lyrics.substring(0, Math.min(200, lyrics.length())) + "...");
        } else {
            System.out.println("\n❌ Failed to fetch lyrics");
        }
        
        System.out.println("=".repeat(60) + "\n");
    }
}