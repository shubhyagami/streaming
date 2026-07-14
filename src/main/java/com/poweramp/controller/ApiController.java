package com.poweramp.controller;

import com.poweramp.dsp.engine.DspProcessor;
import com.poweramp.dsp.engine.FrequencyResponse;
import com.poweramp.dsp.filter.BiquadFilter;
import com.poweramp.dsp.model.EqPreset;
import com.poweramp.dsp.model.ReverbPreset;
import com.poweramp.service.EqPresetService;
import com.poweramp.service.AudioProcessingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final EqPresetService presetService;
    private final AudioProcessingService processingService;
    private final DspProcessor dspProcessor;

    public ApiController(EqPresetService presetService, AudioProcessingService processingService,
                          DspProcessor dspProcessor) {
        this.presetService = presetService;
        this.processingService = processingService;
        this.dspProcessor = dspProcessor;
    }

    @GetMapping("/presets")
    public List<EqPreset> getPresets() {
        return presetService.getAllPresets();
    }

    @GetMapping("/presets/{id}")
    public ResponseEntity<EqPreset> getPreset(@PathVariable long id) {
        return presetService.getPreset(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/presets")
    public EqPreset createPreset(@RequestBody EqPreset preset) {
        return presetService.createPreset(preset);
    }

    @PutMapping("/presets/{id}")
    public EqPreset updatePreset(@PathVariable long id, @RequestBody EqPreset preset) {
        return presetService.updatePreset(id, preset);
    }

    @DeleteMapping("/presets/{id}")
    public ResponseEntity<Void> deletePreset(@PathVariable long id) {
        presetService.deletePreset(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/presets/export")
    public String exportPresets() {
        return presetService.exportPresetsToJson();
    }

    @PostMapping("/presets/import")
    public List<EqPreset> importPresets(@RequestBody String json) {
        return presetService.importPresetsFromJson(json);
    }

    @PostMapping("/frs")
    public Map<String, Object> getFrequencyResponse(@RequestBody FrsRequest request) {
        List<BiquadFilter> filters = new ArrayList<>();

        if (request.gains != null) {
            for (int i = 0; i < request.gains.length; i++) {
                if (request.gains[i] != 0) {
                    float freq = com.poweramp.dsp.engine.GraphicEqualizer.ISO_1_3_OCTAVE_FREQS[i];
                    filters.add(new com.poweramp.dsp.filter.PeakingFilter(
                        request.sampleRate > 0 ? request.sampleRate : 44100,
                        freq, request.gains[i], 0.707f));
                }
            }
        }

        FrequencyResponse.FrsPoint[] points = FrequencyResponse.compute(filters,
            request.sampleRate > 0 ? request.sampleRate : 44100);

        double[] coords = FrequencyResponse.normalizeToCanvas(points, 800, 200, -24, 24);

        Map<String, Object> response = new HashMap<>();
        response.put("points", points);
        response.put("coords", coords);
        return response;
    }

    @GetMapping("/processing/jobs")
    public Collection<AudioProcessingService.ProcessingJob> getJobs() {
        return processingService.getAllJobs();
    }

    @GetMapping("/processing/jobs/{jobId}")
    public ResponseEntity<AudioProcessingService.ProcessingJob> getJob(@PathVariable String jobId) {
        AudioProcessingService.ProcessingJob job = processingService.getJobStatus(jobId);
        if (job == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(job);
    }

    public record FrsRequest(float[] gains, float sampleRate) {}
}
