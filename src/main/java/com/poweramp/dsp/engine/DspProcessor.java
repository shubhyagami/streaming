package com.poweramp.dsp.engine;

import com.poweramp.dsp.filter.BiquadFilter;
import com.poweramp.dsp.model.*;
import java.util.ArrayList;
import java.util.List;

public class DspProcessor {
    private final GraphicEqualizer graphicEq;
    private final ParametricEqualizer parametricEq;
    private final ToneController toneController;
    private final Limiter limiter;
    private final StereoProcessor stereoProcessor;
    private final TempoProcessor tempoProcessor;
    private final ReverbProcessor reverbProcessor;
    private final float sampleRate;
    private EqMode mode;
    private float masterVolume;

    public DspProcessor(float sampleRate) {
        this.sampleRate = sampleRate;
        this.graphicEq = new GraphicEqualizer(sampleRate);
        this.parametricEq = new ParametricEqualizer(sampleRate);
        this.toneController = new ToneController(sampleRate);
        this.limiter = new Limiter();
        this.stereoProcessor = new StereoProcessor();
        this.tempoProcessor = new TempoProcessor((int) sampleRate);
        this.reverbProcessor = new ReverbProcessor();
        this.mode = EqMode.GRAPHIC;
        this.masterVolume = 1.0f;
    }

    public void applyPreset(EqPreset preset) {
        if (preset == null) return;
        mode = preset.getMode();

        if (mode == EqMode.GRAPHIC) {
            float[] gains = new float[GraphicEqualizer.ISO_1_3_OCTAVE_FREQS.length];
            for (int i = 0; i < gains.length && i < preset.getBands().size(); i++) {
                gains[i] = preset.getBands().get(i).getGain();
            }
            graphicEq.setGains(gains);
        } else {
            parametricEq.setBands(preset.getBands());
        }

        graphicEq.setPreamp(preset.getPreamp());
        parametricEq.setPreamp(preset.getPreamp());
    }

    public float[] process(float[] samples, int offset, int count, int channels) {
        if (channels == 2) {
            return processStereo(samples, offset, count);
        } else {
            return processMono(samples, offset, count);
        }
    }

    private float[] processMono(float[] samples, int offset, int count) {
        if (mode == EqMode.GRAPHIC) {
            graphicEq.process(samples, offset, count);
        } else {
            parametricEq.process(samples, offset, count);
        }
        toneController.process(samples, offset, count);
        limiter.process(samples, offset, count);
        return samples;
    }

    private float[] processStereo(float[] samples, int offset, int count) {
        int sampleCount = count / 2;

        if (mode == EqMode.GRAPHIC) {
            graphicEq.processInterleaved(samples, offset, sampleCount, 2, 0);
            graphicEq.processInterleaved(samples, offset, sampleCount, 2, 1);
        } else {
            parametricEq.process(samples, offset, count);
        }

        toneController.process(samples, offset, count);
        stereoProcessor.process(samples, offset, sampleCount);
        limiter.process(samples, offset, count);

        if (masterVolume != 1.0f) {
            for (int i = 0; i < count; i++) {
                samples[offset + i] *= masterVolume;
            }
        }

        if (reverbProcessor.isEnabled()) {
            reverbProcessor.process(samples, offset, count);
        }

        if (tempoProcessor.isEnabled()) {
            samples = tempoProcessor.process(samples, offset, count);
        }

        return samples;
    }

    public List<BiquadFilter> getActiveEqFilters() {
        List<BiquadFilter> filters = new ArrayList<>();
        if (mode == EqMode.GRAPHIC && graphicEq.isEnabled()) {
            filters.addAll(graphicEq.getFilters());
        } else if (mode == EqMode.PARAMETRIC && parametricEq.isEnabled()) {
            filters.addAll(parametricEq.getFilters());
        }
        return filters;
    }

    public List<BiquadFilter> getActiveToneFilters() {
        List<BiquadFilter> filters = new ArrayList<>();
        if (toneController.isEnabled()) {
            if (toneController.getBassFilter() != null) filters.add(toneController.getBassFilter());
            if (toneController.getTrebleFilter() != null) filters.add(toneController.getTrebleFilter());
        }
        return filters;
    }

    public GraphicEqualizer getGraphicEq() { return graphicEq; }
    public ParametricEqualizer getParametricEq() { return parametricEq; }
    public ToneController getToneController() { return toneController; }
    public Limiter getLimiter() { return limiter; }
    public StereoProcessor getStereoProcessor() { return stereoProcessor; }
    public TempoProcessor getTempoProcessor() { return tempoProcessor; }
    public ReverbProcessor getReverbProcessor() { return reverbProcessor; }
    public float getMasterVolume() { return masterVolume; }
    public void setMasterVolume(float masterVolume) { this.masterVolume = masterVolume; }
    public EqMode getMode() { return mode; }
}
