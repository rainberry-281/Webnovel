package com.bn.berrynovel.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfiguration implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadPath = Paths.get(System.getProperty("user.dir"), "uploads", "images")
                .toAbsolutePath()
                .normalize();

        registry.addResourceHandler("/images/**")
                .addResourceLocations(uploadPath.toUri().toString(), "classpath:/static/images/");
    }
}
