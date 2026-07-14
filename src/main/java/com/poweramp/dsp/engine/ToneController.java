package com.poweramp.dsp.engine;

import com.poweramp.dsp.filter.BiquadFilter;
import com.poweramp.dsp.filter.LowShelfFilter;
import com.poweramp.dsp.filter.HighShelfFilter;

public class ToneController {
    private final float sampleRate;
    private BiquadFilter bassFilter;
    private BiquadFilter trebleFilter;
    private boolean enabled;
    private float bassGain;
    private float trebleGain;

    private static final float BASS_FREQ = 100f;
    private static final float TREBLE_FREQ = 10000f;
    private static final float BASS_Q = 0.707f;
    private static final float TREBLE_Q = 0.707f;

    public ToneController(float sampleRate) {
        this.sampleRate = sampleRate;
        this.enabled = false;
        this.bassGain = 0;
        this.trebleGain = 0;
        updateFilters();
    }

    private void updateFilters() {
        if (bassGain != 0) {
            bassFilter = new LowShelfFilter(sampleRate, BASS_FREQ, bassGain, BASS_Q);
        } else {
            bassFilter = null;
        }
        if (trebleGain != 0) {
            trebleFilter = new HighShelfFilter(sampleRate, TREBLE_FREQ, trebleGain, TREBLE_Q);
        } else {
            trebleFilter = null;
        }
    }

    public void process(float[] samples, int offset, int count) {
        if (!enabled) return;
        if (bassFilter != null) bassFilter.process(samples, offset, count);
        if (trebleFilter != null) trebleFilter.process(samples, offset, count);
    }

    public void setBassGain(float gainDb) {
        this.bassGain = gainDb;
        updateFilters();
    }

    public void setTrebleGain(float gainDb) {
        this.trebleGain = gainDb;
        updateFilters();
    }

    public BiquadFilter getBassFilter() { return bassFilter; }
    public BiquadFilter getTrebleFilter() { return trebleFilter; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public float getBassGain() { return bassGain; }
    public float getTrebleGain() { return trebleGain; }
}
