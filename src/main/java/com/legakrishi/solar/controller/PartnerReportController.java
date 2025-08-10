package com.legakrishi.solar.controller;

import com.legakrishi.solar.config.MonitoringProps;
import com.legakrishi.solar.model.AlertEvent;
import com.legakrishi.solar.model.Reading;
import com.legakrishi.solar.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/partners/report")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PARTNER','ADMIN')")
public class PartnerReportController {

    private final MonitoringProps props;
    private final PartnerSiteRepository partnerSiteRepo;
    private final UserRepository userRepo;
    private final ReadingRepository readingRepo;
    private final AlertEventRepository alertRepo;
    private final DeviceRepository deviceRepo;
    private final SiteRepository siteRepo;

    @GetMapping("/daily")
    public String daily(Model model,
                        Principal principal,
                        @RequestParam(value = "siteId", required = false) Long siteIdParam) {

        Long siteId = resolveSiteId(principal, siteIdParam);

        ZoneId tz = ZoneId.of("Asia/Kolkata");
        LocalDate today = LocalDate.now(tz);

        // --- Today's readings & KPIs ---
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        var readings = readingRepo.findRange(siteId, start, end);

        double energyToday = 0.0;
        if (!readings.isEmpty()) {
            Double first = readings.get(0).getEnergyKwh();
            Double last  = readings.get(readings.size()-1).getEnergyKwh();
            if (first != null && last != null) energyToday = Math.max(0.0, last - first);
        }

        double peakKw = readings.stream()
                .map(Reading::getPowerKw)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .max().orElse(0.0);

        // --- Month-to-date CUF (uses site capacity) ---
        LocalDateTime mStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now(tz);
        var monthReadings = readingRepo.findRange(siteId, mStart, now);

        double energyMonth = 0.0;
        if (!monthReadings.isEmpty()) {
            Double f = monthReadings.get(0).getEnergyKwh();
            Double l = monthReadings.get(monthReadings.size()-1).getEnergyKwh();
            if (f != null && l != null) energyMonth = Math.max(0.0, l - f);
        }

        int daysElapsed = today.getDayOfMonth();
        Double capacityKw = siteRepo.findById(siteId).map(s -> s.getCapacityKw()).orElse(null);
        Double cufPct = null;
        if (capacityKw != null && capacityKw > 0 && daysElapsed > 0) {
            double hours = daysElapsed * 24.0;
            cufPct = (energyMonth / (capacityKw * hours)) * 100.0;
        }

        // --- Alerts (last 24h) ---
        Long deviceId = deviceRepo.findFirstBySiteIdOrderByIdAsc(siteId).map(d -> d.getId()).orElse(null);
        List<AlertEvent> alerts = Collections.emptyList();
        if (deviceId != null) {
            var since = LocalDateTime.now(tz).minusDays(1);
            alerts = alertRepo.findAll().stream()
                    .filter(a -> a.getDevice() != null && a.getDevice().getId().equals(deviceId))
                    .filter(a -> a.getTriggeredAt() != null && a.getTriggeredAt().isAfter(since))
                    .sorted((a,b) -> b.getTriggeredAt().compareTo(a.getTriggeredAt()))
                    .limit(10)
                    .collect(Collectors.toList());
        }

        String siteName = siteRepo.findById(siteId)
                .map(s -> (s.getName() != null && !s.getName().isBlank()) ? s.getName() : ("Site #" + s.getId()))
                .orElse("Site #" + siteId);

        // Model
        model.addAttribute("siteId", siteId);
        model.addAttribute("siteName", siteName);
        model.addAttribute("date", today.toString());

        model.addAttribute("energyTodayKwh", energyToday);
        model.addAttribute("peakPowerKw", peakKw);

        // new items for header
        model.addAttribute("capacityKw", capacityKw);   // may be null
        model.addAttribute("cufPct", cufPct);           // may be null
        model.addAttribute("monthStr", today.toString().substring(0,7)); // yyyy-MM

        model.addAttribute("readings", readings);
        model.addAttribute("alerts", alerts);

        return "partners/report-daily";
    }

    // ----- helpers -----

    private Long resolveSiteId(Principal principal, Long siteIdParam) {
        Long siteId = props.getSiteId();

        if (siteIdParam == null) return siteId;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);

        if (isAdmin) return siteIdParam;

        if (principal != null) {
            var userOpt = userRepo.findByEmail(principal.getName());
            if (userOpt.isPresent() && userOpt.get().getPartner() != null) {
                Long partnerId = userOpt.get().getPartner().getId();
                boolean mapped = partnerSiteRepo.findByPartnerIdAndActiveTrue(partnerId).stream()
                        .anyMatch(ps -> ps.getSite() != null && ps.getSite().getId().equals(siteIdParam));
                if (mapped) return siteIdParam;
            }
        }
        return siteId;
    }
}
