package com.musicplayer.view;

import com.musicplayer.controller.MusicPlayerController;
import com.musicplayer.service.AudioVisualizerService;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public class EqualizerPanel extends JPanel {
    private MusicPlayerController controller;
    
    // komponen UI
    private JToggleButton enableButton;
    private JComboBox<String> presetComboBox;
    private JButton resetButton;
    private JSlider preampSlider;
    private JSlider[] bandSliders;
    private JLabel[] bandLabels;
    private JLabel preampValueLabel;
    private JLabel[] bandValueLabels;
    private JToggleButton visualizerToggle;
    private SpectrumAnalyzerPanel spectrumPanel;
    private WaveformPanel waveformPanel;
    
    // nama frequency band
    private static final String[] BAND_NAMES = {
        "60Hz", "170Hz", "310Hz", "600Hz", "1kHz",
        "3kHz", "6kHz", "12kHz", "14kHz", "16kHz"
    };
    
    private boolean isUpdating = false; // supaya ga feedback loop

    public EqualizerPanel(MusicPlayerController controller) {
        this.controller = controller;
        initializeComponents();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(18, 18, 20));
        
        // main panel spy bisa scroll
        JPanel mainPanel = new JPanel(new MigLayout("fillx, insets 20", "[grow]", "[]20[]20[]"));
        mainPanel.setBackground(new Color(18, 18, 20));

        // eq section
        JPanel equalizerSection = createEqualizerSection();
        mainPanel.add(equalizerSection, "wrap, growx");
        
        // visualizer section
        JPanel visualizerSection = createVisualizerSection();
        mainPanel.add(visualizerSection, "wrap, growx");
        
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        
        add(scrollPane, BorderLayout.CENTER);
    }
    
    private JPanel createEqualizerSection() {
        JPanel section = new JPanel(new BorderLayout(10, 10));
        section.setBackground(new Color(18, 18, 20));

        // top panel
        JPanel topPanel = createTopPanel();
        section.add(topPanel, BorderLayout.NORTH);

        // center panel
        JPanel centerPanel = createCenterPanel();
        section.add(centerPanel, BorderLayout.CENTER);

        // bottom panel
        JPanel bottomPanel = createBottomPanel();
        section.add(bottomPanel, BorderLayout.SOUTH);

        return section;
    }
    
    private JPanel createVisualizerSection() {
        JPanel section = new JPanel(new BorderLayout(10, 10));
        section.setBackground(new Color(18, 18, 20));
        section.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(2, 0, 0, 0, new Color(60, 60, 65)),
            BorderFactory.createEmptyBorder(20, 0, 0, 0)
        ));

        // Header
        JPanel headerPanel = new JPanel(new MigLayout("fillx", "[]push[]", ""));
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("AUDIO VISUALIZER");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);

        visualizerToggle = new JToggleButton("OFF");
        visualizerToggle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        visualizerToggle.setFocusPainted(false);
        visualizerToggle.setPreferredSize(new Dimension(80, 35));
        visualizerToggle.addActionListener(e -> toggleVisualizer());
        updateVisualizerToggleStyle();

        headerPanel.add(titleLabel);
        headerPanel.add(visualizerToggle);

        section.add(headerPanel, BorderLayout.NORTH);

        // visualizer panels
        JPanel visualizerPanels = new JPanel(new MigLayout("fillx", "[grow]", "[]10[]"));
        visualizerPanels.setOpaque(false);

        // spectrum analyzer
        JPanel spectrumContainer = new JPanel(new BorderLayout(0, 5));
        spectrumContainer.setOpaque(false);
        
        JLabel spectrumLabel = new JLabel("Spectrum Analyzer");
        spectrumLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        spectrumLabel.setForeground(new Color(180, 180, 180));
        spectrumContainer.add(spectrumLabel, BorderLayout.NORTH);
        
        spectrumPanel = new SpectrumAnalyzerPanel(64);
        spectrumContainer.add(spectrumPanel, BorderLayout.CENTER);
        
        visualizerPanels.add(spectrumContainer, "wrap, growx");

        // waveform
        JPanel waveformContainer = new JPanel(new BorderLayout(0, 5));
        waveformContainer.setOpaque(false);
        
        JLabel waveformLabel = new JLabel("Waveform");
        waveformLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        waveformLabel.setForeground(new Color(180, 180, 180));
        waveformContainer.add(waveformLabel, BorderLayout.NORTH);
        
        waveformPanel = new WaveformPanel();
        waveformContainer.add(waveformPanel, BorderLayout.CENTER);
        
        visualizerPanels.add(waveformContainer, "growx");

        section.add(visualizerPanels, BorderLayout.CENTER);

        return section;
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new MigLayout("fillx", "[]push[]20[]", ""));
        panel.setOpaque(false);

        // title
        JLabel titleLabel = new JLabel("AUDIO EQUALIZER");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);

        // enable/disable toggle button
        enableButton = new JToggleButton("OFF");
        enableButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        enableButton.setFocusPainted(false);
        enableButton.setPreferredSize(new Dimension(80, 35));
        enableButton.addActionListener(e -> toggleEqualizer());
        updateEnableButtonStyle();

        // preset selector
        JLabel presetLabel = new JLabel("Preset:");
        presetLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        presetLabel.setForeground(new Color(180, 180, 180));

        presetComboBox = new JComboBox<>(controller.getAvailablePresets());
        presetComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        presetComboBox.setPreferredSize(new Dimension(150, 30));
        presetComboBox.addActionListener(e -> loadPreset());

        // reset button
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

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new MigLayout("fillx", "[]20[]", "[]10[]"));
        panel.setOpaque(false);

        // preamp section
        JPanel preampPanel = createPreampPanel();
        panel.add(preampPanel, "grow");

        // frequency bands section
        JPanel bandsPanel = createBandsPanel();
        panel.add(bandsPanel, "grow, wrap");

        return panel;
    }

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

        // preamp value label
        preampValueLabel = new JLabel("0.0 dB");
        preampValueLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        preampValueLabel.setForeground(new Color(100, 180, 255));
        panel.add(preampValueLabel, "wrap");

        // preamp slider (vertical)
        preampSlider = new JSlider(JSlider.VERTICAL, -200, 200, 0); // -20.0 to +20.0 (x10)
        preampSlider.setOpaque(false);
        preampSlider.setPreferredSize(new Dimension(50, 250));
        preampSlider.setMajorTickSpacing(100);
        preampSlider.setPaintTicks(false);
        preampSlider.addChangeListener(e -> updatePreamp());
        panel.add(preampSlider, "grow, wrap");

        // scale labels
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

        // panel untuk semua sliders (2 rows x 5 columns)
        JPanel slidersPanel = new JPanel(new GridLayout(2, 5, 15, 20)); // 2 rows, 5 cols, hgap=15, vgap=20
        slidersPanel.setOpaque(false);

        // create 10 slider panels
        for (int i = 0; i < 10; i++) {
            final int index = i;

            // panel untuk 1 slider (value label + slider + freq label)
            JPanel sliderPanel = new JPanel(new BorderLayout(0, 5));
            sliderPanel.setOpaque(false);

            // value label (top)
            bandValueLabels[i] = new JLabel("0.0", SwingConstants.CENTER);
            bandValueLabels[i].setFont(new Font("Segoe UI", Font.PLAIN, 11));
            bandValueLabels[i].setForeground(new Color(100, 180, 255));
            sliderPanel.add(bandValueLabels[i], BorderLayout.NORTH);

            // slider (center) - vertical
            bandSliders[i] = new JSlider(JSlider.VERTICAL, -200, 200, 0);
            bandSliders[i].setOpaque(false);
            bandSliders[i].setPreferredSize(new Dimension(40, 150));
            bandSliders[i].addChangeListener(e -> updateBand(index));
            sliderPanel.add(bandSliders[i], BorderLayout.CENTER);

            // frequency label (bottom)
            bandLabels[i] = new JLabel(BAND_NAMES[i], SwingConstants.CENTER);
            bandLabels[i].setFont(new Font("Segoe UI", Font.PLAIN, 11));
            bandLabels[i].setForeground(new Color(180, 180, 180));
            sliderPanel.add(bandLabels[i], BorderLayout.SOUTH);

            // add to grid
            slidersPanel.add(sliderPanel);
        }

        mainPanel.add(slidersPanel, BorderLayout.CENTER);

        return mainPanel;
    }

    private JPanel createBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setOpaque(false);

        JLabel infoLabel = new JLabel("Drag sliders to adjust frequency response • Range: -20 dB to +20 dB");
        infoLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        infoLabel.setForeground(new Color(120, 120, 120));
        panel.add(infoLabel);

        return panel;
    }

    private void toggleEqualizer() {
        boolean enabled = enableButton.isSelected();
        controller.setEqualizerEnabled(enabled);
        updateEnableButtonStyle();
        
        // enable/disable all controls
        preampSlider.setEnabled(enabled);
        presetComboBox.setEnabled(enabled);
        resetButton.setEnabled(enabled);
        for (JSlider slider : bandSliders) {
            slider.setEnabled(enabled);
        }
        
        System.out.println("🎛️ Equalizer " + (enabled ? "enabled" : "disabled"));
    }

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

    private void updatePreamp() {
        if (isUpdating) return;
        
        float value = preampSlider.getValue() / 10.0f; // Convert to dB
        controller.setPreamp(value);
        preampValueLabel.setText(String.format("%.1f dB", value));
    }

    private void updateBand(int index) {
        if (isUpdating) return;
        
        float value = bandSliders[index].getValue() / 10.0f; // Convert to dB
        controller.setBandAmplitude(index, value);
        bandValueLabels[index].setText(String.format("%.1f", value));
    }

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

    private void toggleVisualizer() {
        boolean enabled = visualizerToggle.isSelected();
        System.out.println("🔥 Visualizer toggle: " + (enabled ? "ON" : "OFF"));
        controller.toggleVisualizer(enabled);
        updateVisualizerToggleStyle();

        if (!enabled) {
            spectrumPanel.clear();
            waveformPanel.clear();
        }
    }
    
    public void updateVisualization(float[] spectrum, float[] waveform) {
        SwingUtilities.invokeLater(() -> {
            spectrumPanel.updateSpectrum(spectrum);
            waveformPanel.updateWaveform(waveform);
        });
    }
    
    private void updateVisualizerToggleStyle() {
        if (visualizerToggle.isSelected()) {
            visualizerToggle.setText("ON");
            visualizerToggle.setBackground(new Color(50, 150, 50));
            visualizerToggle.setForeground(Color.WHITE);
        } else {
            visualizerToggle.setText("OFF");
            visualizerToggle.setBackground(new Color(80, 80, 85));
            visualizerToggle.setForeground(Color.LIGHT_GRAY);
        }
    }
}
