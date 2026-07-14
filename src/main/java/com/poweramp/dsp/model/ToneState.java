package com.poweramp.dsp.model;

public class ToneState {
    private boolean enabled;
    private float bassGain;
    private float trebleGain;

    public ToneState() {
        this.enabled = false;
        this.bassGain = 0f;
        this.trebleGain = 0f;
    }

    public ToneState(boolean enabled, float bassGain, float trebleGain) {
        this.enabled = enabled;
        this.bassGain = bassGain;
        this.trebleGain = trebleGain;
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public float getBassGain() { return bassGain; }
    public void setBassGain(float bassGain) { this.bassGain = bassGain; }
    public float getTrebleGain() { return trebleGain; }
    public void setTrebleGain(float trebleGain) { this.trebleGain = trebleGain; }
}
