package com.legakrishi.solar.controller;

import com.legakrishi.solar.model.EnergySample;
import com.legakrishi.solar.model.MeterKind;
import com.legakrishi.solar.repository.EnergySampleRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController // (defaults to "ingestController")
@RequestMapping("/ingest")
public class IngestController {

    private final EnergySampleRepository repo;

    public IngestController(EnergySampleRepository repo) {
        this.repo = repo;
    }

    @PostMapping("/ingest")
    public ResponseEntity<?> ingest(@RequestBody List<SampleIn> payload,
                                    @RequestHeader(value = "X-API-Key", required = false) String apiKey) {
        if (payload == null || payload.isEmpty()) return ResponseEntity.badRequest().body("empty payload");

        for (SampleIn p : payload) {
            EnergySample s = new EnergySample();
            s.setSiteId(p.siteId);
            s.setMeterKind(p.meterKind);
            s.setSampleTime(p.sampleTimeUtc != null ? p.sampleTimeUtc : Instant.now());

            s.setTotalAcPowerKw(n(p.totalAcPowerKw));
            s.setDailyAcEnergyKwh(n(p.dailyAcEnergyKwh));
            s.setDailyAcExportKwh(n(p.dailyAcExportKwh));
            s.setDailyAcImportKwh(n(p.dailyAcImportKwh));
            s.setDailyDcEnergyKwh(n(p.dailyDcEnergyKwh));

            s.setDeviceId(p.deviceId);
            s.setFirmware(p.firmware);

            repo.save(s);
        }
        return ResponseEntity.ok().build();
    }

    private static Double n(Double v){ return v == null ? 0d : v; }

    // DTO for ingest
    public static class SampleIn {
        public Long siteId;
        public MeterKind meterKind;     // MAIN/STANDBY/CHECK
        public Instant sampleTimeUtc;   // ISO timestamp

        public Double totalAcPowerKw;
        public Double dailyAcEnergyKwh;
        public Double dailyAcExportKwh;
        public Double dailyAcImportKwh;
        public Double dailyDcEnergyKwh;

        public String deviceId;
        public String firmware;
    }
}
