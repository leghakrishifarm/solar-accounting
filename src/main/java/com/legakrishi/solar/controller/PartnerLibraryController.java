package com.legakrishi.solar.controller;

import com.legakrishi.solar.model.LibraryDocument;
import com.legakrishi.solar.repository.LibraryDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/partners/library")
@PreAuthorize("hasRole('PARTNER')")
@RequiredArgsConstructor
public class PartnerLibraryController {

    private final LibraryDocumentRepository repo;

    @GetMapping("")
    public String list(Model model, @RequestParam(required = false) String category) {
        List<LibraryDocument> docs = (category == null || category.isBlank())
                ? repo.findAllByOrderByUploadedAtDesc()
                : repo.findByCategoryOrderByUploadedAtDesc(category);
        model.addAttribute("docs", docs);
        model.addAttribute("category", category);
        return "partners/library/list";
    }
}
