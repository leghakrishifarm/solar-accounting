package com.legakrishi.solar.controller;

import com.legakrishi.solar.model.Device;
import com.legakrishi.solar.model.MeterKind;
import com.legakrishi.solar.model.Site;
import com.legakrishi.solar.repository.DeviceRepository;
import com.legakrishi.solar.repository.SiteRepository;
import jakarta.validation.Valid;
import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/devices")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDeviceController {

    private final DeviceRepository deviceRepo;
    private final SiteRepository siteRepo;

    public AdminDeviceController(DeviceRepository deviceRepo, SiteRepository siteRepo) {
        this.deviceRepo = deviceRepo;
        this.siteRepo = siteRepo;
    }

    @GetMapping("")
    public String list(Model model, @RequestParam(required = false) Long siteId) {
        List<Device> devices = (siteId == null)
                ? deviceRepo.findAll()
                : deviceRepo.findAll().stream()
                .filter(d -> d.getSite() != null && d.getSite().getId().equals(siteId))
                .toList();

        model.addAttribute("devices", devices);
        model.addAttribute("sites", siteRepo.findAll());
        model.addAttribute("siteId", siteId);
        return "admin/devices/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        DeviceForm form = new DeviceForm();
        form.setActive(true);
        form.setDefaultMeterKind(null);
        model.addAttribute("form", form);
        model.addAttribute("sites", siteRepo.findAll());
        model.addAttribute("meterKinds", MeterKind.values()); // <— REQUIRED
        return "admin/devices/form";
    }
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Device d = deviceRepo.findById(id).orElseThrow();
        DeviceForm f = new DeviceForm();
        f.setId(d.getId());
        f.setSiteId(d.getSite().getId());
        f.setName(d.getName());
        f.setType(d.getType());
        f.setSerialNo(d.getSerialNo());
        f.setActive(Boolean.TRUE.equals(d.getActive()));
        f.setApiToken(d.getApiToken());
        f.setDefaultMeterKind(d.getDefaultMeterKind());        // <— copies enum
        model.addAttribute("form", f);
        model.addAttribute("sites", siteRepo.findAll());
        model.addAttribute("meterKinds", MeterKind.values());  // <— REQUIRED
        return "admin/devices/form";
    }
    @PostMapping("/save")
    @Transactional
    public String save(@Valid @ModelAttribute("form") DeviceForm f, BindingResult br, Model model) {
        if (br.hasErrors()) {
            model.addAttribute("sites", siteRepo.findAll());
            model.addAttribute("meterKinds", MeterKind.values());
            return "admin/devices/form";
        }

        Site site = siteRepo.findById(f.getSiteId()).orElseThrow();
        Device d = (f.getId() == null) ? new Device() : deviceRepo.findById(f.getId()).orElseThrow();

        d.setSite(site);
        d.setName(f.getName());
        d.setType(f.getType());
        d.setSerialNo(f.getSerialNo());
        d.setActive(f.isActive());
        d.setDefaultMeterKind(f.getDefaultMeterKind()); // persist Default Meter

        // token handling
        if (f.getId() == null && (f.getApiToken() == null || f.getApiToken().isBlank())) {
            d.setApiToken(generateToken());
        } else if (f.getApiToken() != null && !f.getApiToken().isBlank()) {
            d.setApiToken(f.getApiToken());
        }

        deviceRepo.save(d);
        return "redirect:/admin/devices";
    }

    @PostMapping("/{id}/regenerate-token")
    @Transactional
    public String regenerateToken(@PathVariable Long id) {
        Device d = deviceRepo.findById(id).orElseThrow();
        d.setApiToken(generateToken());
        deviceRepo.save(d);
        return "redirect:/admin/devices/" + id + "/edit";
    }

    @PostMapping("/{id}/toggle")
    @Transactional
    public String toggleActive(@PathVariable Long id) {
        Device d = deviceRepo.findById(id).orElseThrow();
        d.setActive(!Boolean.TRUE.equals(d.getActive()));
        deviceRepo.save(d);
        return "redirect:/admin/devices";
    }

    @PostMapping("/{id}/delete")
    @Transactional
    public String delete(@PathVariable Long id) {
        deviceRepo.deleteById(id);
        return "redirect:/admin/devices";
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Data
    public static class DeviceForm {
        private Long id;
        private Long siteId;
        private String name;
        private String type;      // GATEWAY / INVERTER / METER
        private String serialNo;
        private boolean active = true;
        private String apiToken;
        private MeterKind defaultMeterKind; // MAIN / STANDBY / CHECK (or null)
    }
}
