package com.poweramp.dsp.model;

public class ReverbPreset {
    private long id;
    private String name;
    private boolean enabled;
    private float param1; // Size
    private float param2; // Filter
    private float param3; // Fade
    private float param4; // Pre-Delay
    private float param5; // Mix
    private float param6; // Pre-Delay Mix
    private float param7; // Damp

    public ReverbPreset() {}

    public ReverbPreset(String name, float size, float filter, float fade, float preDelay,
                        float mix, float preDelayMix, float damp) {
        this.name = name;
        this.param1 = size;
        this.param2 = filter;
        this.param3 = fade;
        this.param4 = preDelay;
        this.param5 = mix;
        this.param6 = preDelayMix;
        this.param7 = damp;
        this.enabled = true;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public float getParam1() { return param1; }
    public void setParam1(float param1) { this.param1 = param1; }
    public float getParam2() { return param2; }
    public void setParam2(float param2) { this.param2 = param2; }
    public float getParam3() { return param3; }
    public void setParam3(float param3) { this.param3 = param3; }
    public float getParam4() { return param4; }
    public void setParam4(float param4) { this.param4 = param4; }
    public float getParam5() { return param5; }
    public void setParam5(float param5) { this.param5 = param5; }
    public float getParam6() { return param6; }
    public void setParam6(float param6) { this.param6 = param6; }
    public float getParam7() { return param7; }
    public void setParam7(float param7) { this.param7 = param7; }
}
