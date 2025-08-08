package com.legakrishi.solar.controller;

import com.legakrishi.solar.model.Bill;
import com.legakrishi.solar.model.JmrReport;
import com.legakrishi.solar.repository.BillRepository;
import com.legakrishi.solar.repository.JmrReportRepository;
import com.legakrishi.solar.repository.PartnerRepository;
import com.legakrishi.solar.service.BillService;
import com.legakrishi.solar.service.IncomeService;
import jakarta.validation.Valid;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/admin/bills")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBillController {

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private JmrReportRepository jmrReportRepository;

    @Autowired
    private BillService billService;

    @Autowired
    private IncomeService incomeService;

    @Autowired
    private SpringTemplateEngine templateEngine;

    /** 1. List all bills */
    @GetMapping("")
    public String listBills(Model model) {
        List<Bill> bills = billRepository.findAll();
        bills.removeIf(Objects::isNull);
        model.addAttribute("bills", bills);
        return "admin/bills-list";
    }

    /** 2. Add bill */
    @GetMapping("/add")
    public String showAddBillForm(Model model) {
        model.addAttribute("bill", new Bill());
        model.addAttribute("partners", partnerRepository.findAll());
        return "admin/bill-form";
    }

    /** 3. Edit bill */
    @GetMapping("/edit/{id}")
    public String showEditBillForm(@PathVariable Long id, Model model) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found"));
        model.addAttribute("bill", bill);
        model.addAttribute("partners", partnerRepository.findAll());
        return "admin/bill-form";
    }

    /** 4. Save bill */
    @PostMapping("/save")
    public String saveBill(@ModelAttribute @Valid Bill bill, BindingResult result, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("partners", partnerRepository.findAll());
            return "admin/bill-form";
        }
        if (bill.getBillDate() == null) {
            bill.setBillDate(LocalDate.now());
        }
        billRepository.save(bill);
        return "redirect:/admin/bills";
    }

    /** 5. Generate bills from JMR */
    @GetMapping("/generate")
    public String generateBills(RedirectAttributes redirectAttributes) {
        List<JmrReport> approvedJmrs = jmrReportRepository.findByStatus("APPROVED");
        int generatedCount = 0;
        for (JmrReport jmr : approvedJmrs) {
            if (!billRepository.existsByJmrReport(jmr)) {
                billService.generateBillFromJmr(jmr);
                generatedCount++;
            }
        }
        redirectAttributes.addFlashAttribute("success", generatedCount + " bill(s) generated successfully.");
        return "redirect:/admin/bills";
    }

    /** 6. Print bill HTML */
    @GetMapping("/print/{id}")
    public String printBill(@PathVariable Long id, Model model) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        // Convert logo to Base64 for printing too
        File logoFile = new File("src/main/resources/static/img/legha_logo.png");
        String base64Logo = "";
        try {
            byte[] imageBytes = Files.readAllBytes(logoFile.toPath());
            base64Logo = "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }

        model.addAttribute("bill", bill);
        model.addAttribute("logoPath", base64Logo); // Base64 for both PDF and print

        return "admin/bill-print";
    }

    /** 7. Payment form */
    @GetMapping("/payment/{id}")
    public String showPaymentForm(@PathVariable Long id, Model model) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found"));
        model.addAttribute("bill", bill);
        return "admin/bill-payment-form";
    }

    /** 8. Save payment */
    @PostMapping("/payment/save")
    public String savePaymentDetails(@ModelAttribute Bill bill, RedirectAttributes redirectAttributes) {
        Bill existingBill = billRepository.findById(bill.getId())
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        existingBill.setPaymentDate(bill.getPaymentDate());
        existingBill.setActualAmountReceived(bill.getActualAmountReceived());
        existingBill.setGovernmentDeductions(bill.getGovernmentDeductions());
        existingBill.setStatus("PAID");

        billRepository.save(existingBill);
        incomeService.updateIncomeAfterPayment(existingBill);

        redirectAttributes.addFlashAttribute("success", "Payment details updated and income recalculated.");
        return "redirect:/admin/bills";
    }

    /** 9. Download PDF (with logo, single page) */
    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadBillPdf(@PathVariable Long id) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        // Convert logo to Base64
        File logoFile = new File("src/main/resources/static/img/legha_logo.png");
        String base64Logo = "";
        try {
            byte[] imageBytes = Files.readAllBytes(logoFile.toPath());
            base64Logo = "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Thymeleaf context
        Context context = new Context();
        context.setVariable("bill", bill);
        context.setVariable("logoPath", base64Logo); // Base64 instead of file:// path

        String htmlContent = templateEngine.process("admin/bill-print", context);

        // Convert to XHTML
        org.jsoup.nodes.Document document = Jsoup.parse(htmlContent, "UTF-8");
        document.outputSettings().syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml);
        String xhtml = document.html();

        // Generate PDF
        ByteArrayOutputStream pdfStream = new ByteArrayOutputStream();
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.withHtmlContent(xhtml, new File("src/main/resources/static/").toURI().toString());
        builder.toStream(pdfStream);

        try {
            builder.run();
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed: " + e.getMessage());
        }

        byte[] pdfBytes = pdfStream.toByteArray();
        String fileName = "Invoice_" + bill.getInvoiceNumber() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
    // DELETE BILL (ID sent in body)
    @PostMapping("/delete")
    public String deleteBill(@RequestParam("id") Long id, RedirectAttributes redirectAttributes) {
        Bill bill = billRepository.findById(id).orElse(null);
        if (bill == null) {
            redirectAttributes.addFlashAttribute("error", "Bill not found.");
            return "redirect:/admin/bills";
        }

        // Safety: do not delete paid bills (avoids income mismatch)
        if ("PAID".equalsIgnoreCase(bill.getStatus())) {
            redirectAttributes.addFlashAttribute("error", "Cannot delete: this bill is marked PAID.");
            return "redirect:/admin/bills";
        }

        billRepository.delete(bill);
        redirectAttributes.addFlashAttribute("success", "Bill deleted successfully.");
        return "redirect:/admin/bills";
    }
}
