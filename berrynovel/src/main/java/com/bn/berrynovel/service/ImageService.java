package com.bn.berrynovel.service;

import org.springframework.stereotype.Service;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;
import com.bn.berrynovel.domain.Novel;

import jakarta.servlet.ServletContext;

@Service
public class ImageService {
    private final ServletContext servletContext;

    public ImageService(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public String handleImage(MultipartFile file, String target) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String finalName = "";
        String rootPath = System.getProperty("user.dir") + "/src/main/resources/static/images";
        byte[] bytes;
        try {
            bytes = file.getBytes();

            // Dẫn đến lưu file ảnh
            File dir = new File(rootPath + File.separator + target);
            if (!dir.exists())
                dir.mkdirs();

            // Lấy tên file ảnh và path của file ảnh
            finalName = System.currentTimeMillis() + "-" + file.getOriginalFilename();
            File serverFile = new File(dir.getAbsolutePath() + File.separator + finalName);

            BufferedOutputStream stream = new BufferedOutputStream(
                    new FileOutputStream(serverFile));
            stream.write(bytes);
            stream.close();

        } catch (IOException e) {

            e.printStackTrace();
        }

        return finalName;
    }

    // Phục vụ cho việc xóa người dùng hoặc cập nhật ảnh thì xóa luôn file ảnh trên
    // ổ cứng
    public void deleteImage(String fileName, String target) {
        String uploadDir = System.getProperty("user.dir") + "/src/main/resources/static/images/" + target + "/";
        Path path = Paths.get(uploadDir + fileName);
        try {
            Files.deleteIfExists(path);
            System.out.println("Deleted: " + path.toAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String handleImageForNovel(MultipartFile file, String target, Novel novel) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String finalName = "";
        String rootPath = System.getProperty("user.dir") + "/src/main/resources/static/images";
        byte[] bytes;
        try {
            bytes = file.getBytes();

            // Dẫn đến lưu file ảnh
            File dir = new File(rootPath + File.separator + target);
            if (!dir.exists())
                dir.mkdirs();

            // Lấy tên file ảnh và path của file ảnh
            String genreName = (novel.getGenres() != null && !novel.getGenres().isEmpty())
                    ? novel.getGenres().get(0).getName()
                    : "novel";
            finalName = genreName + novel.getId() + ".jpg";
            File serverFile = new File(dir.getAbsolutePath() + File.separator + finalName);

            BufferedOutputStream stream = new BufferedOutputStream(
                    new FileOutputStream(serverFile));
            stream.write(bytes);
            stream.close();

        } catch (IOException e) {

            e.printStackTrace();
        }

        return finalName;
    }
}
