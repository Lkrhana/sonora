package com.musicplayer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class YtDlpService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final String ytDlpPath;

    public YtDlpService() {
        this.ytDlpPath = extractYtDlp();
    }

    /**
     * Extract yt-dlp.exe dari resources ke folder temp
     */
    private String extractYtDlp() {
        try {
            InputStream is = getClass().getResourceAsStream("/client/yt-dlp.exe");
            if (is == null) {
                System.err.println("❌ yt-dlp.exe tidak ditemukan di resources/client/");
                return null;
            }

            File temp = new File(System.getProperty("java.io.tmpdir"), "yt-dlp.exe");

            Files.copy(is, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
            temp.setExecutable(true);

            System.out.println("✅ yt-dlp extracted to: " + temp.getAbsolutePath());
            return temp.getAbsolutePath();

        } catch (Exception e) {
            System.err.println("❌ Error extracting yt-dlp: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get stream URL using yt-dlp
     */
    public String getStreamUrl(String videoId) {
        if (ytDlpPath == null) return null;

        try {
            String url = "https://music.youtube.com/watch?v=" + videoId;

            ProcessBuilder pb = new ProcessBuilder(
                    ytDlpPath,
                    "--format", "bestaudio",
                    "--get-url",
                    "--no-playlist",
                    url
            );

            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String streamUrl = reader.readLine();
            process.waitFor();

            if (streamUrl != null && !streamUrl.isEmpty()) {
                System.out.println("✅ Stream URL obtained via yt-dlp");
                return streamUrl;
            }

        } catch (Exception e) {
            System.err.println("yt-dlp error: " + e.getMessage());
        }

        return null;
    }

    /**
     * Get full track info via yt-dlp
     */
    public JsonNode getTrackInfo(String videoId) {
        if (ytDlpPath == null) return null;

        try {
            String url = "https://music.youtube.com/watch?v=" + videoId;

            ProcessBuilder pb = new ProcessBuilder(
                    ytDlpPath,
                    "--dump-json",
                    "--no-playlist",
                    url
            );

            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }

            process.waitFor();

            return mapper.readTree(json.toString());

        } catch (Exception e) {
            System.err.println("yt-dlp info error: " + e.getMessage());
        }

        return null;
    }
}
