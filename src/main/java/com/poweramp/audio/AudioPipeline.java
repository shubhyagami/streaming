package com.poweramp.audio;

import com.poweramp.dsp.engine.DspProcessor;
import com.poweramp.dsp.model.EqPreset;
import com.poweramp.dsp.model.ReverbPreset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;

public class AudioPipeline {

    private static final Logger log = LoggerFactory.getLogger(AudioPipeline.class);

    private final AudioDecoder decoder;
    private final AudioEncoder encoder;
    private final DspProcessor dspProcessor;

    public AudioPipeline() {
        this.decoder = new AudioDecoder();
        this.encoder = new AudioEncoder();
        this.dspProcessor = new DspProcessor(44100);
    }

    public ProcessingResult process(String inputPath, String outputPath, EqPreset preset,
                                     ReverbPreset reverb, Consumer<Float> progressCallback) throws Exception {
        log.info("Processing: {} -> {}", inputPath, outputPath);

        AudioDecoder.AudioData audio = decoder.decode(inputPath);
        log.info("Decoded: {}Hz, {}ch, {}bits, {} samples",
            audio.sampleRate(), audio.channels(), audio.bitsPerSample(), audio.samples().length);

        if (audio.sampleRate() != 44100) {
            dspProcessor.getGraphicEq().setSampleRate(audio.sampleRate());
        }

        if (preset != null) {
            dspProcessor.applyPreset(preset);
        }

        if (reverb != null) {
            dspProcessor.getReverbProcessor().applyPreset(reverb);
            dspProcessor.getReverbProcessor().setEnabled(true);
        }

        float[] samples = audio.samples();
        int sampleCount = samples.length;
        int chunkSize = 44100; // process 1 second at a time
        int channels = audio.channels();

        for (int offset = 0; offset < sampleCount; offset += chunkSize) {
            int count = Math.min(chunkSize, sampleCount - offset);
            dspProcessor.process(samples, offset, count, channels);

            if (progressCallback != null) {
                progressCallback.accept((float) (offset + count) / sampleCount);
            }
        }

        encoder.encodeWav(samples, audio.sampleRate(), audio.channels(), audio.bitsPerSample(), outputPath);
        log.info("Output written to: {}", outputPath);

        return new ProcessingResult(inputPath, outputPath, sampleCount, audio.sampleRate());
    }

    public DspProcessor getDspProcessor() {
        return dspProcessor;
    }

    public record ProcessingResult(String inputPath, String outputPath, int totalSamples, int sampleRate) {
        public float durationSeconds() {
            return (float) totalSamples / sampleRate;
        }
    }
}
