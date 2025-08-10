package com.legakrishi.solar.jobs;

import com.legakrishi.solar.config.MonitoringProps;
import com.legakrishi.solar.model.AlertEvent;
import com.legakrishi.solar.repository.AlertEventRepository;
import com.legakrishi.solar.repository.DeviceRepository;
import com.legakrishi.solar.service.AlertEmailService;
import com.legakrishi.solar.service.AlertNotifyService;
import com.legakrishi.solar.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;

@Component
@RequiredArgsConstructor
public class OfflineAlertJob {

    private final DeviceRepository deviceRepo;
    private final AlertEventRepository alertRepo;
    private final MonitoringProps props;
    private final AlertNotifyService notifier;
    private final AlertEmailService emailer;
    private final WhatsAppService whatsapp;   // 👈 add WA service

    private static final ZoneId TZ = ZoneId.of("Asia/Kolkata");

    @Scheduled(initialDelay = 30_000, fixedDelay = 120_000)
    @Transactional
    public void run() {
        Long deviceId = props.getDeviceId();
        var dOpt = deviceRepo.findById(deviceId);
        if (dOpt.isEmpty()) return;
        var d = dOpt.get();

        var now = LocalDateTime.now(TZ);

        int thresholdMinutes = props.getOfflineThresholdMinutes();
        var site = d.getSite();
        if (site != null && site.getOfflineThresholdMinutes() != null && site.getOfflineThresholdMinutes() > 0) {
            thresholdMinutes = site.getOfflineThresholdMinutes();
        }

        // lastSeen null → treat as offline (since unknown)
        if (d.getLastSeen() == null) {
            if (recentAlertExists(deviceId, now)) return;

            var alert = buildAlert(d, now, thresholdMinutes, "unknown");
            alertRepo.save(alert);
            notifier.notifyIfConfigured(alert);
            emailer.sendIfConfigured(alert);

            // 🔔 WhatsApp with "since" = unknown
            sendWhatsAppOffline(site, d, null, "unknown");
            return;
        }

        long mins = Duration.between(d.getLastSeen(), now).toMinutes();
        if (mins <= thresholdMinutes) return;
        if (recentAlertExists(deviceId, now)) return;

        var alert = buildAlert(d, now, thresholdMinutes, String.valueOf(mins));
        alertRepo.save(alert);
        notifier.notifyIfConfigured(alert);
        emailer.sendIfConfigured(alert);

        // 🔔 WhatsApp with "since" = lastSeen local time, duration = mins
        sendWhatsAppOffline(site, d, d.getLastSeen(), String.valueOf(mins));
    }

    private boolean recentAlertExists(Long deviceId, LocalDateTime now) {
        return alertRepo.countByDeviceIdAndTypeAndTriggeredAtAfter(
                deviceId, "OFFLINE", now.minusMinutes(30)) > 0;
    }

    private AlertEvent buildAlert(com.legakrishi.solar.model.Device d,
                                  LocalDateTime now,
                                  int thresholdMinutes,
                                  String minutesText) {
        String name = (d.getName() != null && !d.getName().isBlank()) ? d.getName() : ("Device #" + d.getId());
        String msg = "Device " + name + " offline for ~" + minutesText + " min (threshold " + thresholdMinutes + "m)";

        return AlertEvent.builder()
                .site(d.getSite())
                .device(d)
                .type("OFFLINE")
                .message(msg)
                .triggeredAt(now)
                .acknowledged(false)
                .build();
    }

    // === WhatsApp helper ===
    private void sendWhatsAppOffline(com.legakrishi.solar.model.Site site,
                                     com.legakrishi.solar.model.Device d,
                                     LocalDateTime since,
                                     String minutesText) {
        try {
            if (site == null) return;

            // Only send if site has WA enabled or we have global fallback
            boolean enabled = Boolean.TRUE.equals(site.getNotifyWhatsappEnabled());
            String to = site.getNotifyWhatsappTo(); // may be null → WA service will use fallback properties when we pass null
            if (!enabled && (to == null || to.isBlank())) return;

            // Template name: site override or fallback
            String template = (site.getNotifyWhatsappTemplate() != null && !site.getNotifyWhatsappTemplate().isBlank())
                    ? site.getNotifyWhatsappTemplate()
                    : "solar_offline";

            String siteName = (site.getName() != null && !site.getName().isBlank()) ? site.getName() : ("Site #" + site.getId());
            String devName  = (d.getName() != null && !d.getName().isBlank()) ? d.getName()   : ("Device #" + d.getId());

            String sinceStr = (since != null)
                    ? since.atZone(TZ).toLocalTime().toString()
                    : "unknown";

            // Vars must match your template’s {{1}}..{{4}}
            java.util.List<String> vars = java.util.List.of(
                    siteName,
                    devName,
                    sinceStr,
                    minutesText
            );

            // null "to" → service uses whatsapp.to property
            whatsapp.sendTemplate((to != null && !to.isBlank()) ? to : null, template, vars);
        } catch (Exception ignored) {
            // keep alert flow healthy even if WA fails
        }
    }
}
