package com.poweramp.dsp.filter;

public class BandPassFilter extends BiquadFilter {
    public BandPassFilter(float sampleRate, float freq, float q) {
        super(sampleRate);
        calculateCoeffs(freq, q);
    }

    private void calculateCoeffs(float freq, float q) {
        double omega = 2.0 * Math.PI * freq / sampleRate;
        double sn = Math.sin(omega);
        double cs = Math.cos(omega);
        double alpha = sn / (2.0 * q);

        b0 = alpha;
        b1 = 0;
        b2 = -alpha;
        a0 = 1.0 + alpha;
        a1 = -2.0 * cs;
        a2 = 1.0 - alpha;

        double invA0 = 1.0 / a0;
        b0 *= invA0;
        b1 *= invA0;
        b2 *= invA0;
        a1 *= invA0;
        a2 *= invA0;
    }
}
