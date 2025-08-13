// src/main/java/com/legakrishi/solar/controller/ChartDataController.java
package com.legakrishi.solar.controller;

import com.legakrishi.solar.model.MeterKind;
import com.legakrishi.solar.repository.DeviceRepository;
import com.legakrishi.solar.service.ChartSeriesService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/admin/charts")
public class ChartDataController {

    private final ChartSeriesService charts;
    private final DeviceRepository deviceRepo;

    public ChartDataController(ChartSeriesService charts, DeviceRepository deviceRepo) {
        this.charts = charts;
        this.deviceRepo = deviceRepo;
    }

    // ✅ used to print friendly meter labels in legends
    @GetMapping("/meter-labels")
    public Map<String, String> meterLabels(@RequestParam Long siteId) {
        Map<String, String> out = new LinkedHashMap<>();
        for (MeterKind mk : MeterKind.values()) {
            String base = switch (mk) { case MAIN -> "MAIN"; case STANDBY -> "STANDBY"; case CHECK -> "CHECK"; };
            String label = base;
            deviceRepo.findFirstBySiteIdAndDefaultMeterKind(siteId, mk).ifPresent(d -> {
                String suffix = (d.getType()!=null? d.getType() : "Device") + " #" + d.getId();
                out.put(mk.name(), base + " — " + suffix);
            });
            out.putIfAbsent(mk.name(), label);
        }
        return out;
    }

    // ✅ intraday time-view
    @GetMapping("/intraday")
    public Map<String,Object> intraday(@RequestParam Long siteId,
                                       @RequestParam(required = false) String day,
                                       @RequestParam(defaultValue = "TOTAL_AC_POWER") String metric,
                                       @RequestParam(defaultValue = "1") Integer stepMin,
                                       @RequestParam(name = "demo", defaultValue = "false") boolean demo) {
        if (demo) {
            return demoIntraday(metric, stepMin == null ? 1 : stepMin);
        }
        LocalDate d = (day != null && !day.isBlank()) ? LocalDate.parse(day) : null;
        return charts.buildIntradayMeterSeries(siteId, d, metric, stepMin == null ? 1 : stepMin);
    }
    // 🔹 Optional DEMO for daily meter-wise
    @GetMapping("/day-meter-series")
    public Map<String, Object> dayMeterSeries(@RequestParam Long siteId,
                                              @RequestParam(defaultValue = "30") int days,
                                              @RequestParam(defaultValue = "false") boolean demo) {
        if (demo) {
            return demoDaily(days);
        }
        return charts.buildDayMeterSeries(siteId, days);
    }
    // ---------- DEMO helpers ----------
    private Map<String,Object> demoIntraday(String metric, int stepMin){
        int points = (int) Math.round((24*60.0) / Math.max(stepMin, 1)) + 1;
        List<String> labels = new ArrayList<>(points);
        java.time.LocalTime t = java.time.LocalTime.MIDNIGHT;
        for (int i=0;i<points;i++){
            labels.add(t.toString().substring(0,5));
            t = t.plusMinutes(stepMin);
        }
        // simple “sun” curve
        double[] curve = new double[points];
        for (int i=0;i<points;i++){
            double hour = (i*stepMin)/60.0;
            // 6:00..18:00 bell-ish curve up to ~40
            double v = Math.max(0, Math.sin((hour-6)/12.0*Math.PI))*40.0;
            curve[i] = Math.round(v*10.0)/10.0;
        }
        Map<String,Object> out = new HashMap<>();
        out.put("labels", labels);
        out.put("metric", metric);
        out.put("unit", metric.contains("POWER") ? "kW" : "kWh");
        Map<String,Object> series = new HashMap<>();
        series.put("MAIN", toList(curve));
        series.put("STANDBY", toList(scale(curve, 0.12)));
        series.put("CHECK", toList(scale(curve, 0.08)));
        out.put("series", series);
        return out;
    }
    private Map<String,Object> demoDaily(int days){
        LocalDate today = LocalDate.now();
        DateTimeFormatter df = DateTimeFormatter.ISO_DATE;
        List<String> labels = new ArrayList<>(days);
        for (int i=days-1;i>=0;i--) labels.add(today.minusDays(i).format(df));

        double[] base = new double[days];
        for (int i=0;i<days;i++){
            // weekly undulation 20..60
            base[i] = Math.round((40 + 20*Math.sin(i/3.0)) * 10.0)/10.0;
        }
        Map<String,Object> series = new HashMap<>();
        Map<String,Object> g1 = new HashMap<>();
        Map<String,Object> g2 = new HashMap<>();
        Map<String,Object> g3 = new HashMap<>();
        Map<String,Object> g4 = new HashMap<>();
        Map<String,Object> g5 = new HashMap<>();

        g1.put("MAIN",    toList(base));
        g1.put("STANDBY", toList(scale(base, 0.10)));
        g1.put("CHECK",   toList(scale(base, 0.06)));

        g2.put("MAIN",    toList(scale(base, 0.7)));
        g2.put("STANDBY", toList(scale(base, 0.02)));
        g2.put("CHECK",   toList(scale(base, 0.01)));

        g3.put("MAIN",    toList(scale(base, 0.05)));
        g3.put("STANDBY", toList(scale(base, 0.02)));
        g3.put("CHECK",   toList(scale(base, 0.01)));

        g4.put("MAIN",    toList(scale(base, 0.8)));
        g4.put("STANDBY", toList(scale(base, 0.03)));
        g4.put("CHECK",   toList(scale(base, 0.02)));

        g5.put("MAIN",    toList(scale(base, 0.9)));
        g5.put("STANDBY", toList(scale(base, 0.05)));
        g5.put("CHECK",   toList(scale(base, 0.03)));

        series.put("acActiveEnergyKwh", g1);
        series.put("acExportEnergyKwh", g2);
        series.put("acImportEnergyKwh", g3);
        series.put("dcEnergyKwh",       g4);
        series.put("maxAcPowerKw",      g5);

        Map<String,Object> out = new HashMap<>();
        out.put("labels", labels);
        out.put("series", series);
        return out;
    }
    private static List<Double> toList(double[] arr){
        List<Double> L = new ArrayList<>(arr.length);
        for (double v: arr) L.add(v);
        return L;
    }
    private static double[] scale(double[] src, double f){
        double[] r = new double[src.length];
        for (int i=0;i<src.length;i++) r[i] = Math.round(src[i]*f*10.0)/10.0;
        return r;
    }
    }
