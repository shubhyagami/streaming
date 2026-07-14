package com.poweramp.dsp.filter;

public class PeakingFilter extends BiquadFilter {
    public PeakingFilter(float sampleRate, float freq, float gainDb, float q) {
        super(sampleRate);
        calculateCoeffs(freq, gainDb, q);
    }

    private void calculateCoeffs(float freq, float gainDb, float q) {
        double omega = 2.0 * Math.PI * freq / sampleRate;
        double sn = Math.sin(omega);
        double cs = Math.cos(omega);
        double a = Math.pow(10.0, gainDb / 40.0);
        double alpha = sn / (2.0 * q);

        b0 = 1.0 + alpha * a;
        b1 = -2.0 * cs;
        b2 = 1.0 - alpha * a;
        a0 = 1.0 + alpha / a;
        a1 = -2.0 * cs;
        a2 = 1.0 - alpha / a;

        double invA0 = 1.0 / a0;
        b0 *= invA0;
        b1 *= invA0;
        b2 *= invA0;
        a1 *= invA0;
        a2 *= invA0;
    }
}
