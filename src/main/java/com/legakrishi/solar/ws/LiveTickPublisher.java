package com.legakrishi.solar.ws;

import com.legakrishi.solar.model.EnergySample;
import com.legakrishi.solar.model.MeterKind;
import com.legakrishi.solar.repository.EnergySampleRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class LiveTickPublisher {

    private final SimpMessagingTemplate broker;
    private final EnergySampleRepository repo;

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
    private static final long SITE_ID = 1L;

    public LiveTickPublisher(SimpMessagingTemplate broker, EnergySampleRepository repo) {
        this.broker = broker;
        this.repo = repo;
    }

    // Broadcast a tick every 15 seconds to topic for site 1
    @Scheduled(fixedRate = 15000, initialDelay = 3000)
    public void sendTick() {
        ZoneId zone = ZoneId.systemDefault();
        Instant now = Instant.now();

        Map<String, Double> perMeter = new HashMap<>();
        Instant latestSeen = null;

        // pull the latest sample PER meter kind, but not after "now"
        for (MeterKind mk : MeterKind.values()) {
            Optional<EnergySample> opt =
                    repo.findTopBySiteIdAndMeterKindAndSampleTimeLessThanEqualOrderBySampleTimeDesc(
                            SITE_ID, mk, now
                    );

            Double power = opt.map(EnergySample::getTotalAcPowerKw).orElse(0.0);
            perMeter.put(mk.name(), nz(power));

            if (opt.isPresent()) {
                Instant t = opt.get().getSampleTime();
                if (latestSeen == null || t.isAfter(latestSeen)) latestSeen = t;
            }
        }

        // label = time of the latest sample we saw (or "now" if none)
        String label = (latestSeen != null)
                ? ZonedDateTime.ofInstant(latestSeen, zone).format(HHMM)
                : ZonedDateTime.now(zone).format(HHMM);

        broker.convertAndSend("/topic/intraday/" + SITE_ID, new IntradayTick(label, perMeter));
    }

    private static double nz(Double v) { return v == null ? 0d : v; }
}
