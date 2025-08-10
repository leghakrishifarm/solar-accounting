package com.legakrishi.solar.repository;

import com.legakrishi.solar.model.LibraryDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LibraryDocumentRepository extends JpaRepository<LibraryDocument, Long> {
    List<LibraryDocument> findAllByOrderByUploadedAtDesc();
    List<LibraryDocument> findByCategoryOrderByUploadedAtDesc(String category);
}
