package com.poweramp.service;

import com.poweramp.audio.AudioPipeline;
import com.poweramp.dsp.model.EqPreset;
import com.poweramp.dsp.model.ReverbPreset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Service
public class AudioProcessingService {

    private static final Logger log = LoggerFactory.getLogger(AudioProcessingService.class);
    private static final java.util.Map<String, ProcessingJob> jobs = new java.util.concurrent.ConcurrentHashMap<>();

    @Value("${poweramp.audio.temp-output:/tmp/poweramp-output}")
    private String outputDir;

    public record ProcessingJob(String jobId, String inputFile, String outputFile, float progress,
                                 String status, String error) {}

    @Async
    public CompletableFuture<String> startProcessing(String inputFile, EqPreset preset, ReverbPreset reverb,
                                                      Consumer<Float> progressCallback) {
        String jobId = UUID.randomUUID().toString().substring(0, 8);
        String outputFile = outputDir + File.separator + jobId + "_output.wav";

        new File(outputDir).mkdirs();

        ProcessingJob job = new ProcessingJob(jobId, inputFile, outputFile, 0f, "PROCESSING", null);
        jobs.put(jobId, job);

        try {
            AudioPipeline pipeline = new AudioPipeline();

            if (preset != null) {
                pipeline.getDspProcessor().applyPreset(preset);
            }
            if (reverb != null) {
                pipeline.getDspProcessor().getReverbProcessor().applyPreset(reverb);
                pipeline.getDspProcessor().getReverbProcessor().setEnabled(true);
            }

            pipeline.process(inputFile, outputFile, preset, reverb, progress -> {
                jobs.put(jobId, new ProcessingJob(jobId, inputFile, outputFile, progress, "PROCESSING", null));
                if (progressCallback != null) progressCallback.accept(progress);
            });

            jobs.put(jobId, new ProcessingJob(jobId, inputFile, outputFile, 1.0f, "COMPLETED", null));
            return CompletableFuture.completedFuture(jobId);

        } catch (Exception e) {
            log.error("Processing failed", e);
            jobs.put(jobId, new ProcessingJob(jobId, inputFile, outputFile, -1, "FAILED", e.getMessage()));
            return CompletableFuture.failedFuture(e);
        }
    }

    public ProcessingJob getJobStatus(String jobId) {
        return jobs.get(jobId);
    }

    public java.util.Collection<ProcessingJob> getAllJobs() {
        return jobs.values();
    }
}
