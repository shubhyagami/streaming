package com.poweramp.dsp.preset;

import com.poweramp.dsp.engine.GraphicEqualizer;
import com.poweramp.dsp.model.EqBand;
import com.poweramp.dsp.model.EqMode;
import com.poweramp.dsp.model.EqPreset;
import java.util.ArrayList;
import java.util.List;

public class BuiltInPresets {

    private BuiltInPresets() {}

    public static List<EqPreset> getAll() {
        return List.of(
            createPreset("Flat", EqMode.GRAPHIC, 0,
                flatBands(), "Flat response, no equalization"),
            createPreset("Bass", EqMode.GRAPHIC, 0,
                bands(5,4,3,2,1,0,-1,-2,-2,-2, -1,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0), "Enhanced low frequencies"),
            createPreset("Bass Extreme", EqMode.GRAPHIC, -2,
                bands(8,7,6,5,4,3,2,1,0,-1, -2,-3,-3,-3,-2,-1,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0), "Extreme bass boost"),
            createPreset("Bass+Treble", EqMode.GRAPHIC, 0,
                bands(4,3,2,1,0,-1,-2,-2,-2,-1, 0,1,2,3,4,5,5,5,4,3, 2,1,0,0,0,0,0,0,0,0,0), "Boosted bass and treble"),
            createPreset("Treble", EqMode.GRAPHIC, 0,
                bands(0,0,0,0,0,0,0,0,0,0, 0,0,0,1,2,3,4,5,6,6, 5,4,3,2,1,0,0,0,0,0,0), "Enhanced high frequencies"),
            createPreset("Classical", EqMode.GRAPHIC, 0,
                bands(3,3,2,2,1,1,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,1,2,3,4,4,3,2,1,0,-1), "Warm sound for classical music"),
            createPreset("Dance", EqMode.GRAPHIC, 0,
                bands(5,5,4,3,2,1,0,-1,-2,-2, -1,0,1,2,3,4,4,3,2,1, 0,0,0,0,0,0,0,0,0,0,0), "Punchy bass for dance music"),
            createPreset("Rock", EqMode.GRAPHIC, 0,
                bands(3,3,2,2,1,0,-1,-2,-2,-1, 0,1,2,3,3,2,1,0,-1,-2, -2,-1,0,1,2,3,3,2,1,0,-1), "Rock music optimization"),
            createPreset("Techno", EqMode.GRAPHIC, 0,
                bands(6,5,4,3,2,1,0,-1,-2,-2, -1,0,1,2,3,4,4,3,2,1, 0,0,0,0,0,0,0,0,0,0,0), "Electronic music optimization"),
            createPreset("Pop", EqMode.GRAPHIC, 0,
                bands(2,2,1,1,0,0,-1,-1,0,0, 1,2,3,3,2,1,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0), "Pop music optimization"),
            createPreset("Soft", EqMode.GRAPHIC, 0,
                bands(0,0,-1,-1,-2,-2,-3,-3,-2,-1, 0,1,2,2,1,0,-1,-1,-2,-2, -2,-1,0,0,0,0,0,0,0,0,0), "Soft, gentle sound"),
            createPreset("Live", EqMode.GRAPHIC, 0,
                bands(0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,2,3,4, 4,3,2,1,0,0,0,0,0,0,0), "Live concert hall effect"),
            createPreset("Middle", EqMode.GRAPHIC, 0,
                bands(-2,-2,-1,0,1,2,3,4,4,3, 2,1,0,-1,-2,-3,-4,-4,-3,-2, -1,0,1,1,0,0,0,0,0,0,0), "Enhanced midrange frequencies"),
            createPreset("Phone Speaker", EqMode.GRAPHIC, -3,
                bands(5,4,3,2,1,0,-1,-2,-2,-1, 0,1,2,3,4,5,5,4,3,2, 1,0,0,0,0,0,0,0,0,0,0), "Optimized for phone speakers"),
            createPreset("Clarity", EqMode.GRAPHIC, 0,
                bands(1,1,0,0,0,0,0,0,0,0, 0,0,0,1,2,3,4,5,5,4, 3,2,1,0,0,0,0,0,0,0,0), "Enhanced clarity and detail"),
            createPreset("Punchy Bass", EqMode.GRAPHIC, 0,
                bands(4,4,3,2,1,0,-1,-1,-2,-2, -1,0,1,2,2,1,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0), "Punchy, tight bass"),
            createPreset("Soft Bass", EqMode.GRAPHIC, 0,
                bands(3,3,2,2,1,1,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0), "Subtle bass enhancement"),
            createPreset("Soft Treble", EqMode.GRAPHIC, 0,
                bands(0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,1,2,2, 1,0,0,0,0,0,0,0,0,0,0), "Subtle treble enhancement"),
            createPreset("Voice", EqMode.GRAPHIC, 0,
                bands(-1,-1,0,0,1,2,3,4,5,5, 4,3,2,1,0,-1,-2,-3,-4,-4, -3,-2,-1,0,0,0,0,0,0,0,0), "Optimized for vocal content"),
            createPreset("Tone Lows", EqMode.GRAPHIC, 0,
                bands(3,3,2,1,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0), "Low frequency emphasis"),
            createPreset("Tone Highs", EqMode.GRAPHIC, 0,
                bands(0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,1,2, 3,3,2,1,0,0,0,0,0,0,0), "High frequency emphasis"),
            createPreset("Tone Lows+Highs", EqMode.GRAPHIC, 0,
                bands(3,3,2,1,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,1, 2,3,3,2,1,0,0,0,0,0,0), "Both low and high emphasis")
        );
    }

    private static EqPreset createPreset(String name, EqMode mode, float preamp, float[] gains, String desc) {
        List<EqBand> bands = new ArrayList<>();
        for (int i = 0; i < 31; i++) {
            float freq = GraphicEqualizer.ISO_1_3_OCTAVE_FREQS[i];
            float gain = i < gains.length ? gains[i] : 0;
            bands.add(new EqBand(freq, gain, 0.707f));
        }
        EqPreset preset = new EqPreset(name, mode, preamp, bands, EqPreset.PresetCategory.BUILT_IN);
        preset.setDescription(desc);
        return preset;
    }

    private static float[] flatBands() {
        return new float[31];
    }

    private static float[] bands(float... gains) {
        return gains;
    }
}
