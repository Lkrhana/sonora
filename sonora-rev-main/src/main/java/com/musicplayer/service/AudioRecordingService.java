package com.musicplayer.service;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;


// Service untuk merekam audio dari mikrofon

public class AudioRecordingService {
    private TargetDataLine targetLine;
    private boolean isRecording = false;
    private ByteArrayOutputStream recordedData;

    // Start recording dari mikrofon

    public void startRecording() throws LineUnavailableException {
        AudioFormat format = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                44100, // Sample rate
                16,    // Sample size in bits
                2,     // Channels (stereo)
                4,     // Frame size
                44100, // Frame rate
                false  // Big endian
        );

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Line not supported");
        }

        targetLine = (TargetDataLine) AudioSystem.getLine(info);
        targetLine.open(format);
        targetLine.start();

        recordedData = new ByteArrayOutputStream();
        isRecording = true;

        // Start recording thread
        new Thread(() -> {
            byte[] buffer = new byte[4096];
            while (isRecording) {
                int count = targetLine.read(buffer, 0, buffer.length);
                if (count > 0) {
                    recordedData.write(buffer, 0, count);
                }
            }
        }).start();
    }

 // Stop recording safe ke file
    public File stopRecording(String outputPath) throws IOException {
        isRecording = false;

        if (targetLine != null) {
            targetLine.stop();
            targetLine.close();
        }

        // Save to WAV file
        byte[] audioData = recordedData.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(audioData);

        AudioFormat format = targetLine.getFormat();
        AudioInputStream audioInputStream = new AudioInputStream(
                bais, format, audioData.length / format.getFrameSize());

        File outputFile = new File(outputPath);
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile);

        return outputFile;
    }

    public boolean isRecording() {
        return isRecording;
    }
}