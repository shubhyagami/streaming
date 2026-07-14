package com.poweramp.dsp.filter;

public class BiquadFilter {
    protected double b0, b1, b2, a0, a1, a2;
    protected double x1, x2, y1, y2;
    protected float sampleRate;

    public BiquadFilter(float sampleRate) {
        this.sampleRate = sampleRate;
        reset();
    }

    public void reset() {
        b0 = b1 = b2 = a1 = a2 = 0; a0 = 1;
        x1 = x2 = y1 = y2 = 0;
    }

    public void clearState() {
        x1 = x2 = y1 = y2 = 0;
    }

    public float process(float input) {
        double output = b0 * input + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
        x2 = x1;
        x1 = input;
        y2 = y1;
        y1 = output;
        return (float) output;
    }

    public void process(float[] samples, int offset, int count) {
        for (int i = 0; i < count; i++) {
            int idx = offset + i;
            double input = samples[idx];
            double output = b0 * input + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            x2 = x1;
            x1 = input;
            y2 = y1;
            y1 = output;
            samples[idx] = (float) output;
        }
    }

    public void processInterleaved(float[] samples, int offset, int count, int channelCount, int channel) {
        for (int i = 0; i < count; i++) {
            int idx = offset + i * channelCount + channel;
            double input = samples[idx];
            double output = b0 * input + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            x2 = x1;
            x1 = input;
            y2 = y1;
            y1 = output;
            samples[idx] = (float) output;
        }
    }

    public double getMagnitudeResponse(double freq) {
        double omega = 2.0 * Math.PI * freq / sampleRate;
        double phi = Math.sin(omega / 2.0);
        double phi2 = phi * phi;
        double c = Math.cos(omega);
        double b0_2 = b0 + b1 * c + b2 * Math.cos(2 * omega);
        double b0_i = b1 * Math.sin(omega) + b2 * Math.sin(2 * omega);
        double a0_2 = 1 + a1 * c + a2 * Math.cos(2 * omega);
        double a0_i = a1 * Math.sin(omega) + a2 * Math.sin(2 * omega);
        double numerator = b0_2 * b0_2 + b0_i * b0_i;
        double denominator = a0_2 * a0_2 + a0_i * a0_i;
        if (denominator == 0) return 0;
        return 10.0 * Math.log10(numerator / denominator);
    }

    public void setSampleRate(float sampleRate) {
        this.sampleRate = sampleRate;
    }

    protected float normalizedFreq(float freq) {
        return freq / sampleRate;
    }

    public BiquadFilter copy() {
        BiquadFilter f = new BiquadFilter(sampleRate);
        f.b0 = b0; f.b1 = b1; f.b2 = b2; f.a0 = a0; f.a1 = a1; f.a2 = a2;
        return f;
    }
}
