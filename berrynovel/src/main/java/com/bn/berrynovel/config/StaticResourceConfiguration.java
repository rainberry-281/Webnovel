package com.bn.berrynovel.config;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfiguration implements WebMvcConfigurer {

    private final UploadPathProvider uploadPathProvider;

    public StaticResourceConfiguration(UploadPathProvider uploadPathProvider) {
        this.uploadPathProvider = uploadPathProvider;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Set<String> imageLocations = new LinkedHashSet<>();

        imageLocations.add(this.uploadPathProvider.getImageUploadRoot().toUri().toString());
        imageLocations.add("classpath:/static/images/");

        registry.addResourceHandler("/images/**")
                .addResourceLocations(imageLocations.toArray(String[]::new));
    }
}
