package com.poweramp.dsp.filter;

import com.poweramp.dsp.model.EqBand;
import com.poweramp.dsp.model.FilterType;

public class FilterFactory {

    public static BiquadFilter createFilter(EqBand band, float sampleRate) {
        return createFilter(band.getType(), sampleRate, band.getFrequency(), band.getGain(), band.getQ());
    }

    public static BiquadFilter createFilter(FilterType type, float sampleRate, float freq, float gainDb, float q) {
        if (q <= 0) q = 0.707f;
        return switch (type) {
            case PEAKING -> new PeakingFilter(sampleRate, freq, gainDb, q);
            case LOW_SHELF -> new LowShelfFilter(sampleRate, freq, gainDb, q);
            case HIGH_SHELF -> new HighShelfFilter(sampleRate, freq, gainDb, q);
            case LOW_PASS -> new LowPassFilter(sampleRate, freq, q);
            case HIGH_PASS -> new HighPassFilter(sampleRate, freq, q);
            case BAND_PASS -> new BandPassFilter(sampleRate, freq, q);
        };
    }
}
