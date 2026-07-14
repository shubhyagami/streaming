package com.poweramp.web.dto;

public class BandDto {
    private float frequency;
    private float gain;
    private float q;
    private String type;
    private int channels;
    private boolean locked;
    private int color;

    public float getFrequency() { return frequency; }
    public void setFrequency(float frequency) { this.frequency = frequency; }
    public float getGain() { return gain; }
    public void setGain(float gain) { this.gain = gain; }
    public float getQ() { return q; }
    public void setQ(float q) { this.q = q; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public int getChannels() { return channels; }
    public void setChannels(int channels) { this.channels = channels; }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
}
