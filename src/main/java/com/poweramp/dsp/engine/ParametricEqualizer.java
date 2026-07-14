package com.poweramp.dsp.engine;

import com.poweramp.dsp.filter.BiquadFilter;
import com.poweramp.dsp.filter.FilterFactory;
import com.poweramp.dsp.model.EqBand;
import java.util.ArrayList;
import java.util.List;

public class ParametricEqualizer {
    private final List<EqBand> bands;
    private final List<BiquadFilter> filters;
    private final float sampleRate;
    private boolean enabled;
    private float preamp;

    public ParametricEqualizer(float sampleRate) {
        this.sampleRate = sampleRate;
        this.bands = new ArrayList<>();
        this.filters = new ArrayList<>();
        this.enabled = true;
        this.preamp = 0;
    }

    public void setBands(List<EqBand> newBands) {
        bands.clear();
        filters.clear();
        for (EqBand band : newBands) {
            addBand(band);
        }
    }

    public void addBand(EqBand band) {
        bands.add(band.copy());
        filters.add(FilterFactory.createFilter(band, sampleRate));
    }

    public void updateBand(int index, EqBand band) {
        if (index < 0 || index >= bands.size()) return;
        bands.set(index, band.copy());
        filters.set(index, FilterFactory.createFilter(band, sampleRate));
    }

    public void removeBand(int index) {
        if (index < 0 || index >= bands.size()) return;
        bands.remove(index);
        filters.remove(index);
    }

    public void process(float[] samples, int offset, int count) {
        if (!enabled) return;

        if (preamp != 0) {
            double preampScale = Math.pow(10.0, preamp / 20.0);
            for (int i = 0; i < count; i++) {
                samples[offset + i] *= preampScale;
            }
        }

        for (BiquadFilter filter : filters) {
            filter.process(samples, offset, count);
        }
    }

    public void clear() {
        bands.clear();
        filters.clear();
    }

    public List<BiquadFilter> getFilters() { return List.copyOf(filters); }
    public List<EqBand> getBands() { return List.copyOf(bands); }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public float getPreamp() { return preamp; }
    public void setPreamp(float preamp) { this.preamp = preamp; }
}
