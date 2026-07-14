package com.poweramp.web.dto;

import java.util.List;

public class PresetDto {
    private long id;
    private String name;
    private String mode;
    private float preamp;
    private String category;
    private String description;
    private List<BandDto> bands;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public float getPreamp() { return preamp; }
    public void setPreamp(float preamp) { this.preamp = preamp; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<BandDto> getBands() { return bands; }
    public void setBands(List<BandDto> bands) { this.bands = bands; }
}
