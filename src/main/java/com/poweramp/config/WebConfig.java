package com.poweramp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("index");
        registry.addViewController("/equalizer").setViewName("index");
        registry.addViewController("/player").setViewName("index");
        registry.addViewController("/presets").setViewName("index");
        registry.addViewController("/processing").setViewName("index");
    }
}
