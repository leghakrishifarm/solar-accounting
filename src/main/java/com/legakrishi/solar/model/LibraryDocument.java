package com.legakrishi.solar.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "library_documents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LibraryDocument {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)             // Display title
    private String title;

    private String category;            // e.g. "Agreement", "Bill", "Photo", etc.

    @Column(nullable=false, unique=true)
    private String fileName;            // stored filename on disk (UUID.ext)

    @Column(nullable=false)
    private String originalName;        // original uploaded name

    private long sizeBytes;
    private String uploadedBy;          // admin email/name
    private LocalDateTime uploadedAt;
}
