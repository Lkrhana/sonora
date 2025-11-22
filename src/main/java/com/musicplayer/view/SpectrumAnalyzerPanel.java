package com.musicplayer.view;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Custom panel untuk menampilkan spectrum analyzer dengan bars
 * Sesuai dengan tema dark UI aplikasi
 */
public class SpectrumAnalyzerPanel extends JPanel {
    
    private float[] spectrumData;
    private int numBars;
    
    // Colors matching app theme
    private static final Color BACKGROUND_COLOR = new Color(30, 30, 35);
    private static final Color BAR_COLOR_START = new Color(100, 180, 255); // Blue accent
    private static final Color BAR_COLOR_END = new Color(50, 120, 200);
    private static final Color GRID_COLOR = new Color(50, 50, 55);
    
    private GradientPaint barGradient;
    
    public SpectrumAnalyzerPanel(int numBars) {
        this.numBars = numBars;
        this.spectrumData = new float[numBars];
        
        setBackground(BACKGROUND_COLOR);
        setPreferredSize(new Dimension(600, 150));
        setBorder(BorderFactory.createLineBorder(new Color(60, 60, 65), 1));
    }
    
    /**
     * Update spectrum data
     */
    public void updateSpectrum(float[] newData) {
        if (newData != null && newData.length == numBars) {
            System.arraycopy(newData, 0, spectrumData, 0, numBars);
            repaint(); // Trigger redraw
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // Enable anti-aliasing for smooth graphics
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int width = getWidth();
        int height = getHeight();
        
        // Draw background grid (optional)
        drawGrid(g2d, width, height);
        
        // Calculate bar dimensions
        int barWidth = (width - (numBars + 1) * 2) / numBars; // 2px spacing
        int spacing = 2;
        
        // Create gradient for bars
        barGradient = new GradientPaint(
            0, height, BAR_COLOR_START,
            0, 0, BAR_COLOR_END
        );
        
        // Draw spectrum bars
        for (int i = 0; i < numBars; i++) {
            int x = i * (barWidth + spacing) + spacing;
            int barHeight = (int) (spectrumData[i] * (height - 10)); // Leave margin
            int y = height - barHeight - 5; // 5px bottom margin
            
            // Apply gradient
            g2d.setPaint(barGradient);
            
            // Draw rounded rectangle bar
            RoundRectangle2D bar = new RoundRectangle2D.Float(
                x, y, barWidth, barHeight, 3, 3 // 3px corner radius
            );
            g2d.fill(bar);
            
            // Add subtle glow effect for higher values
            if (spectrumData[i] > 0.7f) {
                g2d.setColor(new Color(100, 180, 255, 50)); // Semi-transparent
                g2d.fill(new RoundRectangle2D.Float(
                    x - 1, y - 1, barWidth + 2, barHeight + 2, 4, 4
                ));
            }
        }
    }
    
    /**
     * Draw subtle background grid
     */
    private void drawGrid(Graphics2D g2d, int width, int height) {
        g2d.setColor(GRID_COLOR);
        g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                0, new float[]{2, 4}, 0)); // Dashed line
        
        // Horizontal lines
        int numLines = 4;
        for (int i = 1; i < numLines; i++) {
            int y = (height / numLines) * i;
            g2d.drawLine(0, y, width, y);
        }
    }
    
    /**
     * Clear spectrum (set all to zero)
     */
    public void clear() {
        for (int i = 0; i < spectrumData.length; i++) {
            spectrumData[i] = 0;
        }
        repaint();
    }
}
