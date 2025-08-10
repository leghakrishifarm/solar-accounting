package com.legakrishi.solar.controller;

import com.legakrishi.solar.config.MonitoringProps;
import com.legakrishi.solar.repository.DeviceRepository;
import com.legakrishi.solar.repository.PartnerSiteRepository;
import com.legakrishi.solar.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.*;
import java.util.Map;

@RestController
@RequestMapping("/partners/api")
@PreAuthorize("hasAnyRole('PARTNER','ADMIN')")
public class PartnerHealthController {

    private final DeviceRepository deviceRepo;
    private final MonitoringProps props;
    private final PartnerSiteRepository partnerSiteRepo;
    private final UserRepository userRepo;

    public PartnerHealthController(DeviceRepository deviceRepo,
                                   MonitoringProps props,
                                   PartnerSiteRepository partnerSiteRepo,
                                   UserRepository userRepo) {
        this.deviceRepo = deviceRepo;
        this.props = props;
        this.partnerSiteRepo = partnerSiteRepo;
        this.userRepo = userRepo;
    }

    // JSON: online if lastSeen within configured threshold; supports ?siteId=
    @GetMapping("/online")
    public Map<String, Object> online(Principal principal,
                                      @RequestParam(value = "siteId", required = false) Long siteIdParam) {

        // Default site/device from config
        Long siteId = props.getSiteId();
        Long deviceId = props.getDeviceId();

        // If a siteId is passed, allow if ADMIN or mapped partner
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
                // Resolve a device for this site (first one) if available
                var devOpt = deviceRepo.findFirstBySiteIdOrderByIdAsc(siteId);
                if (devOpt.isPresent()) {
                    deviceId = devOpt.get().getId();
                } // else keep config deviceId as fallback
            }
        }

        var d = deviceRepo.findById(deviceId).orElseThrow();

        String lastStr = "—";
        boolean online = false;
        long minutesSince = 9999;

        if (d.getLastSeen() != null) {
            var now = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
            var diff = Duration.between(d.getLastSeen(), now);
            minutesSince = Math.abs(diff.toMinutes());
            lastStr = d.getLastSeen().toString();
            online = minutesSince <= props.getOfflineThresholdMinutes();
        }

        return Map.of(
                "siteId", siteId,
                "deviceId", deviceId,
                "online", online,
                "lastSeen", lastStr,
                "minutesSince", minutesSince
        );
    }
}
