package com.legakrishi.solar.service;

import com.legakrishi.solar.model.MeterKind;
import com.legakrishi.solar.model.Reading;
import com.legakrishi.solar.model.ReadingDayMeter;
import com.legakrishi.solar.repository.ReadingDayMeterRepository;
import com.legakrishi.solar.repository.ReadingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ChartSeriesService {

    private final ReadingDayMeterRepository rdmRepo;
    private final ReadingRepository readingRepo;

    public ChartSeriesService(ReadingDayMeterRepository rdmRepo, ReadingRepository readingRepo) {
        this.rdmRepo = rdmRepo;
        this.readingRepo = readingRepo;
    }

    /** ------------------------ DAILY (last N days) ------------------------ */
    public Map<String, Object> buildDayMeterSeries(Long siteId, int days) {
        if (days <= 0) days = 30;

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days - 1);

        DateTimeFormatter ISO = DateTimeFormatter.ISO_DATE;
        List<String> labels = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            labels.add(ISO.format(d));
        }
        int len = labels.size();

        Map<String, Map<String, List<BigDecimal>>> series = new LinkedHashMap<>();
        series.put("acActiveEnergyKwh", emptyMeterMap(len));
        series.put("acExportEnergyKwh", emptyMeterMap(len));
        series.put("acImportEnergyKwh", emptyMeterMap(len));
        series.put("dcEnergyKwh",       emptyMeterMap(len));
        series.put("maxAcPowerKw",      emptyMeterMap(len));

        for (MeterKind mk : MeterKind.values()) {
            List<ReadingDayMeter> rows = rdmRepo
                    .findBySiteIdAndMeterKindAndDayBetweenOrderByDayAsc(siteId, mk, start, end);

            for (ReadingDayMeter r : rows) {
                int idx = (int) ChronoUnit.DAYS.between(start, r.getDay());
                if (idx < 0 || idx >= len) continue;

                set(series.get("acActiveEnergyKwh").get(mk.name()), idx, nz(r.getAcActiveEnergyKwh()));
                set(series.get("acExportEnergyKwh").get(mk.name()), idx, nz(r.getAcExportEnergyKwh()));
                set(series.get("acImportEnergyKwh").get(mk.name()), idx, nz(r.getAcImportEnergyKwh()));
                set(series.get("dcEnergyKwh").get(mk.name()),       idx, nz(r.getDcEnergyKwh()));
                set(series.get("maxAcPowerKw").get(mk.name()),      idx, nz(r.getMaxAcPowerKw()));
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("labels", labels);
        out.put("series", series);
        return out;
    }

    /** ------------------------ INTRADAY (time-view) ------------------------ */
    @Transactional(readOnly = true)
    public Map<String, Object> buildIntradayMeterSeries(Long siteId,
                                                        LocalDate day,
                                                        String metric,
                                                        Integer stepMin) {
        if (day == null) day = LocalDate.now();
        if (stepMin == null || stepMin <= 0) stepMin = 10;

        LocalDateTime start = day.atStartOfDay();
        LocalDateTime end   = day.plusDays(1).atStartOfDay();

        // Time labels (HH:mm)
        List<LocalDateTime> ticks = new ArrayList<>();
        for (LocalDateTime t = start; !t.isAfter(end); t = t.plusMinutes(stepMin)) ticks.add(t);
        DateTimeFormatter HHmm = DateTimeFormatter.ofPattern("HH:mm");
        List<String> labels = ticks.stream().map(t -> HHmm.format(t)).toList();

        // meter -> values
        Map<String, List<BigDecimal>> series = new LinkedHashMap<>();
        for (MeterKind mk : MeterKind.values()) {
            series.put(mk.name(), new ArrayList<>(Collections.nCopies(labels.size(), BigDecimal.ZERO)));
        }

        String unit = unitFor(metric);

        // fill per meter
        for (MeterKind mk : MeterKind.values()) {
            List<Reading> list = readingRepo
                    .findBySiteIdAndMeterKindAndTsBetweenOrderByTsAsc(siteId, mk, start, end);

            // legacy MAIN rows with NULL meter_kind: merge into MAIN
            if (mk == MeterKind.MAIN) {
                List<Reading> legacy = readingRepo
                        .findBySiteIdAndTsBetweenAndMeterKindIsNullOrderByTsAsc(siteId, start, end);
                if (!legacy.isEmpty()) {
                    list = merge(list, legacy);
                }
            }

            if (list.isEmpty()) continue;

            // base total for "daily-from-total" fallback
            BigDecimal base = firstBaseTotal(list, metric);

            int ri = 0;
            BigDecimal last = BigDecimal.ZERO;
            List<BigDecimal> out = series.get(mk.name());

            for (int ti = 0; ti < labels.size(); ti++) {
                LocalDateTime tick = ticks.get(ti);
                while (ri < list.size() && !list.get(ri).getTs().isAfter(tick)) {
                    BigDecimal v = valueOf(list.get(ri), metric, base);
                    if (v != null) last = v;
                    ri++;
                }
                out.set(ti, last != null ? last : BigDecimal.ZERO);
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("labels", labels);
        out.put("metric", metric);
        out.put("unit", unit);
        out.put("series", series);
        return out;
    }

    /* ---------------------------- helpers ---------------------------- */

    private static Map<String, List<BigDecimal>> emptyMeterMap(int len) {
        Map<String, List<BigDecimal>> m = new LinkedHashMap<>();
        for (MeterKind mk : MeterKind.values()) {
            m.put(mk.name(), new ArrayList<>(Collections.nCopies(len, BigDecimal.ZERO)));
        }
        return m;
    }

    private static void set(List<BigDecimal> arr, int idx, BigDecimal val) {
        if (arr == null || idx < 0 || idx >= arr.size()) return;
        arr.set(idx, val != null ? val : BigDecimal.ZERO);
    }

    private static BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }

    private static String unitFor(String metric) {
        return "TOTAL_AC_POWER".equalsIgnoreCase(metric) ? "kW" : "kWh";
        // extend if you add other instantaneous metrics
    }

    private static List<Reading> merge(List<Reading> a, List<Reading> b) {
        List<Reading> m = new ArrayList<>(a.size() + b.size());
        m.addAll(a); m.addAll(b);
        m.sort(Comparator.comparing(Reading::getTs));
        return m;
    }

    /** First non-null "total" for the chosen metric, used to compute daily=total-d0 when daily column is absent */
    private static BigDecimal firstBaseTotal(List<Reading> list, String metric) {
        for (Reading r : list) {
            BigDecimal t = switch (metric.toUpperCase()) {
                case "TOTAL_AC_ENERGY" -> r.getTotalAcActiveEnergyKwh();
                case "TOTAL_AC_EXPORT" -> r.getTotalAcActiveExportEnergyKwh();
                case "TOTAL_AC_IMPORT" -> r.getTotalAcActiveImportEnergyKwh();
                case "TOTAL_DC_ENERGY" -> r.getTotalDcEnergyKwh();
                default -> null;
            };
            if (t != null) return t;
        }
        return null;
    }

    private static BigDecimal valueOf(Reading r, String metric, BigDecimal baseTotal) {
        switch (metric.toUpperCase()) {
            case "TOTAL_AC_POWER":
                return r.getTotalAcActivePowerKw(); // instantaneous kW

            case "DAILY_AC_ENERGY":
                if (r.getDailyAcActiveEnergyKwh() != null) return r.getDailyAcActiveEnergyKwh();
                if (r.getTotalAcActiveEnergyKwh() != null && baseTotal != null)
                    return r.getTotalAcActiveEnergyKwh().subtract(baseTotal).max(BigDecimal.ZERO);
                return null;

            case "DAILY_AC_EXPORT":
                if (r.getDailyAcActiveExportEnergyKwh() != null) return r.getDailyAcActiveExportEnergyKwh();
                if (r.getTotalAcActiveExportEnergyKwh() != null && baseTotal != null)
                    return r.getTotalAcActiveExportEnergyKwh().subtract(baseTotal).max(BigDecimal.ZERO);
                return null;

            case "DAILY_AC_IMPORT":
                if (r.getDailyAcActiveImportEnergyKwh() != null) return r.getDailyAcActiveImportEnergyKwh();
                if (r.getTotalAcActiveImportEnergyKwh() != null && baseTotal != null)
                    return r.getTotalAcActiveImportEnergyKwh().subtract(baseTotal).max(BigDecimal.ZERO);
                return null;

            case "DAILY_DC_ENERGY":
                if (r.getDailyDcEnergyKwh() != null) return r.getDailyDcEnergyKwh();
                if (r.getTotalDcEnergyKwh() != null && baseTotal != null)
                    return r.getTotalDcEnergyKwh().subtract(baseTotal).max(BigDecimal.ZERO);
                return null;

            case "TOTAL_AC_ENERGY": return r.getTotalAcActiveEnergyKwh();
            case "TOTAL_AC_EXPORT": return r.getTotalAcActiveExportEnergyKwh();
            case "TOTAL_AC_IMPORT": return r.getTotalAcActiveImportEnergyKwh();
            case "TOTAL_DC_ENERGY": return r.getTotalDcEnergyKwh();
            default: return null;
        }
    }
}
