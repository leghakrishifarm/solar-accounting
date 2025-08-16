package com.legakrishi.solar.iot.dto;

import com.legakrishi.solar.model.MeterKind;

import java.time.Instant;

/**
 * One telemetry point from a device.
 * Send a JSON array of these to /api/iot/ingest
 */
public record IngestPoint(
        Long siteId,
        MeterKind meter,         // "MAIN" | "STANDBY" | "CHECK"
        Instant sampleTimeUtc,   // optional; if null, server will set now()

        // instantaneous / totals / daily counters
        Double totalAcPowerKw,
        Double totalAcEnergyKwh,
        Double dailyAcEnergyKwh,

        Double dailyAcExportKwh,
        Double totalAcExportKwh,

        Double dailyAcImportKwh,
        Double totalAcImportKwh,

        Double dailyDcEnergyKwh,
        Double totalDcEnergyKwh,

        // optional device metadata
        Long deviceId,
        String firmware
) {}
