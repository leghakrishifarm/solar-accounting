package com.legakrishi.solar.jobs;

import com.legakrishi.solar.config.MonitoringProps;
import com.legakrishi.solar.model.AlertEvent;
import com.legakrishi.solar.repository.AlertEventRepository;
import com.legakrishi.solar.repository.DeviceRepository;
import com.legakrishi.solar.repository.ReadingRepository;
import com.legakrishi.solar.service.AlertEmailService;
import com.legakrishi.solar.service.AlertNotifyService;
import com.legakrishi.solar.service.WhatsAppService; // <-- NEW
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.DoubleSummaryStatistics;

@Component
@RequiredArgsConstructor
public class ZeroPowerAlertJob {

    private final DeviceRepository deviceRepo;
    private final ReadingRepository readingRepo;
    private final AlertEventRepository alertRepo;
    private final MonitoringProps props;
    private final AlertNotifyService notifier;
    private final AlertEmailService emailer;
    private final WhatsAppService whatsapp; // <-- NEW

    private static final ZoneId TZ = ZoneId.of("Asia/Kolkata");
    private static final LocalTime DEF_START = LocalTime.of(9, 0);
    private static final LocalTime DEF_END   = LocalTime.of(17, 0);

    @Scheduled(initialDelay = 45_000, fixedDelay = 120_000)
    @Transactional
    public void run() {
        Long deviceId = props.getDeviceId();
        var dOpt = deviceRepo.findById(deviceId);
        if (dOpt.isEmpty()) return;
        var d = dOpt.get();

        var now = LocalDateTime.now(TZ);

        // skip if offline; OfflineAlertJob handles that
        if (d.getLastSeen() == null
                || Duration.between(d.getLastSeen(), now).toMinutes() > props.getOfflineThresholdMinutes()) {
            return;
        }

        var site = d.getSite();
        LocalTime startWin = DEF_START;
        LocalTime endWin   = DEF_END;
        if (site != null) {
            if (site.getDaylightStart() != null) startWin = site.getDaylightStart();
            if (site.getDaylightEnd()   != null) endWin   = site.getDaylightEnd();
        }

        var lt = now.toLocalTime();
        if (lt.isBefore(startWin) || lt.isAfter(endWin)) return;

        double thresholdKw = props.getZeroThresholdKw();
        if (site != null && site.getZeroThresholdKw() != null) {
            thresholdKw = site.getZeroThresholdKw();
        }

        var siteId = site.getId();
        var start = now.minusMinutes(props.getZeroWindowMinutes());
        var list = readingRepo.findRange(siteId, start, now);
        if (list.size() < props.getZeroMinReadings()) return;

        DoubleSummaryStatistics stats = list.stream()
                .map(r -> r.getPowerKw() == null ? 0.0 : r.getPowerKw())
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();

        boolean zeroPower = stats.getMax() <= thresholdKw;
        if (!zeroPower) return;

        // avoid duplicates for 30 minutes
        long recent = alertRepo.countByDeviceIdAndTypeAndTriggeredAtAfter(
                deviceId, "ZERO_POWER", now.minusMinutes(30));
        if (recent > 0) return;

        var alert = AlertEvent.builder()
                .site(site)
                .device(d)
                .type("ZERO_POWER")
                .message("Zero power for ~" + props.getZeroWindowMinutes()
                        + " min during daylight (≤ " + String.format(java.util.Locale.US, "%.2f", thresholdKw) + " kW)")
                .triggeredAt(now)
                .acknowledged(false)
                .build();

        alertRepo.save(alert);

        // existing notifiers
        notifier.notifyIfConfigured(alert);
        emailer.sendIfConfigured(alert);

        // NEW: WhatsApp push (uses whatsapp.* props; null = default recipient)
        whatsapp.sendAlert(alert, null);
    }
}
