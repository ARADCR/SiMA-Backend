package com.sima.backend.service.impl;

import com.sima.backend.service.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final String uploadDir = "uploads";

    @Override
    public String storeFile(MultipartFile file, String folder) {
        try {
            // Create target folder if it doesn't exist
            Path folderPath = Paths.get(uploadDir, folder).toAbsolutePath().normalize();
            Files.createDirectories(folderPath);

            // Generate unique filename
            String originalFileName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
            String fileExtension = "";
            int dotIndex = originalFileName.lastIndexOf('.');
            if (dotIndex > 0) {
                fileExtension = originalFileName.substring(dotIndex);
                originalFileName = originalFileName.substring(0, dotIndex);
            }

            // Remove spaces and special chars from original filename for safety
            originalFileName = originalFileName.replaceAll("[^a-zA-Z0-9.-]", "_");
            String fileName = originalFileName + "_" + UUID.randomUUID().toString().substring(0, 8) + fileExtension;

            // Copy file to the target location (Replacing existing file with the same name)
            Path targetLocation = folderPath.resolve(fileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return fileName;
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo guardar el archivo. Intenta de nuevo.", ex);
        }
    }
}
