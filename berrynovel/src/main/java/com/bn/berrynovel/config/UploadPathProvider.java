package com.bn.berrynovel.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Component;

@Component
public class UploadPathProvider {
    private static final String PROJECT_DIRECTORY = "berrynovel";
    private static final Path IMAGE_UPLOAD_DIRECTORY = Paths.get("uploads", "images");

    public Path getImageUploadRoot() {
        Path workingDirectory = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();

        if (isProjectDirectory(workingDirectory)) {
            return workingDirectory.resolve(IMAGE_UPLOAD_DIRECTORY).normalize();
        }

        Path projectDirectory = workingDirectory.resolve(PROJECT_DIRECTORY).normalize();
        if (Files.exists(projectDirectory.resolve("pom.xml")) || Files.isDirectory(projectDirectory.resolve("src"))) {
            return projectDirectory.resolve(IMAGE_UPLOAD_DIRECTORY).normalize();
        }

        return workingDirectory.resolve(IMAGE_UPLOAD_DIRECTORY).normalize();
    }

    private boolean isProjectDirectory(Path path) {
        Path fileName = path.getFileName();
        return fileName != null && PROJECT_DIRECTORY.equalsIgnoreCase(fileName.toString());
    }
}
