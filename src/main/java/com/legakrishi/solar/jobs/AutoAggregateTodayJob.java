package com.legakrishi.solar.jobs;

import com.legakrishi.solar.config.MonitoringProps;
import com.legakrishi.solar.service.AggregationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AutoAggregateTodayJob {

    private static final Logger log = LoggerFactory.getLogger(AutoAggregateTodayJob.class);

    private final AggregationService aggregationService;
    private final MonitoringProps props; // ← read siteId from application.properties

    // runs every 5 minutes
    @Scheduled(initialDelay = 10_000, fixedDelay = 300_000)
    public void run() {
        try {
            Long siteId = props.getSiteId();
            var rd = aggregationService.aggregateTodayForSite(siteId);
            log.info("Aggregated today: site={} day={} energyTodayKwh={} maxPowerKw={}",
                    siteId, rd.getDay(), rd.getEnergyTodayKwh(), rd.getMaxPowerKw());
        } catch (Exception e) {
            log.warn("Auto aggregate failed: {}", e.getMessage());
        }
    }
}
