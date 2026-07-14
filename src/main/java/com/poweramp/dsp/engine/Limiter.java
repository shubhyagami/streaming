package com.poweramp.dsp.engine;

public class Limiter {
    private boolean enabled;
    private float threshold;
    private float attackMs;
    private float releaseMs;
    private double gain;
    private static final float SAMPLE_RATE = 44100f;

    public Limiter() {
        this.enabled = true;
        this.threshold = -1.0f;
        this.attackMs = 1.0f;
        this.releaseMs = 50.0f;
        this.gain = 1.0;
    }

    public void process(float[] samples, int offset, int count) {
        if (!enabled) return;

        float attackCoeff = (float) Math.exp(-1000.0 / (attackMs * SAMPLE_RATE));
        float releaseCoeff = (float) Math.exp(-1000.0 / (releaseMs * SAMPLE_RATE));
        double thresholdLinear = Math.pow(10.0, threshold / 20.0);
        double makeupGain = 1.0 / thresholdLinear;

        for (int i = 0; i < count; i++) {
            int idx = offset + i;
            float absSample = Math.abs(samples[idx]);

            double targetGain;
            if (absSample > thresholdLinear) {
                targetGain = thresholdLinear / absSample;
            } else {
                targetGain = 1.0;
            }

            if (targetGain < gain) {
                gain += (targetGain - gain) * (1.0 - attackCoeff);
            } else {
                gain += (targetGain - gain) * (1.0 - releaseCoeff);
            }

            samples[idx] = (float) (samples[idx] * gain * makeupGain);
        }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public float getThreshold() { return threshold; }
    public void setThreshold(float threshold) { this.threshold = threshold; }
}
