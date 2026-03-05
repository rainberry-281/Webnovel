package com.bn.berrynovel.service;

import org.springframework.stereotype.Service;
import java.io.File;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

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
        String rootPath = this.servletContext.getRealPath("/resources/images");
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
    // public void deleteImage(String fileName, String target) {
    // String uploadDir = "src/main/resources/" + target + "/";
    // Path path = Paths.get(uploadDir + fileName);
    // try {
    // Files.deleteIfExists(path);
    // System.out.println("Deleted: " + path.toAbsolutePath());
    // } catch (IOException e) {
    // e.printStackTrace();
    // }
    // }
}
