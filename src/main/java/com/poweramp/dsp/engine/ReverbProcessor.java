package com.poweramp.dsp.engine;

import com.poweramp.dsp.model.ReverbPreset;
import java.util.Arrays;

public class ReverbProcessor {
    private boolean enabled;
    private float mix;
    private float[] combDelay;
    private float[] allpassDelay;
    private int[] combDelayIdx;
    private int[] allpassDelayIdx;

    private static final int[] COMB_LENGTHS = {1557, 1617, 1491, 1422, 1277, 1356, 1188, 1116};
    private static final int[] ALLPASS_LENGTHS = {225, 341, 441, 556};
    private static final float[] COMB_GAINS = {0.84f, 0.84f, 0.83f, 0.83f, 0.84f, 0.84f, 0.83f, 0.83f};
    private static final float ALLPASS_GAIN = 0.5f;
    private static final float SCALE = 0.015f;

    public ReverbProcessor() {
        this.enabled = false;
        this.mix = 0.3f;
        this.combDelay = new float[COMB_LENGTHS.length];
        this.allpassDelay = new float[ALLPASS_LENGTHS.length];
        this.combDelayIdx = new int[COMB_LENGTHS.length];
        this.allpassDelayIdx = new int[ALLPASS_LENGTHS.length];
    }

    public void process(float[] samples, int offset, int count) {
        if (!enabled || mix <= 0) return;

        for (int i = 0; i < count; i++) {
            int idx = offset + i;
            float input = samples[idx];
            float wet = 0;

            for (int j = 0; j < COMB_LENGTHS.length; j++) {
                int delayLen = (int) (COMB_LENGTHS[j]);
                combDelayIdx[j] = (combDelayIdx[j] + 1) % delayLen;
                float delayed = combDelay[j];
                combDelay[j] = input + delayed * COMB_GAINS[j];
                wet += delayed;
            }

            for (int j = 0; j < ALLPASS_LENGTHS.length; j++) {
                int delayLen = (int) (ALLPASS_LENGTHS[j]);
                allpassDelayIdx[j] = (allpassDelayIdx[j] + 1) % delayLen;
                float delayed = allpassDelay[j];
                allpassDelay[j] = wet + delayed * ALLPASS_GAIN;
                wet = delayed - wet * ALLPASS_GAIN;
            }

            samples[idx] = input * (1.0f - mix) + wet * SCALE * mix;
        }
    }

    public void applyPreset(ReverbPreset preset) {
        if (preset == null) return;
        this.mix = preset.getParam5() / 100f;
        this.enabled = preset.isEnabled();
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public float getMix() { return mix; }
    public void setMix(float mix) { this.mix = Math.max(0, Math.min(1, mix)); }
}
