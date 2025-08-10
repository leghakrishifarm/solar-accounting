package com.legakrishi.solar.controller;

import com.legakrishi.solar.config.MonitoringProps;
import com.legakrishi.solar.kpi.PartnerKpiService;
import com.legakrishi.solar.model.JmrReport;
import com.legakrishi.solar.model.Partner;
import com.legakrishi.solar.model.User;
import com.legakrishi.solar.model.PartnerSite;
import com.legakrishi.solar.repository.*;
import com.legakrishi.solar.service.BillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
public class PartnerDashboardController {

    @Autowired private PartnerKpiService partnerKpiService;

    private final UserRepository userRepo;
    private final PartnerRepository partnerRepo;
    private final BillRepository billRepo;
    private final TransactionRepository transRepo;
    private final JmrReportRepository jmrRepo;
    private final BillService billService;
    private final MonitoringProps monitoringProps;
    private final PartnerSiteRepository partnerSiteRepo;

    public PartnerDashboardController(
            UserRepository userRepo,
            PartnerRepository partnerRepo,
            BillRepository billRepo,
            TransactionRepository transRepo,
            JmrReportRepository jmrRepo,
            BillService billService,
            MonitoringProps monitoringProps,
            PartnerSiteRepository partnerSiteRepo
    ) {
        this.userRepo = userRepo;
        this.partnerRepo = partnerRepo;
        this.billRepo = billRepo;
        this.transRepo = transRepo;
        this.jmrRepo = jmrRepo;
        this.billService = billService;
        this.monitoringProps = monitoringProps;
        this.partnerSiteRepo = partnerSiteRepo;
    }

    @GetMapping("/partners/dashboard")
    @PreAuthorize("hasRole('PARTNER')")
    public String dashboard(Model model,
                            Principal principal,
                            @RequestParam(value = "siteId", required = false) Long siteIdParam) {

        // Logged-in Partner
        String email = principal.getName();
        User user = userRepo.findByEmail(email).orElseThrow();
        Partner partner = user.getPartner();
        model.addAttribute("partner", partner);
        model.addAttribute("page", "dashboard"); // highlight active menu

        // Load partner's active site links (could be 0..N)
        List<PartnerSite> links = partnerSiteRepo.findByPartnerIdAndActiveTrue(partner.getId());
        model.addAttribute("sites", links);

        // Resolve which site to show:
        // 1) if siteId param present AND belongs to this partner -> use it
        // 2) else first mapped site
        // 3) else fallback to configured site-id
        Long resolvedSiteId = null;
        if (siteIdParam != null &&
                links.stream().anyMatch(ps -> ps.getSite() != null && ps.getSite().getId().equals(siteIdParam))) {
            resolvedSiteId = siteIdParam;
        } else {
            resolvedSiteId = links.stream()
                    .findFirst()
                    .map(ps -> ps.getSite().getId())
                    .orElse(monitoringProps.getSiteId());
        }
        model.addAttribute("currentSiteId", resolvedSiteId);

        // KPIs for resolved site
        var kpis = partnerKpiService.compute(resolvedSiteId);
        model.addAttribute("kpis", kpis);

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

        // keep existing template name to avoid breaking anything
        return "partners/dashboard";
    }
}
