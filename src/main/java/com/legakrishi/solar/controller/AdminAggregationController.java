package com.legakrishi.solar.controller;

import com.legakrishi.solar.model.ReadingDay;
import com.legakrishi.solar.service.AggregationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.legakrishi.solar.model.MeterKind;
import java.time.LocalDate;

@RestController
@RequestMapping("/admin/agg")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAggregationController {

    private final AggregationService service;

    public AdminAggregationController(AggregationService service) {
        this.service = service;
    }

    // Simple GET for testing (no CSRF issues). Use while logged in as ADMIN.
    @GetMapping("/today")
    public String aggregateToday(@RequestParam(defaultValue = "1") Long siteId) {
        ReadingDay rd = service.aggregateTodayForSite(siteId);
        return "OK: site=" + siteId +
                ", day=" + rd.getDay() +
                ", energyTodayKwh=" + rd.getEnergyTodayKwh() +
                ", maxPowerKw=" + rd.getMaxPowerKw();
    }
    // Example: /admin/aggregation/day-meter?siteId=1&day=2025-08-10&meter=MAIN
    @GetMapping("/day-meter")
    public String aggregateDayMeter(@RequestParam Long siteId,
                                    @RequestParam String day,
                                    @RequestParam(defaultValue = "MAIN") MeterKind meter) {
        LocalDate d = LocalDate.parse(day); // yyyy-MM-dd
        var r = service.aggregateDayPerMeter(siteId, d, meter);
        return "OK: site=" + siteId + ", day=" + r.getDay() + ", meter=" + r.getMeterKind()
                + " | AC=" + r.getAcActiveEnergyKwh()
                + " kWh, Export=" + r.getAcExportEnergyKwh()
                + " kWh, Import=" + r.getAcImportEnergyKwh()
                + " kWh, DC=" + r.getDcEnergyKwh()
                + " kWh, MaxP=" + r.getMaxAcPowerKw() + " kW";
    }
}
