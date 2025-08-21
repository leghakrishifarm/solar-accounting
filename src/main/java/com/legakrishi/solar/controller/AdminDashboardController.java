package com.legakrishi.solar.controller;

import com.legakrishi.solar.model.JmrReport;
import com.legakrishi.solar.model.Transaction;
import com.legakrishi.solar.model.User;
import com.legakrishi.solar.model.Site;
import com.legakrishi.solar.repository.BillRepository;
import com.legakrishi.solar.repository.JmrReportRepository;
import com.legakrishi.solar.repository.PartnerRepository;
import com.legakrishi.solar.repository.SiteRepository;
import com.legakrishi.solar.repository.TransactionRepository;
import com.legakrishi.solar.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final PartnerRepository partnerRepo;
    private final TransactionRepository transRepo;
    private final BillRepository billRepo;
    private final UserRepository userRepo;
    private final JmrReportRepository jmrReportRepo;
    private final SiteRepository siteRepo;

    public AdminDashboardController(PartnerRepository partnerRepo,
                                    TransactionRepository transRepo,
                                    BillRepository billRepo,
                                    UserRepository userRepo,
                                    JmrReportRepository jmrReportRepo,
                                    SiteRepository siteRepo) {
        this.partnerRepo = partnerRepo;
        this.transRepo = transRepo;
        this.billRepo = billRepo;
        this.userRepo = userRepo;
        this.jmrReportRepo = jmrReportRepo;
        this.siteRepo = siteRepo;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // --- Current user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String loggedInEmail = (authentication != null) ? authentication.getName() : null;

        Optional<User> userOpt = (loggedInEmail != null) ? userRepo.findByEmail(loggedInEmail) : Optional.empty();
        if (userOpt.isEmpty()) {
            // If you prefer redirect: return "redirect:/login";
            throw new IllegalStateException("Logged-in user not found: " + loggedInEmail);
        }
        User user = userOpt.get();
        model.addAttribute("user", user);

        // --- High-level counts and balances
        long partnerCount = partnerRepo.count();
        model.addAttribute("partnerCount", partnerCount);

        Double totalIncomeReceived     = billRepo.sumActualAmountReceivedByStatus("PAID");
        Double totalGovDeductions      = billRepo.sumGovernmentDeductionsByStatus("PAID");
        Double totalOutgoing           = transRepo.sumAmountByType(Transaction.TransactionType.OUTGOING);
        Double otherIncome             = transRepo.sumAmountByType(Transaction.TransactionType.OTHER_INCOME);

        // Null-safety
        totalIncomeReceived = totalIncomeReceived != null ? totalIncomeReceived : 0.0;
        totalGovDeductions  = totalGovDeductions  != null ? totalGovDeductions  : 0.0;
        totalOutgoing       = totalOutgoing       != null ? totalOutgoing       : 0.0;
        otherIncome         = otherIncome         != null ? otherIncome         : 0.0;

        double totalIncome   = totalIncomeReceived + otherIncome;
        double balanceAmount = totalIncome - totalGovDeductions - totalOutgoing;

        model.addAttribute("totalIncomeReceived", totalIncomeReceived);
        model.addAttribute("totalGovernmentDeductions", totalGovDeductions);
        model.addAttribute("totalOutgoing", totalOutgoing);
        model.addAttribute("otherIncome", otherIncome);
        model.addAttribute("totalIncome", totalIncome);
        model.addAttribute("balanceAmount", balanceAmount);

        // --- Lists / tables
        model.addAttribute("partners", partnerRepo.findAll());
        model.addAttribute("pendingBills", billRepo.findByStatus("PENDING"));

        // --- Last JMR reading (for quick glance meters)
        Optional<JmrReport> lastReading = jmrReportRepo.findTopByOrderByReadingDateDesc();
        if (lastReading.isPresent()) {
            JmrReport j = lastReading.get();
            model.addAttribute("lastReading", j);
            model.addAttribute("mainMeter", j.getMainMeter());
            model.addAttribute("checkMeter", j.getCheckMeter());
        } else {
            model.addAttribute("lastReading", null);
            model.addAttribute("mainMeter", null);
            model.addAttribute("checkMeter", null);
        }

        // --- Sites + default site for live charts
        List<Site> sites = siteRepo.findAll();
        model.addAttribute("sites", sites);
        model.addAttribute("defaultSiteId", sites.isEmpty() ? null : sites.get(0).getId());

        // page flag for nav highlighting
        model.addAttribute("page", "dashboard");

        return "admin/dashboard";
    }
}
