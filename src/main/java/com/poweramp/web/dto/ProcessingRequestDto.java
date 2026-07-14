package com.poweramp.web.dto;

import java.util.List;

public class ProcessingRequestDto {
    private List<String> files;
    private Long presetId;
    private Long reverbPresetId;
    private boolean enableReverb;
    private boolean enableLimiter;
    private float masterVolume;

    public List<String> getFiles() { return files; }
    public void setFiles(List<String> files) { this.files = files; }
    public Long getPresetId() { return presetId; }
    public void setPresetId(Long presetId) { this.presetId = presetId; }
    public Long getReverbPresetId() { return reverbPresetId; }
    public void setReverbPresetId(Long reverbPresetId) { this.reverbPresetId = reverbPresetId; }
    public boolean isEnableReverb() { return enableReverb; }
    public void setEnableReverb(boolean enableReverb) { this.enableReverb = enableReverb; }
    public boolean isEnableLimiter() { return enableLimiter; }
    public void setEnableLimiter(boolean enableLimiter) { this.enableLimiter = enableLimiter; }
    public float getMasterVolume() { return masterVolume; }
    public void setMasterVolume(float masterVolume) { this.masterVolume = masterVolume; }
}
