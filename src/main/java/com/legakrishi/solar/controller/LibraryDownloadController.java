package com.legakrishi.solar.controller;

import com.legakrishi.solar.model.LibraryDocument;
import com.legakrishi.solar.repository.LibraryDocumentRepository;
import com.legakrishi.solar.service.LibraryStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class LibraryDownloadController {

    private final LibraryDocumentRepository repo;
    private final LibraryStorageService storage;

    // Anyone logged-in can download (ADMIN or PARTNER)
    @GetMapping("/library/download/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PARTNER')")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws Exception {
        LibraryDocument doc = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid document id"));
        Resource file = storage.loadAsResource(doc.getFileName());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.getOriginalName().replace("\"","") + "\"")
                .body(file);
    }
}
