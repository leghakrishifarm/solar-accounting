package com.legakrishi.solar.controller;

import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/admin/charts")
public class ChartsStubController {

    // NOTE: different path to avoid collision with ChartDataController
    @GetMapping("/intraday-stub")
    public Map<String, Object> intradayStub(
            @RequestParam Long siteId,
            @RequestParam String day,
            @RequestParam String metric,
            @RequestParam(defaultValue = "1") int stepMin
    ) {
        List<String> labels = Arrays.asList("10:00","10:10","10:20","10:30","10:40","10:50","11:00");

        Map<String, Object> series = new LinkedHashMap<>();
        series.put("MAIN",    Arrays.asList(0.0, 1.2, 2.5, 3.1, 2.2, 1.0, 0.0));
        series.put("STANDBY", Arrays.asList(0,0,0,0,0,0,0));
        series.put("CHECK",   Arrays.asList(0,0,0,0,0,0,0));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("labels", labels);
        body.put("metric", metric);
        body.put("unit", metric.contains("POWER") ? "kW" : "kWh");
        body.put("series", series);
        return body;
    }
}
