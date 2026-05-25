package bai_tap_lon.server.service;

import bai_tap_lon.server.config.ImageConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class LocalImageStorageService implements ImageStorageService {
    private static final long MAX_SIZE = 10L * 1024 * 1024;

    @Override
    public String save(byte[] bytes, String originalFilename) {
        // Lưu file vào thư mục uploads
        // Trả về URL để truy cập file

        //validate
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("File is empty");
        }
        if (bytes.length > MAX_SIZE) {
            throw new IllegalArgumentException("File size exceeds limit of 10MB");
        }

        // tao duong dan
        String fileExtension = extractExtension(originalFilename);
        String fileName = UUID.randomUUID() + "." + fileExtension;
        Path targetPath = Paths.get(ImageConfig.getUploadDir() + fileName);

        //relativePath lưu DB
        String relativePath = "/uploads/" + fileName;

        // Lưu file
        try{
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, bytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file" + e.getMessage(), e);
        }
        return relativePath;
    }

    private String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot >= 0) ? filename.substring(dot + 1).toLowerCase() : "jpg";
    }
}
