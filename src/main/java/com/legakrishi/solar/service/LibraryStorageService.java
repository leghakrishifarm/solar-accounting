package com.legakrishi.solar.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class LibraryStorageService {

    private final Path root;

    public LibraryStorageService(@Value("${library.upload-dir:uploads/library}") String uploadDir) throws IOException {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
    }

    public StoredFile store(MultipartFile file) throws IOException {
        String ext = "";
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        if (dot >= 0) ext = original.substring(dot);
        String stored = UUID.randomUUID().toString().replace("-", "") + ext;

        Path target = this.root.resolve(stored);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        return new StoredFile(stored, original, Files.size(target));
    }

    public Resource loadAsResource(String storedFileName) throws MalformedURLException {
        Path file = this.root.resolve(storedFileName).normalize();
        Resource resource = new UrlResource(file.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new RuntimeException("File not found: " + storedFileName);
        }
        return resource;
    }

    public record StoredFile(String storedName, String originalName, long sizeBytes) {}
}
