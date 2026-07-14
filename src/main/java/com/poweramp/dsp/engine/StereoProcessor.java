package com.poweramp.dsp.engine;

public class StereoProcessor {
    private boolean enabled;
    private float balance;
    private float stereoFx;

    public StereoProcessor() {
        this.enabled = true;
        this.balance = 0.5f;
        this.stereoFx = 0;
    }

    public void process(float[] samples, int offset, int sampleCount) {
        if (!enabled) return;
        for (int i = 0; i < sampleCount; i += 2) {
            int idx = offset + i;
            if (idx + 1 >= samples.length) break;
            float left = samples[idx];
            float right = samples[idx + 1];

            float balLeft = 1.0f;
            float balRight = 1.0f;
            if (balance < 0.5f) {
                balRight = balance * 2.0f;
            } else if (balance > 0.5f) {
                balLeft = (1.0f - balance) * 2.0f;
            }

            if (stereoFx > 0) {
                float mid = (left + right) * 0.5f;
                float side = (right - left) * 0.5f * (1.0f + stereoFx);
                left = mid - side;
                right = mid + side;
            }

            samples[idx] = left * balLeft;
            samples[idx + 1] = right * balRight;
        }
    }

    public void setBalance(float balance) { this.balance = Math.max(0, Math.min(1, balance)); }
    public void setStereoFx(float stereoFx) { this.stereoFx = Math.max(0, Math.min(1, stereoFx)); }
    public float getBalance() { return balance; }
    public float getStereoFx() { return stereoFx; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
