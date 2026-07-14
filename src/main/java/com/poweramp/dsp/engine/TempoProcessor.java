package com.poweramp.dsp.engine;

public class TempoProcessor {
    private boolean enabled;
    private float tempo;
    private int sampleRate;

    public TempoProcessor(int sampleRate) {
        this.enabled = false;
        this.tempo = 1.0f;
        this.sampleRate = sampleRate;
    }

    public float[] process(float[] samples, int offset, int count) {
        if (!enabled || Math.abs(tempo - 1.0f) < 0.01f) {
            if (offset == 0 && count == samples.length) return samples;
            float[] out = new float[count];
            System.arraycopy(samples, offset, out, 0, count);
            return out;
        }

        int outputLen = (int) (count / tempo);
        float[] output = new float[outputLen];
        for (int i = 0; i < outputLen; i++) {
            float srcPos = i * tempo;
            int srcIdx = (int) srcPos;
            float frac = srcPos - srcIdx;
            if (srcIdx + 1 < count) {
                output[i] = samples[offset + srcIdx] * (1 - frac) + samples[offset + srcIdx + 1] * frac;
            } else if (srcIdx < count) {
                output[i] = samples[offset + srcIdx];
            }
        }
        return output;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public float getTempo() { return tempo; }
    public void setTempo(float tempo) { this.tempo = Math.max(0.5f, Math.min(3.0f, tempo)); }
}
