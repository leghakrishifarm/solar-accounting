package com.legakrishi.solar.controller;

import com.legakrishi.solar.model.AlertChannel;
import com.legakrishi.solar.repository.AlertDeliveryRepository;
import com.legakrishi.solar.repository.AlertEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/alert-deliveries")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAlertDeliveriesController {

    private final AlertDeliveryRepository deliveryRepo;
    private final AlertEventRepository alertRepo;

    @GetMapping("")
    public String latest(@RequestParam(value = "channel", required = false) AlertChannel channel,
                         Model model) {
        var list = (channel == null)
                ? deliveryRepo.findTop200ByOrderByAttemptedAtDesc()
                : deliveryRepo.findTop200ByChannelOrderByAttemptedAtDesc(channel);
        model.addAttribute("deliveries", list);
        model.addAttribute("channel", channel);
        return "admin/alerts/deliveries";
    }

    @GetMapping("/alert/{alertId}")
    public String byAlert(@PathVariable Long alertId, Model model) {
        var alert = alertRepo.findById(alertId).orElse(null);
        var list  = deliveryRepo.findByAlertIdOrderByAttemptedAtDesc(alertId);
        model.addAttribute("alert", alert);
        model.addAttribute("deliveries", list);
        return "admin/alerts/deliveries";
    }
}
