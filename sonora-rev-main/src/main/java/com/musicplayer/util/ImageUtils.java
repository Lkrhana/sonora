package com.musicplayer.util;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

/**
 * Utility untuk image operations
 */
public class ImageUtils {

    /**
     * Load image dari URL
     */
    public static ImageIcon loadImageFromUrl(String urlString, int width, int height) {
        try {
            URL url = new URL(urlString);
            BufferedImage image = ImageIO.read(url);
            if (image != null) {
                Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                return new ImageIcon(scaledImage);
            }
        } catch (IOException e) {
            System.err.println("Failed to load image from URL: " + urlString);
        }
        return getPlaceholderIcon(width, height);
    }

    /**
     * Load image dari resources
     */
    public static ImageIcon loadImageFromResources(String path, int width, int height) {
        try {
            URL resource = ImageUtils.class.getResource(path);
            if (resource != null) {
                BufferedImage image = ImageIO.read(resource);
                Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
                return new ImageIcon(scaledImage);
            }
        } catch (IOException e) {
            System.err.println("Failed to load image from resources: " + path);
        }
        return getPlaceholderIcon(width, height);
    }

    /**
     * Create placeholder icon
     */
    public static ImageIcon getPlaceholderIcon(int width, int height) {
        BufferedImage placeholder = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = placeholder.createGraphics();

        // Gradient background
        GradientPaint gradient = new GradientPaint(
                0, 0, new Color(60, 60, 70),
                width, height, new Color(40, 40, 50)
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, width, height);

        // Music note icon
        g2d.setColor(new Color(100, 100, 110));
        g2d.setFont(new Font("Segoe UI", Font.BOLD, Math.min(width, height) / 2));
        FontMetrics fm = g2d.getFontMetrics();
        String text = "♪";
        int x = (width - fm.stringWidth(text)) / 2;
        int y = ((height - fm.getHeight()) / 2) + fm.getAscent();
        g2d.drawString(text, x, y);

        g2d.dispose();
        return new ImageIcon(placeholder);
    }

    /**
     * Create circular image (for avatars/album art)
     */
    public static ImageIcon createCircularImage(ImageIcon icon, int size) {
        BufferedImage output = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = output.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, size, size));

        Image img = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
        g2d.drawImage(img, 0, 0, null);
        g2d.dispose();

        return new ImageIcon(output);
    }
}
