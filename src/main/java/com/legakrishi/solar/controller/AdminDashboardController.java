package com.legakrishi.solar.controller;

import com.legakrishi.solar.model.Transaction;
import com.legakrishi.solar.model.User;
import com.legakrishi.solar.model.JmrReport;
import com.legakrishi.solar.repository.PartnerRepository;
import com.legakrishi.solar.repository.TransactionRepository;
import com.legakrishi.solar.repository.BillRepository;
import com.legakrishi.solar.repository.UserRepository;
import com.legakrishi.solar.repository.JmrReportRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import com.legakrishi.solar.repository.SiteRepository;

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

    @Autowired
    public AdminDashboardController(
            PartnerRepository partnerRepo,
            TransactionRepository transRepo,
            BillRepository billRepo,
            UserRepository userRepo,
            JmrReportRepository jmrReportRepo,
            SiteRepository siteRepo)
    {
        this.partnerRepo = partnerRepo;
        this.transRepo = transRepo;
        this.billRepo = billRepo;
        this.userRepo = userRepo;
        this.jmrReportRepo = jmrReportRepo;
        this.siteRepo = siteRepo;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String loggedInEmail = authentication.getName();

        Optional<User> userOpt = userRepo.findByEmail(loggedInEmail);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Logged-in user not found: " + loggedInEmail);
        }

        User user = userOpt.get();
        model.addAttribute("user", user);

        model.addAttribute("partnerCount", partnerRepo.count());

        Double totalIncomeReceived = billRepo.sumActualAmountReceivedByStatus("PAID");
        Double totalGovernmentDeductions = billRepo.sumGovernmentDeductionsByStatus("PAID");
        Double totalOutgoing = transRepo.sumAmountByType(Transaction.TransactionType.OUTGOING);
        Double otherIncome = transRepo.sumAmountByType(Transaction.TransactionType.OTHER_INCOME);

        totalIncomeReceived = totalIncomeReceived != null ? totalIncomeReceived : 0.0;
        totalGovernmentDeductions = totalGovernmentDeductions != null ? totalGovernmentDeductions : 0.0;
        totalOutgoing = totalOutgoing != null ? totalOutgoing : 0.0;
        otherIncome = otherIncome != null ? otherIncome : 0.0;

        double totalIncome = totalIncomeReceived + otherIncome;
        double balanceAmount = totalIncome - totalGovernmentDeductions - totalOutgoing;

        model.addAttribute("totalIncomeReceived", totalIncomeReceived);
        model.addAttribute("totalGovernmentDeductions", totalGovernmentDeductions);
        model.addAttribute("totalOutgoing", totalOutgoing);
        model.addAttribute("balanceAmount", balanceAmount);
        model.addAttribute("totalIncome", totalIncome);
        model.addAttribute("otherIncome", otherIncome);

        model.addAttribute("partners", partnerRepo.findAll());
        model.addAttribute("pendingBills", billRepo.findByStatus("PENDING"));

        // Correct: Fetch Last Reading from JmrReport instead of Bill
        Optional<JmrReport> lastReading = jmrReportRepo.findTopByOrderByReadingDateDesc();
        if (lastReading.isPresent()) {
            model.addAttribute("lastReading", lastReading.get());
            model.addAttribute("mainMeter", lastReading.get().getMainMeter());
            model.addAttribute("checkMeter", lastReading.get().getCheckMeter());
        } else {
            model.addAttribute("lastReading", null);
            model.addAttribute("mainMeter", null);
            model.addAttribute("checkMeter", null);
        }
        var sites = siteRepo.findAll();
        model.addAttribute("sites", sites);
        model.addAttribute("defaultSiteId", sites.isEmpty() ? null : sites.get(0).getId());

        model.addAttribute("page", "dashboard");

        return "admin/dashboard";
    }
}
