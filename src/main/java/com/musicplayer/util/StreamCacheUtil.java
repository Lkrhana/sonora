package com.musicplayer.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.*;
import java.nio.file.*;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;

/**
 * Utilitas untuk cache stream audio ke file lokal sementara
 * dengan batasan total cache size dan pembersihan otomatis
 */
public class StreamCacheUtil {

    private static final String CACHE_DIR = System.getProperty("user.home") + File.separator + ".smartmusicplayer" + File.separator + "cache";
    private static final long MAX_CACHE_SIZE_BYTES = 100 * 1024 * 1024; // 100 MB

    private static final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    static {
        // Buat folder cache jika belum ada
        File dir = new File(CACHE_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    /**
     * Download stream URL ke file cache lokal, kembalikan file lokal yang sudah disimpan
     * Jika file sudah ada, kembalikan langsung (cache hit)
     */
    public static File cacheStreamToFile(String streamUrl) throws IOException {
        String fileName = generateSafeFileName(streamUrl);
        File cachedFile = new File(CACHE_DIR, fileName);

        if (cachedFile.exists()) {
            // Update last modified waktu agar tidak dihapus terlalu cepat
            cachedFile.setLastModified(System.currentTimeMillis());
            return cachedFile;
        }

        // Download stream ke file sementara
        Request request = new Request.Builder()
                .url(streamUrl)
                .build();

        System.out.println("⬇️  Downloading stream to cache: " + streamUrl);

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("Failed to download stream: " + response);
            }

            ResponseBody body = response.body();

            try (InputStream inputStream = body.byteStream();
                 FileOutputStream outputStream = new FileOutputStream(cachedFile)) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
        }

        // Bersihkan cache jika perlu
        cleanUpCache();

        return cachedFile;
    }

    /**
     * Menghapus file cache tertua jika ukuran cache melebihi batas maksimum
     */
    private static void cleanUpCache() {
        File cacheDir = new File(CACHE_DIR);
        File[] files = cacheDir.listFiles();

        if (files == null) return;

        long totalSize = 0;
        PriorityQueue<File> filesByModified = new PriorityQueue<>(Comparator.comparingLong(File::lastModified));

        for (File file : files) {
            totalSize += file.length();
            filesByModified.offer(file);
        }

        while (totalSize > MAX_CACHE_SIZE_BYTES && !filesByModified.isEmpty()) {
            File oldestFile = filesByModified.poll();
            long length = oldestFile.length();
            if (oldestFile.delete()) {
                totalSize -= length;
                System.out.println("🗑️ Deleted cache file: " + oldestFile.getName());
            }
        }
    }

    /**
     * Generate nama file yang aman dari URL stream (hash)
     */
    private static String generateSafeFileName(String url) {
        return Integer.toHexString(url.hashCode()) + ".cache";
    }
}
