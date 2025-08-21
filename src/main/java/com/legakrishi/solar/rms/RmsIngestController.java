package com.legakrishi.solar.rms;

import com.legakrishi.solar.model.EnergySample;
import com.legakrishi.solar.model.MeterKind;
import com.legakrishi.solar.repository.EnergySampleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/rms/ingest")
public class RmsIngestController {

    private final EnergySampleRepository repo;
    private final SimpMessagingTemplate broker;

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    public RmsIngestController(EnergySampleRepository repo, SimpMessagingTemplate broker) {
        this.repo = repo;
        this.broker = broker;
    }

    // Simple DTO for RMS payload
    public static class Payload {
        public Long siteId;
        public String meterKind;          // "MAIN" | "STANDBY" | "CHECK"
        public String sampleTime;         // ISO-8601, e.g. "2025-08-16T13:45:00Z"
        public Double totalAcPowerKw;
        public Double dailyAcEnergyKwh;
        public Double dailyAcExportKwh;
        public Double dailyAcImportKwh;
        public Double dailyDcEnergyKwh;
    }

    @PostMapping("/energy")
    public ResponseEntity<?> ingest(@RequestBody Payload p) {
        if (p == null || p.siteId == null || p.meterKind == null || p.sampleTime == null) {
            return ResponseEntity.badRequest().body("siteId, meterKind, sampleTime required");
        }

        // Parse meter kind
        final MeterKind mk;
        try {
            mk = MeterKind.valueOf(p.meterKind.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body("unknown meterKind: " + p.meterKind);
        }

        // Parse timestamp (UTC ISO-8601)
        final Instant ts;
        try {
            ts = Instant.parse(p.sampleTime);
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("invalid sampleTime: " + p.sampleTime);
        }

        // Idempotent insert (skip if exists)
        if (repo.existsBySiteIdAndSampleTimeAndMeterKind(p.siteId, ts, mk)) {
            // Optional: still publish a tick so UI stays fresh — uncomment if desired
            // publishTickAtOrBefore(p.siteId, ts);
            return ResponseEntity.status(HttpStatus.CONFLICT).body("duplicate sample");
        }

        // Persist the sample
        EnergySample s = new EnergySample();
        s.setSiteId(p.siteId);
        s.setMeterKind(mk);
        s.setSampleTime(ts);
        s.setTotalAcPowerKw(p.totalAcPowerKw);
        s.setDailyAcEnergyKwh(p.dailyAcEnergyKwh);
        s.setDailyAcExportKwh(p.dailyAcExportKwh);
        s.setDailyAcImportKwh(p.dailyAcImportKwh);
        s.setDailyDcEnergyKwh(p.dailyDcEnergyKwh);
        repo.save(s);

        // Publish a consolidated tick at this time (latest <= ts per meter)
        publishTickAtOrBefore(p.siteId, ts);

        return ResponseEntity.accepted().build();
    }

    /**
     * Build and publish a tick with the latest (<= ts) power value for each meter.
     * Keys in the outgoing map are STRING names ("MAIN", "STANDBY", "CHECK") for the UI.
     */
    private void publishTickAtOrBefore(Long siteId, Instant ts) {
        // Collect latest value per meter using enum-keyed map (type-safe)
        Map<MeterKind, Double> perMeterEnum = new EnumMap<>(MeterKind.class);
        for (MeterKind kind : MeterKind.values()) {
            Optional<EnergySample> opt = repo.findTopBySiteIdAndMeterKindAndSampleTimeLessThanEqualOrderBySampleTimeDesc(
                    siteId, kind, ts);
            Double v = opt.map(EnergySample::getTotalAcPowerKw).orElse(0.0);
            perMeterEnum.put(kind, v == null ? 0.0 : v);
        }

        // Convert to String-keyed map for payload (what the UI expects)
        Map<String, Double> perMeterOut = new LinkedHashMap<>();
        perMeterEnum.forEach((k, v) -> perMeterOut.put(k.name(), v));

        // Label = local HH:mm for the tick time
        String label = ZonedDateTime.ofInstant(ts, ZoneId.systemDefault()).format(HHMM);

        // Publish
        broker.convertAndSend("/topic/intraday/" + siteId,
                new com.legakrishi.solar.ws.IntradayTick(label, perMeterOut));
    }
}
