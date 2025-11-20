package com.musicplayer;

import com.formdev.flatlaf.FlatDarkLaf;
import com.musicplayer.view.MainFrame;
import javax.swing.*;

/**
 * Main entry point untuk Smart Music Player
 * Aplikasi pemutar musik dengan Audio Fingerprinting dan Rekomendasi Cerdas
 */
public class MusicPlayerApp {
    public static void main(String[] args) {
        // Set FlatLaf dark theme untuk tampilan modern
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (Exception e) {
            System.err.println("Failed to initialize FlatLaf theme");
        }

        // Launch UI di Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}