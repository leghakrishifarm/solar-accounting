package com.legakrishi.solar.controller;

import com.legakrishi.solar.model.MeterKind;
import com.legakrishi.solar.repository.DeviceRepository;
import com.legakrishi.solar.service.ChartSeriesService;
import com.legakrishi.solar.service.PartnerAccessService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@PreAuthorize("hasRole('PARTNER')")
@RequestMapping("/partners/charts")
public class ChartDataPartnerController {

    private final ChartSeriesService charts;
    private final DeviceRepository deviceRepo;
    private final PartnerAccessService access;

    public ChartDataPartnerController(ChartSeriesService charts,
                                      DeviceRepository deviceRepo,
                                      PartnerAccessService access) {
        this.charts = charts;
        this.deviceRepo = deviceRepo;
        this.access = access;
    }

    @GetMapping("/day-meter-series")
    public Map<String, Object> dayMeterSeries(@RequestParam Long siteId,
                                              @RequestParam(defaultValue = "30") int days) {
        access.assertHasSite(siteId);
        return charts.buildDayMeterSeries(siteId, days);
    }

    @GetMapping("/meter-labels")
    public Map<String, String> meterLabels(@RequestParam Long siteId) {
        access.assertHasSite(siteId);
        Map<String, String> out = new LinkedHashMap<>();
        for (var mk : MeterKind.values()) {
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

    @GetMapping("/intraday")
    public Map<String,Object> intraday(@RequestParam Long siteId,
                                       @RequestParam(required = false) String day,
                                       @RequestParam(defaultValue = "DAILY_AC_IMPORT") String metric,
                                       @RequestParam(defaultValue = "10") Integer stepMin) {
        access.assertHasSite(siteId);
        LocalDate d = (day != null && !day.isBlank()) ? LocalDate.parse(day) : null;
        return charts.buildIntradayMeterSeries(siteId, d, metric, stepMin);
    }
}
