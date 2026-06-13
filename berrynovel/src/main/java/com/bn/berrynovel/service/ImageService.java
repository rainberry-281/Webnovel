package com.bn.berrynovel.service;

import org.springframework.stereotype.Service;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.io.IOException;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.bn.berrynovel.config.UploadPathProvider;
import com.bn.berrynovel.domain.Novel;

@Service
public class ImageService {
    private final UploadPathProvider uploadPathProvider;

    public ImageService(UploadPathProvider uploadPathProvider) {
        this.uploadPathProvider = uploadPathProvider;
    }

    public String handleImage(MultipartFile file, String target) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String finalName = System.currentTimeMillis() + "-" + cleanFileName(file.getOriginalFilename());
        return saveImage(file, target, finalName);
    }

    // Delete the image file from disk when deleting a user or updating an image.
    public boolean deleteImage(String fileName, String target) {
        if (fileName == null || fileName.isBlank()) {
            return false;
        }

        Path targetDirectory = getTargetDirectory(target);
        Path path = targetDirectory.resolve(cleanFileName(fileName)).normalize();
        if (!path.startsWith(targetDirectory)) {
            return false;
        }

        try {
            boolean deleted = Files.deleteIfExists(path);
            System.out.println("Deleted: " + path.toAbsolutePath());
            return deleted;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String handleImageForNovel(MultipartFile file, String target, Novel novel) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String genreName = (novel.getGenres() != null && !novel.getGenres().isEmpty())
                ? novel.getGenres().get(0).getName()
                : "novel";
        String finalName = genreName + novel.getId() + ".jpg";
        return saveImage(file, target, finalName);
    }

    private String saveImage(MultipartFile file, String target, String finalName) {
        Path targetDirectory = getTargetDirectory(target);
        Path destination = targetDirectory.resolve(finalName).normalize();

        if (!destination.startsWith(targetDirectory)) {
            return "";
        }

        try {
            Files.createDirectories(targetDirectory);
            file.transferTo(destination);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }

        return finalName;
    }

    private Path getTargetDirectory(String target) {
        return this.uploadPathProvider.getImageUploadRoot().resolve(target).normalize();
    }

    private String cleanFileName(String fileName) {
        String cleanName = StringUtils.cleanPath(fileName == null ? "image" : fileName);
        if (!StringUtils.hasText(cleanName)) {
            return "image";
        }

        Path path = Paths.get(cleanName).getFileName();
        return path == null ? "image" : path.toString();
    }
}
