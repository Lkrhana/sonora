package com.musicplayer.view;

import com.musicplayer.controller.MusicPlayerController;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 * Panel untuk Audio Equalizer dengan 10 frequency bands
 */
public class EqualizerPanel extends JPanel {
    private MusicPlayerController controller;
    
    // UI Components
    private JToggleButton enableButton;
    private JComboBox<String> presetComboBox;
    private JButton resetButton;
    private JSlider preampSlider;
    private JSlider[] bandSliders;
    private JLabel[] bandLabels;
    private JLabel preampValueLabel;
    private JLabel[] bandValueLabels;
    
    // Frequency band names
    private static final String[] BAND_NAMES = {
        "60Hz", "170Hz", "310Hz", "600Hz", "1kHz",
        "3kHz", "6kHz", "12kHz", "14kHz", "16kHz"
    };
    
    private boolean isUpdating = false; // Prevent feedback loops

    public EqualizerPanel(MusicPlayerController controller) {
        this.controller = controller;
        initializeComponents();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(18, 18, 20));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Top panel - Header & Controls
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        // Center panel - Preamp + Frequency Bands
        JPanel centerPanel = createCenterPanel();
        add(centerPanel, BorderLayout.CENTER);

        // Bottom panel - Info
        JPanel bottomPanel = createBottomPanel();
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * Create top panel with enable button, preset selector, and reset button
     */
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new MigLayout("fillx", "[]push[]20[]", ""));
        panel.setOpaque(false);

        // Title
        JLabel titleLabel = new JLabel("AUDIO EQUALIZER");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);

        // Enable/Disable toggle button
        enableButton = new JToggleButton("OFF");
        enableButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        enableButton.setFocusPainted(false);
        enableButton.setPreferredSize(new Dimension(80, 35));
        enableButton.addActionListener(e -> toggleEqualizer());
        updateEnableButtonStyle();

        // Preset selector
        JLabel presetLabel = new JLabel("Preset:");
        presetLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        presetLabel.setForeground(new Color(180, 180, 180));

        presetComboBox = new JComboBox<>(controller.getAvailablePresets());
        presetComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        presetComboBox.setPreferredSize(new Dimension(150, 30));
        presetComboBox.addActionListener(e -> loadPreset());

        // Reset button
        resetButton = new JButton("Reset");
        resetButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        resetButton.setFocusPainted(false);
        resetButton.setPreferredSize(new Dimension(80, 30));
        resetButton.addActionListener(e -> resetEqualizer());

        panel.add(titleLabel);
        panel.add(enableButton, "split 5, gapleft 20");
        panel.add(presetLabel, "gapleft 15");
        panel.add(presetComboBox);
        panel.add(resetButton, "gapleft 10");

        return panel;
    }

    /**
     * Create center panel with preamp and 10 frequency band sliders
     */
    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new MigLayout("fillx", "[]20[]", "[]10[]"));
        panel.setOpaque(false);

        // Preamp section
        JPanel preampPanel = createPreampPanel();
        panel.add(preampPanel, "grow");

        // Frequency bands section
        JPanel bandsPanel = createBandsPanel();
        panel.add(bandsPanel, "grow, wrap");

        return panel;
    }

    /**
     * Create preamp slider panel
     */
    private JPanel createPreampPanel() {
        JPanel panel = new JPanel(new MigLayout("fill, insets 10", "[center]", "[]10[]push[]10[]"));
        panel.setBackground(new Color(30, 30, 35));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 65), 1),
            "Preamp",
            javax.swing.border.TitledBorder.CENTER,
            javax.swing.border.TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 12),
            new Color(180, 180, 180)
        ));

        // Preamp value label
        preampValueLabel = new JLabel("0.0 dB");
        preampValueLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        preampValueLabel.setForeground(new Color(100, 180, 255));
        panel.add(preampValueLabel, "wrap");

        // Preamp slider (vertical)
        preampSlider = new JSlider(JSlider.VERTICAL, -200, 200, 0); // -20.0 to +20.0 (x10)
        preampSlider.setOpaque(false);
        preampSlider.setPreferredSize(new Dimension(50, 250));
        preampSlider.setMajorTickSpacing(100);
        preampSlider.setPaintTicks(false);
        preampSlider.addChangeListener(e -> updatePreamp());
        panel.add(preampSlider, "grow, wrap");

        // Scale labels
        JLabel maxLabel = new JLabel("+20");
        maxLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        maxLabel.setForeground(new Color(150, 150, 150));
        panel.add(maxLabel, "split 3, flowy");

        JLabel zeroLabel = new JLabel("0");
        zeroLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        zeroLabel.setForeground(new Color(150, 150, 150));
        panel.add(zeroLabel);

        JLabel minLabel = new JLabel("-20");
        minLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        minLabel.setForeground(new Color(150, 150, 150));
        panel.add(minLabel);

        return panel;
    }

    /**
     * Create panel with 10 frequency band sliders (2 ROWS x 5 BANDS - SIMPLE)
     */
    private JPanel createBandsPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(30, 30, 35));
        mainPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 65), 1),
            "Frequency Bands",
            javax.swing.border.TitledBorder.CENTER,
            javax.swing.border.TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 12),
            new Color(180, 180, 180)
        ));

        bandSliders = new JSlider[10];
        bandLabels = new JLabel[10];
        bandValueLabels = new JLabel[10];

        // Panel untuk semua sliders - 2 rows x 5 columns
        JPanel slidersPanel = new JPanel(new GridLayout(2, 5, 15, 20)); // 2 rows, 5 cols, hgap=15, vgap=20
        slidersPanel.setOpaque(false);

        // Create 10 slider panels
        for (int i = 0; i < 10; i++) {
            final int index = i;

            // Panel untuk 1 slider (value label + slider + freq label)
            JPanel sliderPanel = new JPanel(new BorderLayout(0, 5));
            sliderPanel.setOpaque(false);

            // Value label (top)
            bandValueLabels[i] = new JLabel("0.0", SwingConstants.CENTER);
            bandValueLabels[i].setFont(new Font("Segoe UI", Font.PLAIN, 11));
            bandValueLabels[i].setForeground(new Color(100, 180, 255));
            sliderPanel.add(bandValueLabels[i], BorderLayout.NORTH);

            // Slider (center) - vertical
            bandSliders[i] = new JSlider(JSlider.VERTICAL, -200, 200, 0);
            bandSliders[i].setOpaque(false);
            bandSliders[i].setPreferredSize(new Dimension(40, 150));
            bandSliders[i].addChangeListener(e -> updateBand(index));
            sliderPanel.add(bandSliders[i], BorderLayout.CENTER);

            // Frequency label (bottom)
            bandLabels[i] = new JLabel(BAND_NAMES[i], SwingConstants.CENTER);
            bandLabels[i].setFont(new Font("Segoe UI", Font.PLAIN, 11));
            bandLabels[i].setForeground(new Color(180, 180, 180));
            sliderPanel.add(bandLabels[i], BorderLayout.SOUTH);

            // Add to grid
            slidersPanel.add(sliderPanel);
        }

        mainPanel.add(slidersPanel, BorderLayout.CENTER);

        return mainPanel;
    }

    /**
     * Create bottom info panel
     */
    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setOpaque(false);

        JLabel infoLabel = new JLabel("Drag sliders to adjust frequency response • Range: -20 dB to +20 dB");
        infoLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        infoLabel.setForeground(new Color(120, 120, 120));
        panel.add(infoLabel);

        return panel;
    }

    /**
     * Toggle equalizer on/off
     */
    private void toggleEqualizer() {
        boolean enabled = enableButton.isSelected();
        controller.setEqualizerEnabled(enabled);
        updateEnableButtonStyle();
        
        // Enable/disable all controls
        preampSlider.setEnabled(enabled);
        presetComboBox.setEnabled(enabled);
        resetButton.setEnabled(enabled);
        for (JSlider slider : bandSliders) {
            slider.setEnabled(enabled);
        }
        
        System.out.println("🎛️ Equalizer " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Update enable button style
     */
    private void updateEnableButtonStyle() {
        if (enableButton.isSelected()) {
            enableButton.setText("ON");
            enableButton.setBackground(new Color(50, 150, 50));
            enableButton.setForeground(Color.WHITE);
        } else {
            enableButton.setText("OFF");
            enableButton.setBackground(new Color(80, 80, 85));
            enableButton.setForeground(Color.LIGHT_GRAY);
        }
    }

    /**
     * Load selected preset
     */
    private void loadPreset() {
        if (isUpdating) return;
        
        String presetName = (String) presetComboBox.getSelectedItem();
        if (presetName != null) {
            isUpdating = true;
            
            controller.loadPreset(presetName);
            
            // Update UI sliders
            updateSlidersFromController();
            
            isUpdating = false;
            System.out.println("🎵 Loaded preset: " + presetName);
        }
    }

    /**
     * Reset equalizer to flat
     */
    private void resetEqualizer() {
        isUpdating = true;
        
        controller.resetEqualizer();
        
        // Reset UI
        preampSlider.setValue(0);
        for (JSlider slider : bandSliders) {
            slider.setValue(0);
        }
        presetComboBox.setSelectedIndex(0); // Select "Flat"
        
        isUpdating = false;
        System.out.println("🔄 Equalizer reset to flat");
    }

    /**
     * Update preamp value
     */
    private void updatePreamp() {
        if (isUpdating) return;
        
        float value = preampSlider.getValue() / 10.0f; // Convert to dB
        controller.setPreamp(value);
        preampValueLabel.setText(String.format("%.1f dB", value));
    }

    /**
     * Update specific band amplitude
     */
    private void updateBand(int index) {
        if (isUpdating) return;
        
        float value = bandSliders[index].getValue() / 10.0f; // Convert to dB
        controller.setBandAmplitude(index, value);
        bandValueLabels[index].setText(String.format("%.1f", value));
    }

    /**
     * Update all sliders from controller values
     */
    private void updateSlidersFromController() {
        isUpdating = true;
        
        // Update preamp
        float preamp = controller.getPreamp();
        preampSlider.setValue((int)(preamp * 10));
        preampValueLabel.setText(String.format("%.1f dB", preamp));
        
        // Update all bands
        float[] amplitudes = controller.getAllBandAmplitudes();
        for (int i = 0; i < 10; i++) {
            bandSliders[i].setValue((int)(amplitudes[i] * 10));
            bandValueLabels[i].setText(String.format("%.1f", amplitudes[i]));
        }
        
        isUpdating = false;
    }
}
