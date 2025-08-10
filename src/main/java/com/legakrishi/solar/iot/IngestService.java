package com.legakrishi.solar.iot;

import com.legakrishi.solar.iot.dto.IngestPayload;
import com.legakrishi.solar.model.Device;
import com.legakrishi.solar.model.Reading;
import com.legakrishi.solar.repository.DeviceRepository;
import com.legakrishi.solar.repository.ReadingRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import com.legakrishi.solar.model.MeterKind;
import java.math.BigDecimal;

@Service
public class IngestService {

    private final DeviceRepository deviceRepo;
    private final ReadingRepository readingRepo;

    public IngestService(DeviceRepository deviceRepo, ReadingRepository readingRepo) {
        this.deviceRepo = deviceRepo;
        this.readingRepo = readingRepo;
    }

    @Transactional
    public void ingest(IngestPayload p) {
        if (p.getDeviceId() == null || p.getDeviceToken() == null) {
            throw new IllegalArgumentException("deviceId and deviceToken are required");
        }

        Device d = deviceRepo.findById(p.getDeviceId())
                .filter(Device::getActive)
                .filter(dev -> dev.getApiToken().equals(p.getDeviceToken()))
                .orElseThrow(() -> new SecurityException("Invalid device or token"));

        // Timestamp in Asia/Kolkata (matches your project)
        LocalDateTime ts = (p.getTsEpoch() != null)
                ? LocalDateTime.ofInstant(Instant.ofEpochSecond(p.getTsEpoch()), ZoneId.of("Asia/Kolkata"))
                : LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        // Default to MAIN when not provided
        MeterKind kind = p.getMeter();
        if (kind == null && d.getDefaultMeterKind() != null) {
            kind = d.getDefaultMeterKind();
        }
        if (kind == null) {
            kind = MeterKind.MAIN; // final fallback
        }

        Reading r = Reading.builder()
                .site(d.getSite())
                .device(d)
                .ts(ts)
                // NEW: which meter
                .meterKind(kind)

                .powerKw(p.getPowerKw())
                .energyKwh(p.getEnergyKwh())
                .dcVoltage(p.getDcVoltage())
                .dcCurrent(p.getDcCurrent())
                .acVoltage(p.getAcVoltage())
                .acCurrent(p.getAcCurrent())
                .temperature(p.getTemperature())
                .status(p.getStatus())

                // NEW: TracKSo-like metrics (nullable is OK)
                .totalAcActivePowerKw(p.getTotalAcActivePowerKw())
                .totalAcActiveEnergyKwh(p.getTotalAcActiveEnergyKwh())
                .dailyAcActiveEnergyKwh(p.getDailyAcActiveEnergyKwh())
                .dailyAcActiveExportEnergyKwh(p.getDailyAcActiveExportEnergyKwh())
                .totalAcActiveExportEnergyKwh(p.getTotalAcActiveExportEnergyKwh())
                .dailyAcActiveImportEnergyKwh(p.getDailyAcActiveImportEnergyKwh())
                .totalAcActiveImportEnergyKwh(p.getTotalAcActiveImportEnergyKwh())
                .dailyDcEnergyKwh(p.getDailyDcEnergyKwh())
                .totalDcEnergyKwh(p.getTotalDcEnergyKwh())

                .build();

        readingRepo.save(r);
        deviceRepo.touchLastSeen(d.getId());
    }
}
