package com.poweramp.audio;

import javax.sound.sampled.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

public class AudioDecoder {

    public record AudioData(float[] samples, int sampleRate, int channels, int bitsPerSample) {}

    public AudioData decode(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) throw new FileNotFoundException("File not found: " + filePath);

        String name = file.getName().toLowerCase();
        if (name.endsWith(".wav")) {
            return decodeWav(file);
        }
        throw new UnsupportedOperationException("Unsupported format: " + name + " (only WAV supported in this build)");
    }

    private AudioData decodeWav(File file) throws IOException {
        try (AudioInputStream ais = AudioSystem.getAudioInputStream(file)) {
            AudioFormat format = ais.getFormat();
            int sampleRate = (int) format.getSampleRate();
            int channels = format.getChannels();
            int bitsPerSample = format.getSampleSizeInBits();

            byte[] rawBytes = ais.readAllBytes();
            float[] samples;

            if (bitsPerSample == 16) {
                ShortBuffer sb = ByteBuffer.wrap(rawBytes)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .asShortBuffer();
                samples = new float[sb.remaining()];
                for (int i = 0; i < samples.length; i++) {
                    samples[i] = sb.get(i) / 32768.0f;
                }
            } else if (bitsPerSample == 8) {
                samples = new float[rawBytes.length];
                for (int i = 0; i < samples.length; i++) {
                    samples[i] = (rawBytes[i] & 0xFF) / 128.0f - 1.0f;
                }
            } else if (bitsPerSample == 24) {
                int sampleCount = rawBytes.length / 3;
                samples = new float[sampleCount];
                for (int i = 0; i < sampleCount; i++) {
                    int val = (rawBytes[i * 3] & 0xFF)
                            | ((rawBytes[i * 3 + 1] & 0xFF) << 8)
                            | ((rawBytes[i * 3 + 2] & 0xFF) << 16);
                    if ((val & 0x800000) != 0) val |= 0xFF000000;
                    samples[i] = val / 8388608.0f;
                }
            } else {
                throw new UnsupportedOperationException("Unsupported bits per sample: " + bitsPerSample);
            }

            return new AudioData(samples, sampleRate, channels, bitsPerSample);
        } catch (UnsupportedAudioFileException e) {
            throw new IOException("Unsupported audio file: " + e.getMessage());
        }
    }
}
