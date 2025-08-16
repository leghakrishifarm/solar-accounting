package com.legakrishi.solar.config;

import com.legakrishi.solar.model.EnergySample;
import com.legakrishi.solar.model.MeterKind;
import com.legakrishi.solar.repository.EnergySampleRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    ApplicationRunner seedEnergySamples(EnergySampleRepository repo) {
        return args -> {
            final Long siteId = 1L;

            ZoneId zone = ZoneId.systemDefault();
            LocalDate day = LocalDate.now(zone);
            ZonedDateTime startZ = day.atStartOfDay(zone);

            List<EnergySample> batch = new ArrayList<>();

            // seed every 15 minutes
            for (int minute = 0; minute < 24 * 60; minute += 15) {
                Instant sampleTime = startZ.plusMinutes(minute).toInstant();

                for (MeterKind mk : MeterKind.values()) {
                    // skip if already present (per site,time,kind)
                    if (repo.existsBySiteIdAndSampleTimeAndMeterKind(siteId, sampleTime, mk)) {
                        continue;
                    }

                    double daylightFactor = Math.max(0,
                            Math.sin(Math.PI * (minute / 1440.0))); // 0..1 over the day

                    EnergySample s = new EnergySample();
                    s.setSiteId(siteId);
                    s.setMeterKind(mk);
                    s.setSampleTime(sampleTime);
                    s.setTotalAcPowerKw(50.0 * daylightFactor);
                    s.setDailyAcEnergyKwh(5.0 * daylightFactor);
                    s.setDailyAcExportKwh(3.0 * daylightFactor);
                    s.setDailyAcImportKwh(0.5 * (1.0 - daylightFactor));
                    s.setDailyDcEnergyKwh(4.5 * daylightFactor);

                    batch.add(s);
                }
            }

            if (!batch.isEmpty()) {
                repo.saveAll(batch);
            }
        };
    }
}
