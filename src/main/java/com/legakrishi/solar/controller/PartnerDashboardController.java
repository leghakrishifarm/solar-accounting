package com.legakrishi.solar.controller;

import com.legakrishi.solar.model.JmrReport;
import com.legakrishi.solar.model.Partner;
import com.legakrishi.solar.model.User;
import com.legakrishi.solar.repository.*;
import com.legakrishi.solar.service.BillService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.Optional;

@Controller
public class PartnerDashboardController {

    private final UserRepository userRepo;
    private final PartnerRepository partnerRepo;
    private final BillRepository billRepo;
    private final TransactionRepository transRepo;
    private final JmrReportRepository jmrRepo;
    private final BillService billService;

    public PartnerDashboardController(
            UserRepository userRepo,
            PartnerRepository partnerRepo,
            BillRepository billRepo,
            TransactionRepository transRepo,
            JmrReportRepository jmrRepo,
            BillService billService) {
        this.userRepo = userRepo;
        this.partnerRepo = partnerRepo;
        this.billRepo = billRepo;
        this.transRepo = transRepo;
        this.jmrRepo = jmrRepo;
        this.billService = billService;
    }

    @GetMapping("/partners/dashboard")
    @PreAuthorize("hasRole('PARTNER')")
    public String dashboard(Model model, Principal principal) {
        // Logged-in Partner
        String email = principal.getName();
        User user = userRepo.findByEmail(email).orElseThrow();
        Partner partner = user.getPartner();
        model.addAttribute("partner", partner);
        model.addAttribute("page", "dashboard"); // to highlight active menu

        // Partner-specific stats
        model.addAttribute("billCount", billRepo.countByPartner(partner));
        model.addAttribute("totalPaid", billRepo.sumPaidAmountByPartner(partner));
        model.addAttribute("lastBillStatus", billRepo.findLastBillStatusByPartner(partner));

        // Admin-level stats
        model.addAttribute("totalIncomeReceived", billRepo.sumAllPaidAmount());
        model.addAttribute("totalGovernmentDeductions", billRepo.sumAllGovtDeductions());
        model.addAttribute("totalOutgoing", transRepo.sumAllOutgoing());
        model.addAttribute("balanceAmount", billService.calculateBalance());

        // Last JMR Reading
        Optional<JmrReport> lastReadingOpt = jmrRepo.findTopByOrderByReadingDateDesc();
        lastReadingOpt.ifPresent(lastReading -> {
            model.addAttribute("lastReading", lastReading);
            model.addAttribute("mainMeter", lastReading.getMainMeter());
            model.addAttribute("checkMeter", lastReading.getCheckMeter());
        });

        return "partners/dashboard";
    }
}
