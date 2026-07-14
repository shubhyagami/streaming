package com.poweramp.dsp.model;

public class EqBand {
    private float frequency;
    private float gain;
    private float q;
    private FilterType type;
    private int channels;
    private boolean locked;
    private int color;

    public EqBand() {}

    public EqBand(float frequency, float gain, float q) {
        this(frequency, gain, q, FilterType.PEAKING, 3, false, 0);
    }

    public EqBand(float frequency, float gain, float q, FilterType type) {
        this(frequency, gain, q, type, 3, false, 0);
    }

    public EqBand(float frequency, float gain, float q, FilterType type, int channels, boolean locked, int color) {
        this.frequency = frequency;
        this.gain = gain;
        this.q = q;
        this.type = type;
        this.channels = channels;
        this.locked = locked;
        this.color = color;
    }

    public float getFrequency() { return frequency; }
    public void setFrequency(float frequency) { this.frequency = frequency; }
    public float getGain() { return gain; }
    public void setGain(float gain) { this.gain = gain; }
    public float getQ() { return q; }
    public void setQ(float q) { this.q = q; }
    public FilterType getType() { return type; }
    public void setType(FilterType type) { this.type = type; }
    public int getChannels() { return channels; }
    public void setChannels(int channels) { this.channels = channels; }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }

    public EqBand copy() {
        return new EqBand(frequency, gain, q, type, channels, locked, color);
    }
}
