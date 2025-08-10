package com.legakrishi.solar.controller;

import com.legakrishi.solar.model.LibraryDocument;
import com.legakrishi.solar.repository.LibraryDocumentRepository;
import com.legakrishi.solar.service.LibraryStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin/library")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminLibraryController {

    private final LibraryDocumentRepository repo;
    private final LibraryStorageService storage;

    @GetMapping("")
    public String list(Model model, @RequestParam(required = false) String category) {
        List<LibraryDocument> docs = (category == null || category.isBlank())
                ? repo.findAllByOrderByUploadedAtDesc()
                : repo.findByCategoryOrderByUploadedAtDesc(category);
        model.addAttribute("docs", docs);
        model.addAttribute("category", category);
        return "admin/library/list";
    }

    @GetMapping("/new")
    public String form() {
        return "admin/library/form";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam String title,
                         @RequestParam(required = false) String category,
                         @RequestParam("file") MultipartFile file,
                         RedirectAttributes ra) {
        try {
            var stored = storage.store(file);

            LibraryDocument doc = LibraryDocument.builder()
                    .title(title)
                    .category(category)
                    .fileName(stored.storedName())
                    .originalName(stored.originalName())
                    .sizeBytes(stored.sizeBytes())
                    .uploadedBy("ADMIN") // TODO: set actual admin name/email from SecurityContext
                    .uploadedAt(LocalDateTime.now())
                    .build();

            repo.save(doc);
            ra.addFlashAttribute("success", "Document uploaded.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Upload failed: " + e.getMessage());
        }
        return "redirect:/admin/library";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        repo.findById(id).ifPresent(repo::delete);
        ra.addFlashAttribute("success", "Document deleted.");
        return "redirect:/admin/library";
    }
}
