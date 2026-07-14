package com.poweramp.dsp.engine;

import com.poweramp.dsp.filter.BiquadFilter;
import com.poweramp.dsp.filter.PeakingFilter;
import java.util.ArrayList;
import java.util.List;

public class GraphicEqualizer {
    public static final float[] ISO_1_3_OCTAVE_FREQS = {
        20, 25, 31.5f, 40, 50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630, 800,
        1000, 1250, 1600, 2000, 2500, 3150, 4000, 5000, 6300, 8000, 10000, 12500, 16000, 20000
    };

    private final BiquadFilter[] filters;
    private final float sampleRate;
    private boolean enabled;
    private float preamp;

    public GraphicEqualizer(float sampleRate) {
        this.sampleRate = sampleRate;
        this.filters = new BiquadFilter[ISO_1_3_OCTAVE_FREQS.length];
        this.enabled = true;
        this.preamp = 0;
        for (int i = 0; i < filters.length; i++) {
            filters[i] = new PeakingFilter(sampleRate, ISO_1_3_OCTAVE_FREQS[i], 0, 0.707f);
        }
    }

    public void setGain(int band, float gainDb) {
        if (band < 0 || band >= filters.length) return;
        filters[band] = new PeakingFilter(sampleRate, ISO_1_3_OCTAVE_FREQS[band], gainDb, 0.707f);
    }

    public void setGains(float[] gains) {
        int count = Math.min(gains.length, filters.length);
        for (int i = 0; i < count; i++) {
            filters[i] = new PeakingFilter(sampleRate, ISO_1_3_OCTAVE_FREQS[i], gains[i], 0.707f);
        }
    }

    public void setBandwidth(int band, float q) {
        if (band < 0 || band >= filters.length) return;
        BiquadFilter old = filters[band];
        double gain = old.getMagnitudeResponse(ISO_1_3_OCTAVE_FREQS[band]);
        float gainDb = (float) gain;
        filters[band] = new PeakingFilter(sampleRate, ISO_1_3_OCTAVE_FREQS[band], gainDb, q);
    }

    public void process(float[] samples, int offset, int count) {
        if (!enabled) return;

        if (preamp != 0 && preamp != 1.0f) {
            double preampScale = Math.pow(10.0, preamp / 20.0);
            for (int i = 0; i < count; i++) {
                samples[offset + i] *= preampScale;
            }
        }

        for (BiquadFilter filter : filters) {
            filter.process(samples, offset, count);
        }
    }

    public void processInterleaved(float[] samples, int offset, int sampleCount, int channels, int channel) {
        if (!enabled) return;

        if (preamp != 0) {
            double preampScale = Math.pow(10.0, preamp / 20.0);
            for (int i = 0; i < sampleCount; i++) {
                samples[offset + i * channels + channel] *= preampScale;
            }
        }

        for (BiquadFilter filter : filters) {
            filter.processInterleaved(samples, offset, sampleCount, channels, channel);
        }
    }

    public void reset() {
        for (int i = 0; i < filters.length; i++) {
            filters[i] = new PeakingFilter(sampleRate, ISO_1_3_OCTAVE_FREQS[i], 0, 0.707f);
        }
        preamp = 0;
    }

    public List<BiquadFilter> getFilters() {
        return List.of(filters);
    }

    public float getFrequency(int band) { return ISO_1_3_OCTAVE_FREQS[band]; }
    public int getBandCount() { return filters.length; }
    public void setSampleRate(float sampleRate) {
        float[] gains = new float[filters.length];
        for (int i = 0; i < filters.length; i++) {
            gains[i] = (float) filters[i].getMagnitudeResponse(ISO_1_3_OCTAVE_FREQS[i]);
        }
        for (int i = 0; i < filters.length; i++) {
            filters[i] = new PeakingFilter(sampleRate, ISO_1_3_OCTAVE_FREQS[i], gains[i], 0.707f);
        }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public float getPreamp() { return preamp; }
    public void setPreamp(float preamp) { this.preamp = preamp; }
}
