package com.legakrishi.solar.controller;

import com.legakrishi.solar.service.ChartSeriesService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/partners/charts")
@PreAuthorize("hasRole('PARTNER')")
public class PartnerChartsController {

    private final ChartSeriesService charts;

    public PartnerChartsController(ChartSeriesService charts) {
        this.charts = charts;
    }

    // Used by the partner page JS: /partners/charts/day-meter-series?siteId=...&days=...
    @GetMapping("/day-meter-series")
    public Map<String, Object> dayMeterSeries(@RequestParam("siteId") Long siteId,
                                              @RequestParam(value = "days", defaultValue = "30") int days) {
        return charts.buildDailyMeterSeries(siteId, days);
    }

    // Optional labels endpoint (MAIN/STANDBY/CHECK by default)
    @GetMapping("/meter-labels")
    public Map<String, String> meterLabels(@RequestParam("siteId") Long siteId) {
        Map<String, String> out = new LinkedHashMap<>();
        out.put("MAIN", "MAIN");
        out.put("STANDBY", "STANDBY");
        out.put("CHECK", "CHECK");
        return out;
    }
}
