package com.musicplayer.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility untuk format waktu dan durasi
 */
public class TimeFormatter {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Format detik ke format MM:SS atau HH:MM:SS
     */
    public static String formatDuration(int seconds) {
        if (seconds < 0) {
            return "00:00";
        }

        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%d:%02d", minutes, secs);
        }
    }

    /**
     * Format Duration object
     */
    public static String formatDuration(Duration duration) {
        return formatDuration((int) duration.getSeconds());
    }

    /**
     * Format LocalDateTime ke string readable
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DATE_TIME_FORMATTER);
    }

    /**
     * Format time only
     */
    public static String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(TIME_FORMATTER);
    }

    /**
     * Get relative time (e.g., "2 hours ago", "3 days ago")
     */
    public static String getRelativeTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }

        Duration duration = Duration.between(dateTime, LocalDateTime.now());
        long seconds = duration.getSeconds();

        if (seconds < 60) {
            return "Just now";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        } else if (seconds < 86400) {
            long hours = seconds / 3600;
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (seconds < 604800) {
            long days = seconds / 86400;
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else if (seconds < 2592000) {
            long weeks = seconds / 604800;
            return weeks + " week" + (weeks > 1 ? "s" : "") + " ago";
        } else if (seconds < 31536000) {
            long months = seconds / 2592000;
            return months + " month" + (months > 1 ? "s" : "") + " ago";
        } else {
            long years = seconds / 31536000;
            return years + " year" + (years > 1 ? "s" : "") + " ago";
        }
    }
}
