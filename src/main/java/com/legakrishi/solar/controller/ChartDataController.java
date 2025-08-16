package com.legakrishi.solar.controller;

import com.legakrishi.solar.model.MeterKind;
import com.legakrishi.solar.repository.DeviceRepository;
import com.legakrishi.solar.service.ChartSeriesService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/meter-labels")
    public Map<String, String> meterLabels(@RequestParam Long siteId) {
        Map<String, String> out = new LinkedHashMap<>();
        for (MeterKind mk : MeterKind.values()) {
            String base = switch (mk) { case MAIN -> "MAIN"; case STANDBY -> "STANDBY"; case CHECK -> "CHECK"; };
            deviceRepo.findFirstBySiteIdAndDefaultMeterKind(siteId, mk).ifPresentOrElse(d -> {
                String type = (d.getType() != null && !d.getType().isBlank()) ? d.getType() : "Device";
                out.put(mk.name(), base + " — " + type + " #" + d.getId());
            }, () -> out.put(mk.name(), base));
        }
        return out;
    }

    @GetMapping("/intraday")
    public Map<String,Object> intraday(@RequestParam Long siteId,
                                       @RequestParam(required = false) String day,
                                       @RequestParam(defaultValue = "TOTAL_AC_POWER") String metric,
                                       @RequestParam(defaultValue = "1") Integer stepMin) {
        LocalDate d = (day != null && !day.isBlank()) ? LocalDate.parse(day) : null;
        return charts.buildIntradayMeterSeries(siteId, d, metric, stepMin == null ? 1 : stepMin);
    }

    @GetMapping("/day-meter-series")
    public Map<String, Object> dayMeterSeries(@RequestParam Long siteId,
                                              @RequestParam(defaultValue = "30") int days) {
        return charts.buildDailyMeterSeries(siteId, days);
    }
}
