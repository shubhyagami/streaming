package com.poweramp.dsp.model;

public class VolumeState {
    private float balance;
    private float stereoFx;
    private float tempo;
    private boolean tempoEnabled;
    private float masterVolume;
    private boolean monoEnabled;
    private boolean platformFxEnabled;

    public VolumeState() {
        this.balance = 0.5f;
        this.stereoFx = 0f;
        this.tempo = 1.0f;
        this.tempoEnabled = false;
        this.masterVolume = 1.0f;
        this.monoEnabled = false;
        this.platformFxEnabled = false;
    }

    public float getBalance() { return balance; }
    public void setBalance(float balance) { this.balance = balance; }
    public float getStereoFx() { return stereoFx; }
    public void setStereoFx(float stereoFx) { this.stereoFx = stereoFx; }
    public float getTempo() { return tempo; }
    public void setTempo(float tempo) { this.tempo = tempo; }
    public boolean isTempoEnabled() { return tempoEnabled; }
    public void setTempoEnabled(boolean tempoEnabled) { this.tempoEnabled = tempoEnabled; }
    public float getMasterVolume() { return masterVolume; }
    public void setMasterVolume(float masterVolume) { this.masterVolume = masterVolume; }
    public boolean isMonoEnabled() { return monoEnabled; }
    public void setMonoEnabled(boolean monoEnabled) { this.monoEnabled = monoEnabled; }
    public boolean isPlatformFxEnabled() { return platformFxEnabled; }
    public void setPlatformFxEnabled(boolean platformFxEnabled) { this.platformFxEnabled = platformFxEnabled; }
}
