package com.poweramp.config;

import com.poweramp.dsp.model.EqPreset;
import com.poweramp.dsp.preset.BuiltInPresets;
import com.poweramp.repository.EqBandEntity;
import com.poweramp.repository.EqPresetEntity;
import com.poweramp.repository.EqPresetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final EqPresetRepository presetRepository;

    public DataInitializer(EqPresetRepository presetRepository) {
        this.presetRepository = presetRepository;
    }

    @Override
    public void run(String... args) {
        if (presetRepository.count() > 0) {
            log.info("Presets already initialized, skipping seed");
            return;
        }

        log.info("Seeding built-in EQ presets...");
        List<EqPreset> builtIn = BuiltInPresets.getAll();
        List<EqPresetEntity> entities = new ArrayList<>();

        for (EqPreset preset : builtIn) {
            EqPresetEntity entity = new EqPresetEntity();
            entity.setName(preset.getName());
            entity.setMode(preset.getMode().name());
            entity.setPreamp(preset.getPreamp());
            entity.setCategory(preset.getCategory().name());
            entity.setDescription(preset.getDescription());

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
            entities.add(entity);
        }

        presetRepository.saveAll(entities);
        log.info("Seeded {} built-in EQ presets", entities.size());
    }
}
