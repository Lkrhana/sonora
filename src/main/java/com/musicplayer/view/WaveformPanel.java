package com.musicplayer.view;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

public class WaveformPanel extends JPanel {
    
    private float[] waveformData;

    private static final Color BACKGROUND_COLOR = new Color(30, 30, 35);
    private static final Color WAVEFORM_COLOR = new Color(100, 180, 255);
    private static final Color CENTER_LINE_COLOR = new Color(60, 60, 65);
    
    public WaveformPanel() {
        this.waveformData = new float[1024];
        
        setBackground(BACKGROUND_COLOR);
        setPreferredSize(new Dimension(600, 100));
        setBorder(BorderFactory.createLineBorder(new Color(60, 60, 65), 1));
    }

    public void updateWaveform(float[] newData) {
        if (newData != null) {
            if (waveformData.length != newData.length) {
                waveformData = new float[newData.length];
            }

            System.arraycopy(newData, 0, waveformData, 0, newData.length);
            repaint();
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int width = getWidth();
        int height = getHeight();
        int centerY = height / 2;

        g2d.setColor(CENTER_LINE_COLOR);
        g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4, 4}, 0));
        g2d.drawLine(0, centerY, width, centerY);
        
        g2d.setColor(WAVEFORM_COLOR);
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        Path2D path = new Path2D.Float();

        float step = (float) waveformData.length / width;
        
        boolean firstPoint = true;
        for (int x = 0; x < width; x++) {
            int dataIndex = (int) (x * step);
            
            // safety check biar ga crash
            if (dataIndex >= waveformData.length) dataIndex = waveformData.length - 1;

            float value = waveformData[dataIndex];
           
            if (value > 1.0f) value = 1.0f;
            if (value < -1.0f) value = -1.0f;
            
            int y = centerY - (int) (value * (height / 2 - 10));
            
            if (firstPoint) {
                path.moveTo(x, y);
                firstPoint = false;
            } else {
                path.lineTo(x, y);
            }
        }
        
        g2d.draw(path);
        
        g2d.setColor(new Color(100, 180, 255, 30));
        g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.draw(path);
    }
    
    // supaya lurus lg wavenya
    public void clear() {
        for (int i = 0; i < waveformData.length; i++) {
            waveformData[i] = 0;
        }
        repaint();
    }
}