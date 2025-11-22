package com.musicplayer.service;

import be.tarsos.dsp.util.fft.FFT;
import java.util.Arrays;

public class AudioVisualizerService {

    private float[] spectrumData;
    private float[] waveformData;
    private VisualizerListener listener;

    private static final int BUFFER_SIZE = 1024; 
    private static final int NUM_BARS = 64;
    
    private float[] fftBuffer;
    private int bufferPointer = 0;
    private FFT fft;
    
    private boolean enabled = false; 

    public AudioVisualizerService() {
        spectrumData = new float[NUM_BARS];
        waveformData = new float[BUFFER_SIZE];
        fftBuffer = new float[BUFFER_SIZE]; 
        fft = new FFT(BUFFER_SIZE);
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            clear();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void processAudioSamples(float[] samples) {
        if (!enabled || samples == null) return;

        for (float sample : samples) {
            if (bufferPointer < BUFFER_SIZE) {
                fftBuffer[bufferPointer++] = sample;
            }

            if (bufferPointer >= BUFFER_SIZE) {
                calculateFFTAndWaveform();
                bufferPointer = 0; 
            }
        }
    }

    private void calculateFFTAndWaveform() {
        System.arraycopy(fftBuffer, 0, waveformData, 0, BUFFER_SIZE);

        float[] processingBuffer = Arrays.copyOf(fftBuffer, BUFFER_SIZE);
        float[] fftData = new float[BUFFER_SIZE * 2];
        System.arraycopy(processingBuffer, 0, fftData, 0, BUFFER_SIZE);
        
        fft.forwardTransform(fftData);
        
        // hitung modulus
        float[] modulusData = new float[BUFFER_SIZE];
        fft.modulus(fftData, modulusData);

        int validBins = BUFFER_SIZE / 2;
        int samplesPerBar = validBins / NUM_BARS;
        if (samplesPerBar < 1) samplesPerBar = 1;

        for (int i = 0; i < NUM_BARS; i++) {
            float sum = 0;
            for (int j = i * samplesPerBar; j < (i + 1) * samplesPerBar; j++) {
                if (j < modulusData.length) sum += modulusData[j];
            }
            
            float average = sum / samplesPerBar;

            float value = (float) (Math.log10(1 + average) * 3.5); 

            if (value > 1.0f) value = 1.0f;
            if (value < 0.0f) value = 0.0f;
            
            spectrumData[i] = value;
        }

        if (listener != null) {
            listener.onVisualizationUpdate(spectrumData, waveformData);
        }
    }
    
    public void clear() {
        Arrays.fill(spectrumData, 0);
        Arrays.fill(waveformData, 0);
        bufferPointer = 0;
        if (listener != null) {
            listener.onVisualizationUpdate(spectrumData, waveformData);
        }
    }

    public void setVisualizerListener(VisualizerListener listener) {
        this.listener = listener;
    }

    public interface VisualizerListener {
        void onVisualizationUpdate(float[] spectrum, float[] waveform);
    }
}