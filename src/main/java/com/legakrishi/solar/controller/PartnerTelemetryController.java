package com.legakrishi.solar.controller;

import com.legakrishi.solar.config.MonitoringProps;
import com.legakrishi.solar.model.Reading;
import com.legakrishi.solar.repository.PartnerSiteRepository;
import com.legakrishi.solar.repository.ReadingRepository;
import com.legakrishi.solar.repository.SiteRepository;
import com.legakrishi.solar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/partners/api")
@RequiredArgsConstructor
public class PartnerTelemetryController {

    private final ReadingRepository readingRepo;
    private final MonitoringProps props;
    private final PartnerSiteRepository partnerSiteRepo;
    private final UserRepository userRepo;
    private final SiteRepository siteRepo; // NEW

    @GetMapping("/today-power")
    @ResponseBody
    @PreAuthorize("hasAnyRole('PARTNER','ADMIN')")
    public Map<String, Object> todayPower(
            Principal principal,
            @RequestParam(value = "siteId", required = false) Long siteIdParam) {

        // default from config
        Long siteId = props.getSiteId();

        // if a siteId is passed, allow if:
        // - caller is ADMIN, or
        // - caller is a PARTNER mapped to that site
        if (siteIdParam != null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth != null && auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch("ROLE_ADMIN"::equals);

            if (isAdmin) {
                siteId = siteIdParam;
            } else if (principal != null) {
                var userOpt = userRepo.findByEmail(principal.getName());
                if (userOpt.isPresent() && userOpt.get().getPartner() != null) {
                    Long partnerId = userOpt.get().getPartner().getId();
                    boolean mapped = partnerSiteRepo.findByPartnerIdAndActiveTrue(partnerId).stream()
                            .anyMatch(ps -> ps.getSite() != null && ps.getSite().getId().equals(siteIdParam));
                    if (mapped) siteId = siteIdParam; // else keep default
                }
            }
        }

        var readings = readingRepo.findToday(siteId);

        List<String> labels = new ArrayList<>(readings.size());
        List<Double> values = new ArrayList<>(readings.size());
        for (Reading r : readings) {
            labels.add(r.getTs().toLocalTime().toString());
            values.add(r.getPowerKw() == null ? 0.0 : r.getPowerKw());
        }

        double maxPower = readings.stream()
                .map(Reading::getPowerKw).filter(Objects::nonNull)
                .max(Double::compareTo).orElse(0.0);

        double energyToday = 0.0;
        if (!readings.isEmpty()) {
            var first = readings.get(0).getEnergyKwh();
            var last  = readings.get(readings.size()-1).getEnergyKwh();
            if (first != null && last != null) energyToday = Math.max(0.0, last - first);
        }

        return Map.of(
                "labels", labels,
                "values", values,
                "date", LocalDate.now().toString(),
                "maxPower", maxPower,
                "energyToday", energyToday
        );
    }

    @GetMapping(value = "/export/today.csv", produces = "text/csv; charset=UTF-8")
    @ResponseBody
    @PreAuthorize("hasAnyRole('PARTNER','ADMIN')")
    public ResponseEntity<String> exportTodayCsv(
            Principal principal,
            @RequestParam(value = "siteId", required = false) Long siteIdParam) {

        // default from config
        Long siteId = props.getSiteId();

        // allow site override if ADMIN or mapped PARTNER (same logic as todayPower)
        if (siteIdParam != null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth != null && auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch("ROLE_ADMIN"::equals);

            if (isAdmin) {
                siteId = siteIdParam;
            } else if (principal != null) {
                var userOpt = userRepo.findByEmail(principal.getName());
                if (userOpt.isPresent() && userOpt.get().getPartner() != null) {
                    Long partnerId = userOpt.get().getPartner().getId();
                    boolean mapped = partnerSiteRepo.findByPartnerIdAndActiveTrue(partnerId).stream()
                            .anyMatch(ps -> ps.getSite() != null && ps.getSite().getId().equals(siteIdParam));
                    if (mapped) siteId = siteIdParam;
                }
            }
        }

        var readings = readingRepo.findToday(siteId);

        StringBuilder sb = new StringBuilder();
        sb.append("timestamp,power_kw,energy_kwh,status\n");
        for (Reading r : readings) {
            sb.append(r.getTs() != null ? r.getTs().toString() : "")
                    .append(',')
                    .append(r.getPowerKw() != null ? r.getPowerKw() : "")
                    .append(',')
                    .append(r.getEnergyKwh() != null ? r.getEnergyKwh() : "")
                    .append(',')
                    .append(r.getStatus() != null ? r.getStatus().replace(',', ' ') : "")
                    .append('\n');
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=today-" + siteId + ".csv");
        return new ResponseEntity<>(sb.toString(), headers, HttpStatus.OK);
    }

    @GetMapping(value = "/export/month.csv", produces = "text/csv; charset=UTF-8")
    @ResponseBody
    @PreAuthorize("hasAnyRole('PARTNER','ADMIN')")
    public ResponseEntity<String> exportMonthCsv(
            Principal principal,
            @RequestParam(value = "siteId", required = false) Long siteIdParam) {

        // default from config
        Long siteId = props.getSiteId();

        // allow site override if ADMIN or mapped PARTNER (same rules as todayPower)
        if (siteIdParam != null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth != null && auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch("ROLE_ADMIN"::equals);

            if (isAdmin) {
                siteId = siteIdParam;
            } else if (principal != null) {
                var u = userRepo.findByEmail(principal.getName());
                if (u.isPresent() && u.get().getPartner() != null) {
                    Long partnerId = u.get().getPartner().getId();
                    boolean mapped = partnerSiteRepo.findByPartnerIdAndActiveTrue(partnerId).stream()
                            .anyMatch(ps -> ps.getSite() != null && ps.getSite().getId().equals(siteIdParam));
                    if (mapped) siteId = siteIdParam;
                }
            }
        }

        // Month window: 1st of month 00:00 → now (Asia/Kolkata)
        var tz = java.time.ZoneId.of("Asia/Kolkata");
        var today = java.time.LocalDate.now(tz);
        var start = today.withDayOfMonth(1).atStartOfDay();
        var end = java.time.LocalDateTime.now(tz);

        var readings = readingRepo.findRange(siteId, start, end);

        StringBuilder sb = new StringBuilder();
        sb.append("timestamp,power_kw,energy_kwh,status\n");
        for (Reading r : readings) {
            sb.append(r.getTs() != null ? r.getTs().toString() : "")
                    .append(',')
                    .append(r.getPowerKw() != null ? r.getPowerKw() : "")
                    .append(',')
                    .append(r.getEnergyKwh() != null ? r.getEnergyKwh() : "")
                    .append(',')
                    .append(r.getStatus() != null ? r.getStatus().replace(',', ' ') : "")
                    .append('\n');
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=month-" + siteId + "-" + today.toString().substring(0,7) + ".csv");
        return new ResponseEntity<>(sb.toString(), headers, HttpStatus.OK);
    }

    // --- Month-to-date summary for the selected site (with CUF%) ---
    @GetMapping("/month-summary")
    @ResponseBody
    @PreAuthorize("hasAnyRole('PARTNER','ADMIN')")
    public Map<String, Object> monthSummary(
            Principal principal,
            @RequestParam(value = "siteId", required = false) Long siteIdParam) {

        // default from config
        Long siteId = props.getSiteId();

        // allow site override if ADMIN or mapped PARTNER (same rules as todayPower)
        if (siteIdParam != null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth != null && auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch("ROLE_ADMIN"::equals);

            if (isAdmin) {
                siteId = siteIdParam;
            } else if (principal != null) {
                var u = userRepo.findByEmail(principal.getName());
                if (u.isPresent() && u.get().getPartner() != null) {
                    Long partnerId = u.get().getPartner().getId();
                    boolean mapped = partnerSiteRepo.findByPartnerIdAndActiveTrue(partnerId).stream()
                            .anyMatch(ps -> ps.getSite() != null && ps.getSite().getId().equals(siteIdParam));
                    if (mapped) siteId = siteIdParam;
                }
            }
        }

        var tz = java.time.ZoneId.of("Asia/Kolkata");
        var today = java.time.LocalDate.now(tz);
        var start = today.withDayOfMonth(1).atStartOfDay();
        var now   = java.time.LocalDateTime.now(tz);

        var list = readingRepo.findRange(siteId, start, now);

        double energyMonth = 0.0;
        if (!list.isEmpty()) {
            var first = list.get(0).getEnergyKwh();
            var last  = list.get(list.size()-1).getEnergyKwh();
            if (first != null && last != null) energyMonth = Math.max(0.0, last - first);
        }

        double peakKw = list.stream()
                .map(Reading::getPowerKw)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .max().orElse(0.0);

        long daysElapsed = today.getDayOfMonth(); // MTD days including today
        double hoursInPeriod = daysElapsed * 24.0;

        // CUF% = Energy (kWh) / (Capacity (kW) * Hours in period) * 100
        Double capacityKw = siteRepo.findById(siteId).map(s -> s.getCapacityKw()).orElse(null);
        Double cufPct = null;
        if (capacityKw != null && capacityKw > 0 && hoursInPeriod > 0) {
            cufPct = (energyMonth / (capacityKw * hoursInPeriod)) * 100.0;
        }

        // avg per day based on days that passed
        double avgPerDay = daysElapsed > 0 ? energyMonth / daysElapsed : 0.0;

        return Map.of(
                "month", today.toString().substring(0,7), // e.g. 2025-08
                "siteId", siteId,
                "energyMonthKwh", energyMonth,
                "avgDailyKwh", avgPerDay,
                "peakPowerKw", peakKw,
                "pr", null,         // needs irradiance/target; coming later
                "cufPct", cufPct    // null if capacity not set
        );
    }

    // --- NEW: Month Summary CSV export (MTD) ---
    @GetMapping(value = "/export/month-summary.csv", produces = "text/csv; charset=UTF-8")
    @ResponseBody
    @PreAuthorize("hasAnyRole('PARTNER','ADMIN')")
    public ResponseEntity<String> exportMonthSummaryCsv(
            Principal principal,
            @RequestParam(value = "siteId", required = false) Long siteIdParam) {

        // default site
        Long siteId = props.getSiteId();

        // allow override if ADMIN or mapped PARTNER
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (siteIdParam != null) {
            boolean isAdmin = auth != null && auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch("ROLE_ADMIN"::equals);

            if (isAdmin) {
                siteId = siteIdParam;
            } else if (principal != null) {
                var u = userRepo.findByEmail(principal.getName());
                if (u.isPresent() && u.get().getPartner() != null) {
                    Long partnerId = u.get().getPartner().getId();
                    boolean mapped = partnerSiteRepo.findByPartnerIdAndActiveTrue(partnerId).stream()
                            .anyMatch(ps -> ps.getSite() != null && ps.getSite().getId().equals(siteIdParam));
                    if (mapped) siteId = siteIdParam;
                }
            }
        }

        var tz = java.time.ZoneId.of("Asia/Kolkata");
        var today = java.time.LocalDate.now(tz);
        var start = today.withDayOfMonth(1).atStartOfDay();
        var now   = java.time.LocalDateTime.now(tz);

        var list = readingRepo.findRange(siteId, start, now);

        double energyMonth = 0.0;
        if (!list.isEmpty()) {
            var first = list.get(0).getEnergyKwh();
            var last  = list.get(list.size()-1).getEnergyKwh();
            if (first != null && last != null) energyMonth = Math.max(0.0, last - first);
        }

        double peakKw = list.stream()
                .map(Reading::getPowerKw)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .max().orElse(0.0);

        int daysElapsed = today.getDayOfMonth();
        double avgPerDay = daysElapsed > 0 ? energyMonth / daysElapsed : 0.0;

        Double capacityKw = siteRepo.findById(siteId).map(s -> s.getCapacityKw()).orElse(null);
        Double cufPct = null;
        if (capacityKw != null && capacityKw > 0 && daysElapsed > 0) {
            double hoursInPeriod = daysElapsed * 24.0;
            cufPct = (energyMonth / (capacityKw * hoursInPeriod)) * 100.0;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("month,site_id,energy_month_kwh,avg_daily_kwh,peak_power_kw,cuf_pct\n");
        sb.append(today.toString().substring(0,7)).append(',')
                .append(siteId).append(',')
                .append(String.format(Locale.US, "%.3f", energyMonth)).append(',')
                .append(String.format(Locale.US, "%.3f", avgPerDay)).append(',')
                .append(String.format(Locale.US, "%.3f", peakKw)).append(',')
                .append(cufPct != null ? String.format(Locale.US, "%.2f", cufPct) : "")
                .append('\n');

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=month-summary-" + siteId + "-" + today.toString().substring(0,7) + ".csv");
        return new ResponseEntity<>(sb.toString(), headers, HttpStatus.OK);
    }
}
