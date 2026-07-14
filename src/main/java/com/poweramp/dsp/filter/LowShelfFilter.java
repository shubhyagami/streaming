package com.poweramp.dsp.filter;

public class LowShelfFilter extends BiquadFilter {
    public LowShelfFilter(float sampleRate, float freq, float gainDb, float q) {
        super(sampleRate);
        calculateCoeffs(freq, gainDb, q);
    }

    private void calculateCoeffs(float freq, float gainDb, float q) {
        double omega = 2.0 * Math.PI * freq / sampleRate;
        double sn = Math.sin(omega);
        double cs = Math.cos(omega);
        double a = Math.pow(10.0, gainDb / 40.0);
        double alpha = sn / (2.0 * q);
        double beta = Math.sqrt(a) / q;
        double aPlus1 = a + 1.0;
        double aMinus1 = a - 1.0;

        b0 = a * (aPlus1 - aMinus1 * cs + beta * sn);
        b1 = 2.0 * a * (aMinus1 - aPlus1 * cs);
        b2 = a * (aPlus1 - aMinus1 * cs - beta * sn);
        a0 = aPlus1 + aMinus1 * cs + beta * sn;
        a1 = -2.0 * (aMinus1 + aPlus1 * cs);
        a2 = aPlus1 + aMinus1 * cs - beta * sn;

        double invA0 = 1.0 / a0;
        b0 *= invA0;
        b1 *= invA0;
        b2 *= invA0;
        a1 *= invA0;
        a2 *= invA0;
    }
}
