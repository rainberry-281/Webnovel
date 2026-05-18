package com.bn.berrynovel.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfiguration implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path workingDirectory = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Set<String> imageLocations = new LinkedHashSet<>();

        imageLocations.add(workingDirectory.resolve(Paths.get("uploads", "images")).toUri().toString());
        imageLocations.add(workingDirectory.resolve(Paths.get("berrynovel", "uploads", "images")).toUri().toString());
        imageLocations.add("classpath:/static/images/");

        registry.addResourceHandler("/images/**")
                .addResourceLocations(imageLocations.toArray(String[]::new));
    }
}
