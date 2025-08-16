// src/main/java/com/legakrishi/solar/rms/RmsServiceImpl.java
package com.legakrishi.solar.rms;

import com.legakrishi.solar.rms.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class RmsServiceImpl implements RmsService {

    /**
     * When true, we return demo data so the UI is never blank.
     * Flip to false once you wire real repositories.
     */
    @Value("${rms.demo:true}")
    private boolean demo;

    @Override
    public RmsSummary getSummary(Long siteId) {
        if (demo) {
            return new RmsSummary(
                    81.3, 19.6, 4123.5, 58421.2, 98.7,
                    Instant.now()
            );
        }
        // TODO: replace with real computation:
        // - PR = (actual energy / (irradiance * arrayRatedPower * performanceFactors)) * 100
        // - CUF = (energyToday / (PlantCapacity_kW * 24)) * 100
        // - availability from device uptime
        // - energyToday/MTD from meter/inverter energy tables
        return new RmsSummary(0.0, 0.0, 0.0, 0.0, 0.0, Instant.now());
    }

    @Override
    public RmsDevicesStatus getDevicesStatus(Long siteId) {
        if (demo) {
            return new RmsDevicesStatus(
                    7, 1,
                    Instant.now().minusSeconds(7 * 60),
                    3
            );
        }
        // TODO: fetch last packet timestamps & online flags per device
        return new RmsDevicesStatus(0, 0, null, 0);
    }

    @Override
    public List<RmsAlarm> listAlarms(Long siteId, int limit) {
        if (demo) {
            return List.of(
                    new RmsAlarm(Instant.now().minusSeconds(5 * 60),  "INV-03", "MAJOR",   "DC Disparity"),
                    new RmsAlarm(Instant.now().minusSeconds(25 * 60), "SMB-12", "MINOR",   "String Low Current"),
                    new RmsAlarm(Instant.now().minusSeconds(55 * 60), "MTR-01", "CRITICAL","Meter Comm Lost")
            ).subList(0, Math.min(limit, 3));
        }
        // TODO: query recent active alarms
        return List.of();
    }
}
