package com.poweramp.repository;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "eq_presets")
public class EqPresetEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String mode;

    private float preamp;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private String category;

    @OneToMany(mappedBy = "preset", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderColumn(name = "band_order")
    private List<EqBandEntity> bands = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    public float getPreamp() { return preamp; }
    public void setPreamp(float preamp) { this.preamp = preamp; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public List<EqBandEntity> getBands() { return bands; }
    public void setBands(List<EqBandEntity> bands) { this.bands = bands; }
}
