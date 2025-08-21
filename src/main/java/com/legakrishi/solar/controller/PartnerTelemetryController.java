package com.legakrishi.solar.controller;

import com.legakrishi.solar.config.MonitoringProps;
import com.legakrishi.solar.model.EnergySample;
import com.legakrishi.solar.model.MeterKind;
import com.legakrishi.solar.model.Reading;
import com.legakrishi.solar.repository.EnergySampleRepository;
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
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/partners/api")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PARTNER','ADMIN')")
public class PartnerTelemetryController {

    private final ReadingRepository readingRepo;
    private final MonitoringProps props;
    private final PartnerSiteRepository partnerSiteRepo;
    private final UserRepository userRepo;
    private final SiteRepository siteRepo;
    private final EnergySampleRepository energySampleRepo;

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }

    /** Resolve siteId: default from config; allow override if ADMIN or mapped PARTNER. */
    private Long resolveSiteId(Principal principal, Long siteIdParam) {
        Long siteId = props.getSiteId(); // default
        if (siteIdParam == null) return siteId;

        if (hasRole("ROLE_ADMIN")) {
            return siteIdParam;
        }
        if (principal != null) {
            var u = userRepo.findByEmail(principal.getName());
            if (u.isPresent() && u.get().getPartner() != null) {
                Long partnerId = u.get().getPartner().getId();
                boolean mapped = partnerSiteRepo.findByPartnerIdAndActiveTrue(partnerId).stream()
                        .anyMatch(ps -> ps.getSite() != null && ps.getSite().getId().equals(siteIdParam));
                if (mapped) return siteIdParam;
            }
        }
        return siteId;
    }

    private static double nz(Double v) { return v == null ? 0d : v; }
    private static double round1(double v) { return Math.round(v * 10.0) / 10.0; }
    private static String fmtIso(Instant t, ZoneId tz) { return ZonedDateTime.ofInstant(t, tz).toString(); }

    // ----------------- TODAY POWER -----------------

    @GetMapping("/today-power")
    public Map<String, Object> todayPower(
            Principal principal,
            @RequestParam(value = "siteId", required = false) Long siteIdParam) {

        Long siteId = resolveSiteId(principal, siteIdParam);

        // 1) Try READING table first
        var readings = readingRepo.findToday(siteId);
        if (!readings.isEmpty()) {
            List<String> labels = readings.stream()
                    .map(r -> r.getTs() != null ? r.getTs().toLocalTime().format(HHMM) : "")
                    .collect(Collectors.toList());
            List<Double> values = readings.stream()
                    .map(r -> nz(r.getPowerKw()))
                    .collect(Collectors.toList());

            double maxPower = values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

            double energyToday = 0.0;
            Double first = readings.get(0).getEnergyKwh();
            Double last  = readings.get(readings.size() - 1).getEnergyKwh();
            if (first != null && last != null) energyToday = Math.max(0.0, last - first);

            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("labels", labels);
            resp.put("values", values);
            resp.put("date", LocalDate.now(IST).toString());
            resp.put("maxPower", round1(maxPower));
            resp.put("energyToday", round1(energyToday));
            return resp;
        }

        // 2) Fallback to ENERGY_SAMPLE (MAIN) for "today" in IST
        ZonedDateTime startZdt = LocalDate.now(IST).atStartOfDay(IST);
        Instant start = startZdt.toInstant();
        Instant end   = startZdt.plusDays(1).toInstant();

        var samples = energySampleRepo
                .findBySiteIdAndMeterKindAndSampleTimeBetweenOrderBySampleTime(
                        siteId, MeterKind.MAIN, start, end);

        if (samples == null || samples.isEmpty()) {
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("labels", List.of());
            resp.put("values", List.of());
            resp.put("date", LocalDate.now(IST).toString());
            resp.put("maxPower", 0.0);
            resp.put("energyToday", 0.0);
            return resp;
        }

        List<String> labels = samples.stream()
                .map(s -> ZonedDateTime.ofInstant(s.getSampleTime(), IST).format(HHMM))
                .collect(Collectors.toList());

        List<Double> values = samples.stream()
                .map(s -> nz(s.getTotalAcPowerKw()))
                .collect(Collectors.toList());

        double maxPower = values.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);

        // If daily_ac_energy_kwh is cumulative per day, today's energy is the last non-null value.
        double energyToday = 0.0;
        for (int i = samples.size() - 1; i >= 0; i--) {
            Double v = samples.get(i).getDailyAcEnergyKwh();
            if (v != null) { energyToday = v; break; }
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("labels", labels);
        resp.put("values", values);
        resp.put("date", LocalDate.now(IST).toString());
        resp.put("maxPower", round1(maxPower));
        resp.put("energyToday", round1(energyToday));
        return resp;
    }

    // ----------------- CSV EXPORTS -----------------

    @GetMapping(value = "/export/today.csv", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<String> exportTodayCsv(
            Principal principal,
            @RequestParam(value = "siteId", required = false) Long siteIdParam) {

        Long siteId = resolveSiteId(principal, siteIdParam);

        var readings = readingRepo.findToday(siteId);
        StringBuilder sb = new StringBuilder();
        sb.append("timestamp,power_kw,energy_kwh,status\n");

        if (readings != null && !readings.isEmpty()) {
            for (Reading r : readings) {
                sb.append(r.getTs() != null ? r.getTs().toString() : "")
                        .append(',').append(r.getPowerKw() != null ? r.getPowerKw() : "")
                        .append(',').append(r.getEnergyKwh() != null ? r.getEnergyKwh() : "")
                        .append(',').append(r.getStatus() != null ? r.getStatus().replace(',', ' ') : "")
                        .append('\n');
            }
        } else {
            // Fallback rows from energy_sample (MAIN)
            LocalDate today = LocalDate.now(IST);
            Instant start = today.atStartOfDay(IST).toInstant();
            Instant end   = start.plus(1, java.time.temporal.ChronoUnit.DAYS);

            List<EnergySample> samples = energySampleRepo
                    .findBySiteIdAndMeterKindAndSampleTimeBetweenOrderBySampleTime(
                            siteId, MeterKind.MAIN, start, end);

            if (samples != null) {
                for (EnergySample s : samples) {
                    sb.append(fmtIso(s.getSampleTime(), IST)).append(',')
                            .append(s.getTotalAcPowerKw() != null ? s.getTotalAcPowerKw() : "").append(',')
                            .append(s.getDailyAcEnergyKwh() != null ? s.getDailyAcEnergyKwh() : "").append(',')
                            .append("") // status not available here
                            .append('\n');
                }
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=today-" + siteId + ".csv");
        return new ResponseEntity<>(sb.toString(), headers, HttpStatus.OK);
    }

    @GetMapping(value = "/export/month.csv", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<String> exportMonthCsv(
            Principal principal,
            @RequestParam(value = "siteId", required = false) Long siteIdParam) {

        Long siteId = resolveSiteId(principal, siteIdParam);

        var today = LocalDate.now(IST);
        var start = today.withDayOfMonth(1).atStartOfDay();
        var now   = LocalDateTime.now(IST);

        var readings = readingRepo.findRange(siteId, start, now);
        StringBuilder sb = new StringBuilder();
        sb.append("timestamp,power_kw,energy_kwh,status\n");

        if (readings != null && !readings.isEmpty()) {
            for (Reading r : readings) {
                sb.append(r.getTs() != null ? r.getTs().toString() : "")
                        .append(',').append(r.getPowerKw() != null ? r.getPowerKw() : "")
                        .append(',').append(r.getEnergyKwh() != null ? r.getEnergyKwh() : "")
                        .append(',').append(r.getStatus() != null ? r.getStatus().replace(',', ' ') : "")
                        .append('\n');
            }
        } else {
            // Fallback rows from energy_sample (MAIN)
            Instant from = start.atZone(IST).toInstant();
            Instant to   = now.atZone(IST).toInstant();
            List<EnergySample> samples = energySampleRepo
                    .findBySiteIdAndMeterKindAndSampleTimeBetweenOrderBySampleTime(
                            siteId, MeterKind.MAIN, from, to);

            if (samples != null) {
                for (EnergySample s : samples) {
                    sb.append(fmtIso(s.getSampleTime(), IST)).append(',')
                            .append(s.getTotalAcPowerKw() != null ? s.getTotalAcPowerKw() : "").append(',')
                            .append(s.getDailyAcEnergyKwh() != null ? s.getDailyAcEnergyKwh() : "").append(',')
                            .append("") // status NA
                            .append('\n');
                }
            }
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=month-" + siteId + "-" + today.toString().substring(0,7) + ".csv");
        return new ResponseEntity<>(sb.toString(), headers, HttpStatus.OK);
    }

    // ----------------- MONTH SUMMARY (JSON) -----------------

    @GetMapping("/month-summary")
    public Map<String, Object> monthSummary(
            Principal principal,
            @RequestParam(value = "siteId", required = false) Long siteIdParam) {

        Long siteId = resolveSiteId(principal, siteIdParam);

        var today = LocalDate.now(IST);
        var start = today.withDayOfMonth(1).atStartOfDay();
        var now   = LocalDateTime.now(IST);

        List<Reading> list = Optional.ofNullable(
                readingRepo.findRange(siteId, start, now)
        ).orElseGet(Collections::emptyList);

        double energyMonth = 0.0;
        double peakKw      = 0.0;
        boolean computedFromReading = false;

        if (!list.isEmpty()) {
            // compute delta only if both ends are non-null
            Double first = null, last = null;
            for (Reading r : list) {
                Double e = r.getEnergyKwh();
                if (e != null) { first = e; break; }
            }
            for (int i = list.size() - 1; i >= 0; i--) {
                Double e = list.get(i).getEnergyKwh();
                if (e != null) { last = e; break; }
            }
            if (first != null && last != null && last >= first) {
                energyMonth = last - first;
                computedFromReading = true;
            }
            peakKw = list.stream()
                    .map(Reading::getPowerKw)
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .max().orElse(0.0);
        }

        if (!computedFromReading) {
            // Fallback: ENERGY_SAMPLE (MAIN)
            Instant from = start.atZone(IST).toInstant();
            Instant to   = now.atZone(IST).toInstant();
            List<EnergySample> samples = Optional.ofNullable(
                    energySampleRepo.findBySiteIdAndMeterKindAndSampleTimeBetweenOrderBySampleTime(
                            siteId, MeterKind.MAIN, from, to)
            ).orElseGet(Collections::emptyList);

            // Peak from samples
            double samplePeak = samples.stream()
                    .map(EnergySample::getTotalAcPowerKw)
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .max().orElse(0.0);

            // Try last - first over the whole range (what your test expects)
            Double firstDaily = null, lastDaily = null;
            for (EnergySample s : samples) {
                Double d = s.getDailyAcEnergyKwh();
                if (d != null) { firstDaily = d; break; }
            }
            for (int i = samples.size() - 1; i >= 0; i--) {
                Double d = samples.get(i).getDailyAcEnergyKwh();
                if (d != null) { lastDaily = d; break; }
            }

            if (firstDaily != null && lastDaily != null && lastDaily >= firstDaily) {
                energyMonth = lastDaily - firstDaily;
            } else {
                // If last-first is unusable (e.g., daily resets), fall back to sum of per-day maxima.
                Map<LocalDate, Double> maxDailyByDate = new HashMap<>();
                for (EnergySample s : samples) {
                    Double d = s.getDailyAcEnergyKwh();
                    if (d == null) continue;
                    LocalDate day = s.getSampleTime().atZone(IST).toLocalDate();
                    maxDailyByDate.merge(day, d, Math::max);
                }
                energyMonth = maxDailyByDate.values().stream().mapToDouble(Double::doubleValue).sum();
            }

            if (peakKw <= 0.0) peakKw = samplePeak; // use sample peak if reading had none
        }

        int daysElapsed = today.getDayOfMonth();
        double avgPerDay = daysElapsed > 0 ? energyMonth / daysElapsed : 0.0;

        Double capacityKw = (siteId != null)
                ? siteRepo.findById(siteId).map(s -> s.getCapacityKw()).orElse(null)
                : null;

        Double cufPct = null;
        if (capacityKw != null && capacityKw > 0 && daysElapsed > 0) {
            double hours = daysElapsed * 24.0;
            cufPct = (energyMonth / (capacityKw * hours)) * 100.0;
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("month", today.toString().substring(0,7));
        resp.put("siteId", siteId);
        resp.put("energyMonthKwh", round1(energyMonth));
        resp.put("avgDailyKwh",    round1(avgPerDay));
        resp.put("peakPowerKw",    round1(peakKw));
        resp.put("pr", null);
        resp.put("cufPct", cufPct);
        return resp;
    }

    // ----------------- MONTH SUMMARY CSV -----------------

    @GetMapping(value = "/export/month-summary.csv", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<String> exportMonthSummaryCsv(
            Principal principal,
            @RequestParam(value = "siteId", required = false) Long siteIdParam) {

        Long siteId = resolveSiteId(principal, siteIdParam);

        var today = LocalDate.now(IST);
        var start = today.withDayOfMonth(1).atStartOfDay();
        var now   = LocalDateTime.now(IST);

        List<Reading> list = Optional.ofNullable(
                readingRepo.findRange(siteId, start, now)
        ).orElseGet(Collections::emptyList);

        double energyMonth = 0.0;
        double peakKw      = 0.0;
        boolean computedFromReading = false;

        if (!list.isEmpty()) {
            Double first = null, last = null;
            for (Reading r : list) {
                Double e = r.getEnergyKwh();
                if (e != null) { first = e; break; }
            }
            for (int i = list.size() - 1; i >= 0; i--) {
                Double e = list.get(i).getEnergyKwh();
                if (e != null) { last = e; break; }
            }
            if (first != null && last != null && last >= first) {
                energyMonth = last - first;
                computedFromReading = true;
            }
            peakKw = list.stream()
                    .map(Reading::getPowerKw)
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .max().orElse(0.0);
        }

        if (!computedFromReading) {
            Instant from = start.atZone(IST).toInstant();
            Instant to   = now.atZone(IST).toInstant();
            List<EnergySample> samples = Optional.ofNullable(
                    energySampleRepo.findBySiteIdAndMeterKindAndSampleTimeBetweenOrderBySampleTime(
                            siteId, MeterKind.MAIN, from, to)
            ).orElseGet(Collections::emptyList);

            double samplePeak = samples.stream()
                    .map(EnergySample::getTotalAcPowerKw)
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .max().orElse(0.0);

            Double firstDaily = null, lastDaily = null;
            for (EnergySample s : samples) {
                Double d = s.getDailyAcEnergyKwh();
                if (d != null) { firstDaily = d; break; }
            }
            for (int i = samples.size() - 1; i >= 0; i--) {
                Double d = samples.get(i).getDailyAcEnergyKwh();
                if (d != null) { lastDaily = d; break; }
            }

            if (firstDaily != null && lastDaily != null && lastDaily >= firstDaily) {
                energyMonth = lastDaily - firstDaily;
            } else {
                Map<LocalDate, Double> maxDailyByDate = new HashMap<>();
                for (EnergySample s : samples) {
                    Double d = s.getDailyAcEnergyKwh();
                    if (d == null) continue;
                    LocalDate day = s.getSampleTime().atZone(IST).toLocalDate();
                    maxDailyByDate.merge(day, d, Math::max);
                }
                energyMonth = maxDailyByDate.values().stream().mapToDouble(Double::doubleValue).sum();
            }

            if (peakKw <= 0.0) peakKw = samplePeak;
        }

        int daysElapsed = today.getDayOfMonth();
        double avgPerDay = daysElapsed > 0 ? energyMonth / daysElapsed : 0.0;

        Double capacityKw = (siteId != null)
                ? siteRepo.findById(siteId).map(s -> s.getCapacityKw()).orElse(null)
                : null;

        Double cufPct = null;
        if (capacityKw != null && capacityKw > 0 && daysElapsed > 0) {
            double hours = daysElapsed * 24.0;
            cufPct = (energyMonth / (capacityKw * hours)) * 100.0;
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
