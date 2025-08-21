package com.legakrishi.solar.controller;

import com.legakrishi.solar.config.MonitoringProps;
import com.legakrishi.solar.kpi.PartnerKpiService;
import com.legakrishi.solar.model.JmrReport;
import com.legakrishi.solar.model.Partner;
import com.legakrishi.solar.model.PartnerSite;
import com.legakrishi.solar.model.User;
import com.legakrishi.solar.repository.BillRepository;
import com.legakrishi.solar.repository.JmrReportRepository;
import com.legakrishi.solar.repository.PartnerRepository;
import com.legakrishi.solar.repository.PartnerSiteRepository;
import com.legakrishi.solar.repository.TransactionRepository;
import com.legakrishi.solar.repository.UserRepository;
import com.legakrishi.solar.service.BillService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Controller
public class PartnerDashboardController {

    private final PartnerKpiService partnerKpiService;

    private final UserRepository userRepo;
    private final PartnerRepository partnerRepo;
    private final BillRepository billRepo;
    private final TransactionRepository transRepo;
    private final JmrReportRepository jmrRepo;
    private final BillService billService;
    private final MonitoringProps monitoringProps;
    private final PartnerSiteRepository partnerSiteRepo;

    public PartnerDashboardController(
            PartnerKpiService partnerKpiService,
            UserRepository userRepo,
            PartnerRepository partnerRepo,
            BillRepository billRepo,
            TransactionRepository transRepo,
            JmrReportRepository jmrRepo,
            BillService billService,
            MonitoringProps monitoringProps,
            PartnerSiteRepository partnerSiteRepo
    ) {
        this.partnerKpiService = partnerKpiService;
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
        model.addAttribute("page", "dashboard");

        // Partner's active sites
        List<PartnerSite> links = partnerSiteRepo.findByPartnerIdAndActiveTrue(partner.getId());
        model.addAttribute("sites", links);

        // --- Resolve current site id deterministically with Long-only fallbacks ---
        Long resolvedSiteId;
        if (siteIdParam != null && belongsToPartner(links, siteIdParam)) {
            resolvedSiteId = siteIdParam;
        } else if (!links.isEmpty() && links.get(0).getSite() != null && links.get(0).getSite().getId() != null) {
            resolvedSiteId = links.get(0).getSite().getId(); // <-- Long
        } else {
            // IMPORTANT: ensure this is a Long (MonitoringProps#getSiteId returns Long in your code)
            resolvedSiteId = monitoringProps.getSiteId();
            if (resolvedSiteId == null) {
                resolvedSiteId = 1L; // final hard fallback as Long literal (never int)
            }
        }
        model.addAttribute("currentSiteId", resolvedSiteId);

        // KPIs for the resolved site
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

        // Last JMR reading (optional)
        Optional<JmrReport> lastReadingOpt = jmrRepo.findTopByOrderByReadingDateDesc();
        lastReadingOpt.ifPresent(lastReading -> {
            model.addAttribute("lastReading", lastReading);
            model.addAttribute("mainMeter", lastReading.getMainMeter());
            model.addAttribute("checkMeter", lastReading.getCheckMeter());
        });

        return "partners/dashboard";
    }

    private static boolean belongsToPartner(List<PartnerSite> links, Long siteId) {
        if (siteId == null) return false;
        return links.stream().anyMatch(ps ->
                ps != null &&
                        ps.getSite() != null &&
                        Objects.equals(ps.getSite().getId(), siteId));
    }
}
