package com.poweramp.dsp.model;

import java.util.ArrayList;
import java.util.List;

public class EqPreset {
    private long id;
    private String name;
    private EqMode mode;
    private float preamp;
    private List<EqBand> bands;
    private PresetCategory category;
    private String description;

    public enum PresetCategory {
        BUILT_IN,
        USER,
        AUTO_EQ
    }

    public EqPreset() {
        this.bands = new ArrayList<>();
        this.mode = EqMode.GRAPHIC;
        this.category = PresetCategory.USER;
        this.preamp = 0f;
    }

    public EqPreset(String name, EqMode mode, float preamp, List<EqBand> bands, PresetCategory category) {
        this.name = name;
        this.mode = mode;
        this.preamp = preamp;
        this.bands = bands;
        this.category = category;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public EqMode getMode() { return mode; }
    public void setMode(EqMode mode) { this.mode = mode; }
    public float getPreamp() { return preamp; }
    public void setPreamp(float preamp) { this.preamp = preamp; }
    public List<EqBand> getBands() { return bands; }
    public void setBands(List<EqBand> bands) { this.bands = bands; }
    public PresetCategory getCategory() { return category; }
    public void setCategory(PresetCategory category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
