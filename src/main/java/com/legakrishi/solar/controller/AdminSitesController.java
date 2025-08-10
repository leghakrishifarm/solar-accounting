package com.legakrishi.solar.controller;

import com.legakrishi.solar.repository.SiteRepository;
import com.legakrishi.solar.service.AlertEmailService;
import com.legakrishi.solar.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalTime;
import java.util.Collections;

@Controller
@RequestMapping("/admin/sites")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSitesController {

    private final SiteRepository siteRepo;
    private final AlertEmailService emailer;
    private final WhatsAppService whatsapp;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("sites", siteRepo.findAll());
        return "admin/sites";
    }

    @PostMapping("/{id}/capacity")
    public String updateCapacity(@PathVariable Long id,
                                 @RequestParam(required = false) Double capacityKw,
                                 RedirectAttributes ra) {
        var site = siteRepo.findById(id).orElseThrow();
        site.setCapacityKw(capacityKw);
        siteRepo.save(site);
        ra.addFlashAttribute("msg", "Updated capacity for site #" + id);
        return "redirect:/admin/sites";
    }

    @PostMapping("/{id}/daylight")
    public String updateDaylight(@PathVariable Long id,
                                 @RequestParam(required = false) String daylightStart,
                                 @RequestParam(required = false) String daylightEnd,
                                 RedirectAttributes ra) {
        var site = siteRepo.findById(id).orElseThrow();

        LocalTime start = null, end = null;
        try { if (daylightStart != null && !daylightStart.isBlank()) start = LocalTime.parse(daylightStart); } catch (Exception ignored) {}
        try { if (daylightEnd   != null && !daylightEnd.isBlank())   end   = LocalTime.parse(daylightEnd);   } catch (Exception ignored) {}

        site.setDaylightStart(start);
        site.setDaylightEnd(end);
        siteRepo.save(site);

        ra.addFlashAttribute("msg", "Updated daylight window for site #" + id);
        return "redirect:/admin/sites";
    }

    @PostMapping("/{id}/zero-threshold")
    public String updateZeroThreshold(@PathVariable Long id,
                                      @RequestParam(required = false) Double zeroThresholdKw,
                                      RedirectAttributes ra) {
        var site = siteRepo.findById(id).orElseThrow();
        site.setZeroThresholdKw(zeroThresholdKw);
        siteRepo.save(site);
        ra.addFlashAttribute("msg", "Updated zero-power threshold for site #" + id);
        return "redirect:/admin/sites";
    }

    @PostMapping("/{id}/offline-threshold")
    public String updateOfflineThreshold(@PathVariable Long id,
                                         @RequestParam(required = false) Integer offlineThresholdMinutes,
                                         RedirectAttributes ra) {
        var site = siteRepo.findById(id).orElseThrow();
        site.setOfflineThresholdMinutes(offlineThresholdMinutes);
        siteRepo.save(site);
        ra.addFlashAttribute("msg", "Updated offline threshold for site #" + id);
        return "redirect:/admin/sites";
    }

    @PostMapping("/{id}/webhook")
    public String updateWebhook(@PathVariable Long id,
                                @RequestParam(required = false) Boolean enabled,
                                @RequestParam(required = false) String url,
                                RedirectAttributes ra) {
        var site = siteRepo.findById(id).orElseThrow();
        site.setNotifyWebhookEnabled(enabled != null && enabled);
        site.setNotifyWebhookUrl((url != null && !url.isBlank()) ? url.trim() : null);
        siteRepo.save(site);
        ra.addFlashAttribute("msg", "Updated webhook settings for site #" + id);
        return "redirect:/admin/sites";
    }

    // ----- Email -----
    @PostMapping("/{id}/email")
    public String updateEmail(@PathVariable Long id,
                              @RequestParam(required = false) Boolean enabled,
                              @RequestParam(required = false) String to,
                              RedirectAttributes ra) {
        var site = siteRepo.findById(id).orElseThrow();
        site.setNotifyEmailEnabled(enabled != null && enabled);
        site.setNotifyEmailTo((to != null && !to.isBlank()) ? to.trim() : null);
        siteRepo.save(site);
        ra.addFlashAttribute("msg", "Updated email settings for site #" + id);
        return "redirect:/admin/sites";
    }

    @PostMapping("/{id}/email-test")
    public String testEmail(@PathVariable Long id, RedirectAttributes ra) {
        var site = siteRepo.findById(id).orElseThrow();
        var alert = com.legakrishi.solar.model.AlertEvent.builder()
                .site(site).device(null).type("TEST")
                .message("This is a test alert email from Legha Krishi Farm.")
                .triggeredAt(java.time.LocalDateTime.now())
                .acknowledged(false)
                .build();
        emailer.sendIfConfigured(alert);
        ra.addFlashAttribute("msg", "Test email requested for site #" + id + ". Check inbox/spam.");
        return "redirect:/admin/sites";
    }

    @GetMapping("/{id}/email-test")
    public String testEmailGet(@PathVariable Long id, RedirectAttributes ra) {
        return testEmail(id, ra);
    }

    // ----- WhatsApp: save settings -----
    @PostMapping("/{id}/whatsapp")
    public String updateWhatsapp(@PathVariable Long id,
                                 @RequestParam(required = false) Boolean enabled,
                                 @RequestParam(required = false) String to,
                                 @RequestParam(required = false) String template,
                                 RedirectAttributes ra) {
        var site = siteRepo.findById(id).orElseThrow();

        String normalized = null;
        if (to != null && !to.isBlank()) {
            normalized = to.replaceAll("[^0-9]", ""); // digits only
        }

        site.setNotifyWhatsappEnabled(enabled != null && enabled);
        site.setNotifyWhatsappTo((normalized != null && !normalized.isBlank()) ? normalized : null);
        site.setNotifyWhatsappTemplate((template != null && !template.isBlank())
                ? template.trim()
                : "hello_world");

        siteRepo.save(site);
        ra.addFlashAttribute("msg", "Updated WhatsApp settings for site #" + id);
        return "redirect:/admin/sites";
    }

    // ----- WhatsApp: template test -----
    @GetMapping("/{id}/whatsapp-test")
    public String whatsappTest(@PathVariable Long id, RedirectAttributes ra) {
        var site = siteRepo.findById(id).orElseThrow();

        // Make some sample values for the 4 variables:
        String siteName   = (site.getName() != null && !site.getName().isBlank())
                ? site.getName() : "Legha Krishi Farm – Bhunia";
        String deviceName = "Gateway #1";

        var tz = java.time.ZoneId.of(site.getTimezone() != null ? site.getTimezone() : "Asia/Calcutta");
        var now = java.time.LocalDateTime.now(tz);

        // Pretend it went offline 23 minutes ago
        var since = now.minusMinutes(23);
        String sinceText = since.toLocalTime().toString(); // e.g. 14:42
        String durationMin = String.valueOf(23);

        // Order MUST match your template:
        // {{1}} = SITE, {{2}} = DEVICE, {{3}} = SINCE, {{4}} = DURATION(min)
        var params = java.util.List.of(siteName, deviceName, sinceText, durationMin);

        // Send to the site’s configured number; fallback to global default if your service supports it
        String to = (site.getNotifyWhatsappTo() != null && !site.getNotifyWhatsappTo().isBlank())
                ? site.getNotifyWhatsappTo().trim() : null;

        boolean ok = whatsapp.sendTemplate(to, "solar_offline", params);

        ra.addFlashAttribute("msg",
                (ok ? "WhatsApp test sent" : "WhatsApp test failed") +
                        " for site #" + id + ". Check your phone.");
        return "redirect:/admin/sites";
    }

    // ----- WhatsApp: session text (24h window) -----
    @GetMapping("/{id}/whatsapp-session-test")
    public String whatsappSessionTest(@PathVariable Long id, RedirectAttributes ra) {
        var site = siteRepo.findById(id).orElseThrow();

        boolean ok = whatsapp.sendSessionText(
                site.getNotifyWhatsappTo(),
                "Session test ✅ from admin (this requires you to have messaged us in the last 24h)"
        );

        ra.addFlashAttribute("msg",
                (ok ? "WhatsApp session text sent" : "WhatsApp session text failed")
                        + " for site #" + id + ". Check your phone.");
        return "redirect:/admin/sites";
    }
}
