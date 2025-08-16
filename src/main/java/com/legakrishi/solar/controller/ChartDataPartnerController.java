package com.legakrishi.solar.controller;

import com.legakrishi.solar.service.ChartSeriesService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/partners/charts")
@PreAuthorize("hasRole('PARTNER')")
public class ChartDataPartnerController {

    private final ChartSeriesService charts;

    public ChartDataPartnerController(ChartSeriesService charts) {
        this.charts = charts;
    }

    // e.g. GET /partners/charts/daily?siteId=1&days=7
    @GetMapping("/daily")
    public Map<String, Object> daily(@RequestParam Long siteId,
                                     @RequestParam(defaultValue = "7") int days) {
        // FIX: call the correct service method name
        return charts.buildDailyMeterSeries(siteId, days);
    }

    // e.g. GET /partners/charts/intraday?siteId=1&date=2025-08-15&metric=TOTAL_AC_POWER&stepMin=15
    @GetMapping("/intraday")
    public Map<String, Object> intraday(@RequestParam Long siteId,
                                        @RequestParam(required = false)
                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                        LocalDate date,
                                        @RequestParam(defaultValue = "TOTAL_AC_POWER") String metric,
                                        @RequestParam(defaultValue = "15") Integer stepMin) {
        return charts.buildIntradayMeterSeries(siteId, date, metric, stepMin);
    }
}
