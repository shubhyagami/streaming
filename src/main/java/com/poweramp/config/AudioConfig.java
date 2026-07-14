package com.poweramp.config;

import com.poweramp.dsp.engine.DspProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AudioConfig {

    @Bean
    public DspProcessor dspProcessor() {
        return new DspProcessor(44100);
    }
}
