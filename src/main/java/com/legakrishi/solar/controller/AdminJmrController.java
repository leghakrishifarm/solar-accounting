package com.legakrishi.solar.controller;

import com.legakrishi.solar.model.JmrReport;
import com.legakrishi.solar.repository.BillRepository;
import com.legakrishi.solar.repository.JmrReportRepository;
import com.legakrishi.solar.service.JmrPdfService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin/jmr_reports")
@PreAuthorize("hasRole('ADMIN')")
public class AdminJmrController {

    @Autowired private JmrReportRepository jmrReportRepository;
    @Autowired private JmrPdfService jmrPdfService;
    @Autowired private BillRepository billRepository;

    @Value("${jmr.pdf.upload-dir:uploads/approved_jmr_pdfs}")
    private String pdfUploadDir;

    // 1. List all reports
    @GetMapping("")
    public String listJmrReports(Model model, @ModelAttribute("error") String error,
                                 @ModelAttribute("success") String success) {
        List<JmrReport> reports = jmrReportRepository.findAll();
        model.addAttribute("reports", reports);
        if (error != null && !error.isEmpty()) model.addAttribute("error", error);
        if (success != null && !success.isEmpty()) model.addAttribute("success", success);
        return "admin/jmr-list";
    }

    // 2. Show add form
    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("jmrReport", new JmrReport());
        return "admin/jmr_form";
    }

    // 3. Save new JMR
    @PostMapping("/add")
    public String saveJmr(@ModelAttribute("jmrReport") JmrReport jmrReport) {
        if (jmrReport.getReadingDate() == null) jmrReport.setReadingDate(LocalDate.now());
        jmrReport.setStatus("DRAFT");
        jmrReportRepository.save(jmrReport);
        return "redirect:/admin/jmr_reports";
    }

    // 4. Download System Generated PDF
    @GetMapping("/pdf/{id}")
    public void downloadJmrPdf(@PathVariable Long id, HttpServletResponse response) throws IOException {
        JmrReport jmr = jmrReportRepository.findById(id).orElseThrow();
        byte[] pdfBytes = jmrPdfService.generateJmrPdf(jmr);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=JMR_" + jmr.getMonth() + "_" + jmr.getYear() + ".pdf");
        response.getOutputStream().write(pdfBytes);
    }

    // 5. UPLOAD Approved/Signed PDF
    @PostMapping("/upload_pdf/{id}")
    public String uploadApprovedPdf(@PathVariable Long id,
                                    @RequestParam("pdfFile") MultipartFile file,
                                    RedirectAttributes redirectAttributes) {
        JmrReport jmr = jmrReportRepository.findById(id).orElseThrow();
        if (file.isEmpty() || !file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            redirectAttributes.addFlashAttribute("error", "Only PDF files are allowed.");
            return "redirect:/admin/jmr_reports";
        }
        try {
            File uploadDir = new File(pdfUploadDir);
            if (!uploadDir.exists()) uploadDir.mkdirs();

            String fileName = "APPROVED_JMR_" + id + "_" + System.currentTimeMillis() + ".pdf";
            Path dest = Paths.get(pdfUploadDir, fileName);
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);

            jmr.setApprovedPdfPath(dest.toString().replace("\\", "/"));
            jmrReportRepository.save(jmr);

            redirectAttributes.addFlashAttribute("success", "Approved PDF uploaded.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Upload failed: " + ex.getMessage());
        }
        return "redirect:/admin/jmr_reports";
    }

    // 6. VIEW/Download Approved PDF
    @GetMapping("/approved_pdf/{id}")
    public void serveApprovedPdf(@PathVariable Long id, HttpServletResponse response) throws IOException {
        JmrReport jmr = jmrReportRepository.findById(id).orElseThrow();
        String path = jmr.getApprovedPdfPath();
        if (path == null || path.isEmpty() || !Files.exists(Paths.get(path))) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No approved PDF uploaded.");
            return;
        }
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "inline; filename=Approved_JMR_" + id + ".pdf");
        Files.copy(Paths.get(path), response.getOutputStream());
    }

    // 7. Graceful error for file too large
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxSizeException(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", "File size too large. Max allowed is 20MB.");
        return "redirect:/admin/jmr_reports";
    }

    // APPROVE JMR
    @PostMapping("/approve/{id}")
    public String approveJmr(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        JmrReport jmr = jmrReportRepository.findById(id).orElseThrow();
        jmr.setStatus("APPROVED");
        jmrReportRepository.save(jmr);
        redirectAttributes.addFlashAttribute("success", "JMR approved successfully.");
        return "redirect:/admin/jmr_reports";
    }

    // REJECT JMR
    @PostMapping("/reject/{id}")
    public String rejectJmr(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        JmrReport jmr = jmrReportRepository.findById(id).orElseThrow();
        jmr.setStatus("REJECTED");
        jmrReportRepository.save(jmr);
        redirectAttributes.addFlashAttribute("success", "JMR rejected.");
        return "redirect:/admin/jmr_reports";
    }

    // DELETE JMR (ID sent in body). Requires Bill to be deleted first.
    @PostMapping("/delete")
    public String deleteJmr(@RequestParam("id") Long id, RedirectAttributes redirectAttributes) {
        JmrReport jmr = jmrReportRepository.findById(id).orElse(null);
        if (jmr == null) {
            redirectAttributes.addFlashAttribute("error", "JMR not found.");
            return "redirect:/admin/jmr_reports";
        }

        // prevent accidental orphaning – ask user to delete Bill first
        if (billRepository.existsByJmrReport(jmr)) {
            redirectAttributes.addFlashAttribute("error", "Please delete the related Bill first, then delete this JMR.");
            return "redirect:/admin/jmr_reports";
        }

        // remove approved PDF (non-fatal on failure)
        try {
            String p = jmr.getApprovedPdfPath();
            if (p != null && !p.isBlank()) {
                Files.deleteIfExists(Paths.get(p));
            }
        } catch (IOException ignore) {
            // do not block deletion
        }

        jmrReportRepository.delete(jmr);
        redirectAttributes.addFlashAttribute("success", "JMR deleted successfully.");
        return "redirect:/admin/jmr_reports";
    }
}
