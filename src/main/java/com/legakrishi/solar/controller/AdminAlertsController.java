package com.legakrishi.solar.controller;

import com.legakrishi.solar.config.MonitoringProps;
import com.legakrishi.solar.model.AlertEvent;
import com.legakrishi.solar.repository.AlertEventRepository;
import com.legakrishi.solar.repository.DeviceRepository;
import com.legakrishi.solar.service.WhatsAppService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;

@Controller
@RequestMapping("/admin/alerts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAlertsController {

    private final AlertEventRepository alertRepo;
    private final DeviceRepository deviceRepo;
    private final MonitoringProps props;
    private final WhatsAppService whatsapp;

    public AdminAlertsController(AlertEventRepository alertRepo,
                                 DeviceRepository deviceRepo,
                                 MonitoringProps props,
                                 WhatsAppService whatsapp) {
        this.alertRepo = alertRepo;
        this.deviceRepo = deviceRepo;
        this.props = props;
        this.whatsapp = whatsapp;
    }

    @GetMapping("")
    public String list(Model model) {
        model.addAttribute("alerts", alertRepo.findAll()
                .stream()
                .sorted((a, b) -> b.getTriggeredAt().compareTo(a.getTriggeredAt()))
                .toList());
        return "admin/alerts/list";
    }

    /**
     * Resend the latest alert to WhatsApp for the configured device, or
     * for the first device of the given site (?siteId=...).
     */
    @GetMapping("/whatsapp-resend")
    public String resendLatestToWhatsApp(@RequestParam(required = false) Long siteId,
                                         RedirectAttributes ra) {

        // resolve the device id WITHOUT mutating inside a lambda
        Long resolvedDeviceId = props.getDeviceId();
        if (siteId != null) {
            var devOpt = deviceRepo.findFirstBySiteIdOrderByIdAsc(siteId);
            if (devOpt.isPresent()) {
                resolvedDeviceId = devOpt.get().getId();
            }
        }

        final Long did = resolvedDeviceId;
        var latest = alertRepo.findAll().stream()
                .filter(a -> a.getDevice() != null && a.getDevice().getId().equals(did))
                .max(Comparator.comparing(AlertEvent::getTriggeredAt))
                .orElse(null);

        if (latest == null) {
            ra.addFlashAttribute("msg", "No alerts found for device #" + resolvedDeviceId);
            return "redirect:/admin/alerts";
        }

        boolean ok = whatsapp.sendAlert(latest, null);
        ra.addFlashAttribute("msg",
                (ok ? "WhatsApp re-sent: " : "WhatsApp send FAILED: ")
                        + latest.getType() + " · " + latest.getMessage());
        return "redirect:/admin/alerts";
    }

    /**
     * Resend a specific alert by id to WhatsApp.
     */
    @GetMapping("/{id}/whatsapp")
    public String resendById(@PathVariable Long id, RedirectAttributes ra) {
        var alert = alertRepo.findById(id).orElse(null);
        if (alert == null) {
            ra.addFlashAttribute("msg", "Alert #" + id + " not found.");
            return "redirect:/admin/alerts";
        }
        boolean ok = whatsapp.sendAlert(alert, null);
        ra.addFlashAttribute("msg",
                (ok ? "WhatsApp sent: " : "WhatsApp send FAILED: ")
                        + alert.getType() + " · " + alert.getMessage());
        return "redirect:/admin/alerts";
    }
}
