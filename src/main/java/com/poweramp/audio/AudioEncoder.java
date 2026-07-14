package com.poweramp.audio;

import javax.sound.sampled.*;
import java.io.*;

public class AudioEncoder {

    public void encodeWav(float[] samples, int sampleRate, int channels, int bitsPerSample, String outputPath) throws IOException {
        AudioFormat format = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sampleRate,
            bitsPerSample,
            channels,
            channels * (bitsPerSample / 8),
            sampleRate,
            false
        );

        byte[] rawBytes;
        if (bitsPerSample == 16) {
            rawBytes = new byte[samples.length * 2];
            for (int i = 0; i < samples.length; i++) {
                short s = (short) (Math.max(-1, Math.min(1, samples[i])) * 32767);
                rawBytes[i * 2] = (byte) (s & 0xFF);
                rawBytes[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
            }
        } else if (bitsPerSample == 8) {
            rawBytes = new byte[samples.length];
            for (int i = 0; i < samples.length; i++) {
                rawBytes[i] = (byte) ((Math.max(-1, Math.min(1, samples[i])) + 1.0f) * 127.5f);
            }
        } else {
            throw new UnsupportedOperationException("Unsupported bits per sample: " + bitsPerSample);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(rawBytes);
        AudioInputStream ais = new AudioInputStream(bais, format, samples.length / channels);
        AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(outputPath));
    }
}
