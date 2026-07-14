package com.poweramp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poweramp.dsp.model.*;
import com.poweramp.repository.EqBandEntity;
import com.poweramp.repository.EqPresetEntity;
import com.poweramp.repository.EqPresetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class EqPresetService {

    private final EqPresetRepository repository;
    private final ObjectMapper objectMapper;

    public EqPresetService(EqPresetRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public List<EqPreset> getAllPresets() {
        return repository.findAll().stream().map(this::toModel).collect(Collectors.toList());
    }

    public Optional<EqPreset> getPreset(long id) {
        return repository.findById(id).map(this::toModel);
    }

    public EqPreset createPreset(EqPreset preset) {
        EqPresetEntity entity = toEntity(preset);
        entity.setCategory("USER");
        EqPresetEntity saved = repository.save(entity);
        return toModel(saved);
    }

    public EqPreset updatePreset(long id, EqPreset preset) {
        EqPresetEntity entity = repository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Preset not found: " + id));
        entity.setName(preset.getName());
        entity.setMode(preset.getMode().name());
        entity.setPreamp(preset.getPreamp());
        entity.setDescription(preset.getDescription());

        entity.getBands().clear();
        List<EqBandEntity> bandEntities = new ArrayList<>();
        for (int i = 0; i < preset.getBands().size(); i++) {
            var band = preset.getBands().get(i);
            EqBandEntity be = new EqBandEntity();
            be.setPreset(entity);
            be.setFrequency(band.getFrequency());
            be.setGain(band.getGain());
            be.setQ(band.getQ());
            be.setType(band.getType().name());
            be.setChannels(band.getChannels());
            be.setLocked(band.isLocked());
            be.setColor(band.getColor());
            be.setBandIndex(i);
            bandEntities.add(be);
        }
        entity.setBands(bandEntities);

        EqPresetEntity saved = repository.save(entity);
        return toModel(saved);
    }

    public void deletePreset(long id) {
        repository.deleteById(id);
    }

    public String exportPresetsToJson() {
        try {
            List<EqPreset> presets = getAllPresets();
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(presets);
        } catch (Exception e) {
            throw new RuntimeException("Failed to export presets", e);
        }
    }

    public List<EqPreset> importPresetsFromJson(String json) {
        try {
            EqPreset[] presets = objectMapper.readValue(json, EqPreset[].class);
            List<EqPreset> imported = new ArrayList<>();
            for (EqPreset preset : presets) {
                imported.add(createPreset(preset));
            }
            return imported;
        } catch (Exception e) {
            throw new RuntimeException("Failed to import presets", e);
        }
    }

    private EqPreset toModel(EqPresetEntity entity) {
        List<EqBand> bands = entity.getBands().stream()
            .sorted(Comparator.comparingInt(EqBandEntity::getBandIndex))
            .map(be -> new EqBand(be.getFrequency(), be.getGain(), be.getQ(),
                FilterType.valueOf(be.getType()), be.getChannels(), be.isLocked(), be.getColor()))
            .collect(Collectors.toList());

        EqPreset preset = new EqPreset(entity.getName(),
            EqMode.valueOf(entity.getMode()),
            entity.getPreamp(), bands,
            EqPreset.PresetCategory.valueOf(entity.getCategory()));
        preset.setId(entity.getId());
        preset.setDescription(entity.getDescription());
        return preset;
    }

    private EqPresetEntity toEntity(EqPreset preset) {
        EqPresetEntity entity = new EqPresetEntity();
        entity.setName(preset.getName());
        entity.setMode(preset.getMode().name());
        entity.setPreamp(preset.getPreamp());
        entity.setDescription(preset.getDescription());
        entity.setCategory(preset.getCategory() != null ? preset.getCategory().name() : "USER");

        List<EqBandEntity> bandEntities = new ArrayList<>();
        for (int i = 0; i < preset.getBands().size(); i++) {
            var band = preset.getBands().get(i);
            EqBandEntity be = new EqBandEntity();
            be.setPreset(entity);
            be.setFrequency(band.getFrequency());
            be.setGain(band.getGain());
            be.setQ(band.getQ());
            be.setType(band.getType().name());
            be.setChannels(band.getChannels());
            be.setLocked(band.isLocked());
            be.setColor(band.getColor());
            be.setBandIndex(i);
            bandEntities.add(be);
        }
        entity.setBands(bandEntities);
        return entity;
    }
}
