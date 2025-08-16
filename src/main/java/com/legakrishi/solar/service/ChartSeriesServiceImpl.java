package com.legakrishi.solar.service;

import com.legakrishi.solar.model.EnergySample;
import com.legakrishi.solar.model.MeterKind;
import com.legakrishi.solar.repository.EnergySampleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ChartSeriesServiceImpl implements ChartSeriesService {

    private final EnergySampleRepository repo;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public ChartSeriesServiceImpl(EnergySampleRepository repo) {
        this.repo = repo;
    }

    // === Interface method (Integer) ===
    @Override
    public Map<String, Object> buildIntradayMeterSeries(Long siteId,
                                                        LocalDate localDay,
                                                        String metric,
                                                        Integer stepMin) {
        // Delegate to the primitive overload after normalizing null
        int step = (stepMin == null || stepMin <= 0) ? 1 : stepMin;
        return buildIntradayMeterSeries(siteId, localDay, metric, step);
    }

    // === Convenience overload (primitive int) ===
    public Map<String, Object> buildIntradayMeterSeries(Long siteId,
                                                        LocalDate localDay,
                                                        String metric,
                                                        int stepMin) {
        if (localDay == null) localDay = LocalDate.now();
        final int step = (stepMin <= 0) ? 1 : stepMin;

        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime startZ = localDay.atStartOfDay(zone);
        ZonedDateTime endZ   = startZ.plusDays(1);
        Instant from = startZ.toInstant();
        Instant to   = endZ.toInstant();

        // Labels at 'step' minutes strictly before end of day
        List<String> labels = new ArrayList<>();
        for (ZonedDateTime t = startZ; t.isBefore(endZ); t = t.plusMinutes(step)) {
            labels.add(TIME_FMT.format(t));
        }

        Map<String, List<Double>> seriesPerMeter = new LinkedHashMap<>();
        for (MeterKind mk : MeterKind.values()) {
            List<EnergySample> samples =
                    repo.findBySiteIdAndMeterKindAndSampleTimeBetweenOrderBySampleTimeAsc(siteId, mk, from, to);

            double[] arr = new double[labels.size()];
            Arrays.fill(arr, 0d);

            for (EnergySample s : samples) {
                if (s.getSampleTime() == null) continue;
                ZonedDateTime localT = s.getSampleTime().atZone(zone);
                if (localT.isBefore(startZ) || !localT.isBefore(endZ)) continue;

                long minutes = Duration.between(startZ, localT).toMinutes();
                int idx = (int) (minutes / step); // floor to bucket
                if (idx < 0 || idx >= arr.length) continue;

                arr[idx] = pickMetricValue(metric, s);
            }

            seriesPerMeter.put(mk.name(), toList(arr));
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("labels", labels);
        out.put("metric", metric);
        out.put("unit", unitFor(metric));
        out.put("series", seriesPerMeter);
        return out;
    }

    @Override
    public Map<String, Object> buildDailyMeterSeries(Long siteId, int days) {
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        int safeDays = Math.max(1, days);
        LocalDate fromDay = today.minusDays(safeDays - 1);

        List<String> labels = new ArrayList<>();
        DateTimeFormatter df = DateTimeFormatter.ISO_DATE;
        for (LocalDate d = fromDay; !d.isAfter(today); d = d.plusDays(1)) {
            labels.add(df.format(d));
        }

        Map<String, Map<String, List<Double>>> series = new LinkedHashMap<>();
        series.put("acActiveEnergyKwh", new LinkedHashMap<>());
        series.put("acExportEnergyKwh", new LinkedHashMap<>());
        series.put("acImportEnergyKwh", new LinkedHashMap<>());
        series.put("dcEnergyKwh",       new LinkedHashMap<>());
        series.put("maxAcPowerKw",      new LinkedHashMap<>());

        for (MeterKind mk : MeterKind.values()) {
            List<Double> dailyAc  = new ArrayList<>();
            List<Double> dailyExp = new ArrayList<>();
            List<Double> dailyImp = new ArrayList<>();
            List<Double> dailyDc  = new ArrayList<>();
            List<Double> maxPower = new ArrayList<>();

            for (LocalDate d = fromDay; !d.isAfter(today); d = d.plusDays(1)) {
                ZonedDateTime z0 = d.atStartOfDay(zone);
                ZonedDateTime z1 = z0.plusDays(1);

                List<EnergySample> window = repo
                        .findBySiteIdAndMeterKindAndSampleTimeBetweenOrderBySampleTimeAsc(
                                siteId, mk, z0.toInstant(), z1.toInstant());

                if (window.isEmpty()) {
                    dailyAc.add(0d); dailyExp.add(0d); dailyImp.add(0d); dailyDc.add(0d); maxPower.add(0d);
                } else {
                    EnergySample last = window.get(window.size() - 1);
                    double maxKw = window.stream()
                            .map(EnergySample::getTotalAcPowerKw)
                            .filter(Objects::nonNull)
                            .mapToDouble(Double::doubleValue)
                            .max().orElse(0d);

                    dailyAc.add(nz(last.getDailyAcEnergyKwh()));
                    dailyExp.add(nz(last.getDailyAcExportKwh()));
                    dailyImp.add(nz(last.getDailyAcImportKwh()));
                    dailyDc.add(nz(last.getDailyDcEnergyKwh()));
                    maxPower.add(maxKw);
                }
            }

            series.get("acActiveEnergyKwh").put(mk.name(), dailyAc);
            series.get("acExportEnergyKwh").put(mk.name(), dailyExp);
            series.get("acImportEnergyKwh").put(mk.name(), dailyImp);
            series.get("dcEnergyKwh").put(mk.name(), dailyDc);
            series.get("maxAcPowerKw").put(mk.name(), maxPower);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("labels", labels);
        out.put("series", series);
        return out;
    }

    /* ------------ helpers ------------ */

    private double pickMetricValue(String metric, EnergySample s) {
        if (metric == null) return nz(s.getTotalAcPowerKw());
        switch (metric.toUpperCase(Locale.ROOT)) {
            case "TOTAL_AC_POWER":
            case "AC_POWER":
            case "POWER":
                return nz(s.getTotalAcPowerKw());
            case "TOTAL_AC_ENERGY":
            case "DAILY_AC_ENERGY":
            case "AC_ENERGY":
                return nz(s.getDailyAcEnergyKwh());
            case "DAILY_AC_EXPORT":
            case "TOTAL_AC_EXPORT":
            case "AC_EXPORT":
                return nz(s.getDailyAcExportKwh());
            case "DAILY_AC_IMPORT":
            case "TOTAL_AC_IMPORT":
            case "AC_IMPORT":
                return nz(s.getDailyAcImportKwh());
            case "DAILY_DC_ENERGY":
            case "TOTAL_DC_ENERGY":
            case "DC_ENERGY":
                return nz(s.getDailyDcEnergyKwh());
            default:
                return nz(s.getTotalAcPowerKw());
        }
    }

    private String unitFor(String metric) {
        if (metric == null) return "kW";
        return metric.toUpperCase(Locale.ROOT).contains("POWER") ? "kW" : "kWh";
    }

    private static double nz(Double v) { return v == null ? 0d : v; }

    private static List<Double> toList(double[] arr) {
        return Arrays.stream(arr).boxed().collect(Collectors.toList());
    }
}
