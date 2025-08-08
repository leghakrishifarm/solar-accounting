package com.legakrishi.solar.controller;

import com.legakrishi.solar.model.JmrReport;
import com.legakrishi.solar.model.Partner;
import com.legakrishi.solar.repository.JmrReportRepository;
import com.legakrishi.solar.repository.PartnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/partners")
public class PartnerJmrController {

    @Autowired
    private JmrReportRepository jmrReportRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @GetMapping("/jmr-reports")
    public String viewPartnerJmrReports(Model model, Principal principal) {
        String email = principal.getName();
        Partner partner = partnerRepository.findByUserEmail(email)
                .orElseThrow(() -> new RuntimeException("Partner not found for email: " + email));
        // You may want to filter JMR reports by plant/partner if required
        List<JmrReport> reports = jmrReportRepository.findAll(); // Or filter as needed

        model.addAttribute("reports", reports);
        return "partners/jmr-reports";
    }
}
