package com.legakrishi.solar.controller;

import com.legakrishi.solar.config.MonitoringProps;
import com.legakrishi.solar.model.AlertEvent;
import com.legakrishi.solar.repository.AlertEventRepository;
import com.legakrishi.solar.repository.DeviceRepository;
import com.legakrishi.solar.repository.PartnerSiteRepository;
import com.legakrishi.solar.repository.UserRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/partners/api")
@PreAuthorize("hasAnyRole('PARTNER','ADMIN')")
public class PartnerAlertsController {

    private final AlertEventRepository alertRepo;
    private final MonitoringProps props;
    private final DeviceRepository deviceRepo;
    private final PartnerSiteRepository partnerSiteRepo;
    private final UserRepository userRepo;

    public PartnerAlertsController(AlertEventRepository alertRepo,
                                   MonitoringProps props,
                                   DeviceRepository deviceRepo,
                                   PartnerSiteRepository partnerSiteRepo,
                                   UserRepository userRepo) {
        this.alertRepo = alertRepo;
        this.props = props;
        this.deviceRepo = deviceRepo;
        this.partnerSiteRepo = partnerSiteRepo;
        this.userRepo = userRepo;
    }

    // Most recent, unacknowledged alert (OFFLINE or ZERO_POWER) within last 60 minutes; supports ?siteId=
    @GetMapping("/last-alert")
    public Map<String, Object> lastAlert(Principal principal,
                                         @RequestParam(value = "siteId", required = false) Long siteIdParam) {

        Res r = resolveSiteDevice(principal, siteIdParam);
        var now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));

        var offOpt  = alertRepo.findTopByDeviceIdAndTypeOrderByTriggeredAtDesc(r.deviceId, "OFFLINE");
        var zeroOpt = alertRepo.findTopByDeviceIdAndTypeOrderByTriggeredAtDesc(r.deviceId, "ZERO_POWER");

        AlertEvent chosen = chooseLatestFreshUnacked(offOpt.orElse(null), zeroOpt.orElse(null), now);

        boolean hasAlert = chosen != null;
        return Map.of(
                "hasAlert", hasAlert,
                "type", hasAlert ? chosen.getType() : null,
                "message", hasAlert ? chosen.getMessage() : null,
                "triggeredAt", hasAlert && chosen.getTriggeredAt()!=null ? chosen.getTriggeredAt().toString() : null,
                "siteId", r.siteId,
                "deviceId", r.deviceId
        );
    }

    // Acknowledge the most recent alert (OFFLINE or ZERO_POWER); supports ?siteId=
    @GetMapping("/ack-last-alert")
    public Map<String, Object> ackLastAlert(Principal principal,
                                            @RequestParam(value = "siteId", required = false) Long siteIdParam) {

        Res r = resolveSiteDevice(principal, siteIdParam);
        var now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));

        var offOpt  = alertRepo.findTopByDeviceIdAndTypeOrderByTriggeredAtDesc(r.deviceId, "OFFLINE");
        var zeroOpt = alertRepo.findTopByDeviceIdAndTypeOrderByTriggeredAtDesc(r.deviceId, "ZERO_POWER");
        AlertEvent chosen = chooseLatestFreshUnacked(offOpt.orElse(null), zeroOpt.orElse(null), now);

        if (chosen == null) {
            chosen = chooseLatest(offOpt.orElse(null), zeroOpt.orElse(null));
        }
        if (chosen != null && !Boolean.TRUE.equals(chosen.isAcknowledged())) {
            chosen.setAcknowledged(true);
            alertRepo.save(chosen);
        }
        return Map.of("ok", true, "siteId", r.siteId, "deviceId", r.deviceId);
    }

    // Recent alerts (last 24h), max 5; supports ?siteId=
    @GetMapping("/alerts/latest")
    public java.util.List<java.util.Map<String, Object>> latestAlerts(
            Principal principal,
            @RequestParam(value = "siteId", required = false) Long siteIdParam) {

        Res r = resolveSiteDevice(principal, siteIdParam);
        var now = java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
        var since = now.minusDays(1);

        return alertRepo.findAll().stream()
                .filter(a -> a.getDevice() != null && a.getDevice().getId().equals(r.deviceId))
                .filter(a -> a.getTriggeredAt() != null && a.getTriggeredAt().isAfter(since))
                .sorted((a,b) -> b.getTriggeredAt().compareTo(a.getTriggeredAt()))
                .limit(5)
                .map(a -> {
                    Map<String,Object> m = new LinkedHashMap<>();
                    m.put("type", a.getType());
                    m.put("message", a.getMessage());
                    m.put("triggeredAt", a.getTriggeredAt().toString());
                    m.put("acknowledged", a.isAcknowledged());
                    return m;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    // Alerts CSV (last 24h); supports ?siteId=
    @GetMapping(value = "/alerts/export.csv", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<String> exportAlertsCsv(
            Principal principal,
            @RequestParam(value = "siteId", required = false) Long siteIdParam) {

        Res r = resolveSiteDevice(principal, siteIdParam);

        var now = java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Kolkata"));
        var since = now.minusDays(1);

        var list = alertRepo.findAll().stream()
                .filter(a -> a.getDevice() != null && a.getDevice().getId().equals(r.deviceId))
                .filter(a -> a.getTriggeredAt() != null && a.getTriggeredAt().isAfter(since))
                .sorted((a,b) -> b.getTriggeredAt().compareTo(a.getTriggeredAt()))
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("triggered_at,type,message,acknowledged\n");
        for (var a : list) {
            sb.append(a.getTriggeredAt() != null ? a.getTriggeredAt().toString() : "")
                    .append(',')
                    .append(a.getType() != null ? a.getType() : "")
                    .append(',')
                    .append(a.getMessage() != null ? a.getMessage().replace(',', ' ') : "")
                    .append(',')
                    .append(Boolean.TRUE.equals(a.isAcknowledged()) ? "true" : "false")
                    .append('\n');
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=alerts-24h-site-" + r.siteId + ".csv");
        return new ResponseEntity<>(sb.toString(), headers, HttpStatus.OK);
    }

    // --- helpers ---

    private static class Res { Long siteId; Long deviceId; Res(Long s, Long d){siteId=s;deviceId=d;} }

    private Res resolveSiteDevice(Principal principal, Long siteIdParam) {
        Long siteId = props.getSiteId();
        Long deviceId = props.getDeviceId();

        if (siteIdParam != null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAdmin = auth != null && auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch("ROLE_ADMIN"::equals);

            boolean allowed = false;
            if (isAdmin) {
                allowed = true;
            } else if (principal != null) {
                var userOpt = userRepo.findByEmail(principal.getName());
                if (userOpt.isPresent() && userOpt.get().getPartner() != null) {
                    Long partnerId = userOpt.get().getPartner().getId();
                    allowed = partnerSiteRepo.findByPartnerIdAndActiveTrue(partnerId).stream()
                            .anyMatch(ps -> ps.getSite() != null && ps.getSite().getId().equals(siteIdParam));
                }
            }

            if (allowed) {
                siteId = siteIdParam;
                var devOpt = deviceRepo.findFirstBySiteIdOrderByIdAsc(siteId);
                if (devOpt.isPresent()) {
                    deviceId = devOpt.get().getId();
                }
            }
        }
        return new Res(siteId, deviceId);
    }

    private AlertEvent chooseLatestFreshUnacked(AlertEvent a, AlertEvent b, LocalDateTime now) {
        a = filterFreshUnacked(a, now);
        b = filterFreshUnacked(b, now);
        return chooseLatest(a, b);
    }

    private AlertEvent filterFreshUnacked(AlertEvent e, LocalDateTime now) {
        if (e == null) return null;
        boolean fresh = e.getTriggeredAt() != null && e.getTriggeredAt().isAfter(now.minusMinutes(60));
        boolean notAck = !Boolean.TRUE.equals(e.isAcknowledged());
        return (fresh && notAck) ? e : null;
    }

    private AlertEvent chooseLatest(AlertEvent a, AlertEvent b) {
        if (a == null) return b;
        if (b == null) return a;
        if (a.getTriggeredAt() == null) return b;
        if (b.getTriggeredAt() == null) return a;
        return a.getTriggeredAt().isAfter(b.getTriggeredAt()) ? a : b;
    }
}
