package com.legakrishi.solar.controller;

import com.legakrishi.solar.model.Bill;
import com.legakrishi.solar.model.Partner;
import com.legakrishi.solar.repository.BillRepository;
import com.legakrishi.solar.repository.PartnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.jsoup.Jsoup;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/partners")
public class PartnerBillController {

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @GetMapping("/my-bills")
    public String viewAllBillsForPartners(Model model) {
        List<Bill> bills = billRepository.findAllByJmrReportIsNotNull();
        model.addAttribute("bills", bills);
        return "partners/my-bills";
    }
    @GetMapping("/bills/download/{id}")
    public ResponseEntity<byte[]> downloadBillPdf(@PathVariable Long id, Principal principal) {
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        // Optional: Restrict download only to bills which partner should see
        String email = principal.getName();
        Partner partner = partnerRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Partner not found for email: " + email));
        // If you want, check: if (!bill.getPartner().equals(partner)) throw new AccessDeniedException("Not allowed!");

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
        context.setVariable("logoPath", base64Logo);

        String htmlContent = templateEngine.process("admin/bill-print", context); // Or use "partners/bill-print" if you want

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

    @GetMapping("/payment-history")
    public String viewPaymentHistory(Model model, Principal principal) {
        String email = principal.getName();
        Partner partner = partnerRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Partner not found for email: " + email));
        List<Bill> paidBills = billRepository.findAllByPartnerAndStatus(partner, "PAID");
        model.addAttribute("payments", paidBills);
        model.addAttribute("partner", partner);
        return "partners/payment-history";
    }
}