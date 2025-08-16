// src/main/java/com/legakrishi/solar/service/ChartSeriesService.java
package com.legakrishi.solar.service;

import java.time.LocalDate;
import java.util.Map;

public interface ChartSeriesService {

    Map<String,Object> buildIntradayMeterSeries(Long siteId, LocalDate day, String metric, int stepMin);
    Map<String,Object> buildDailyMeterSeries(Long siteId, int days);

    /** Intraday (time-view) aligned to the chosen step in minutes. */
    Map<String, Object> buildIntradayMeterSeries(Long siteId,
                                                 LocalDate day,
                                                 String metric,
                                                 Integer stepMin);
}
