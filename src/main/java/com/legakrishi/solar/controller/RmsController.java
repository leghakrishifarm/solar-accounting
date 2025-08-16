// src/main/java/com/legakrishi/solar/controller/RmsController.java
package com.legakrishi.solar.controller;

import com.legakrishi.solar.rms.RmsService;
import com.legakrishi.solar.rms.dto.RmsAlarm;
import com.legakrishi.solar.rms.dto.RmsDevicesStatus;
import com.legakrishi.solar.rms.dto.RmsSummary;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/rms")
public class RmsController {

    private final RmsService rmsService;

    public RmsController(RmsService rmsService) {
        this.rmsService = rmsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<RmsSummary> summary(@RequestParam Long siteId) {
        return ResponseEntity.ok(rmsService.getSummary(siteId));
    }

    @GetMapping("/devices/status")
    public ResponseEntity<RmsDevicesStatus> devices(@RequestParam Long siteId) {
        return ResponseEntity.ok(rmsService.getDevicesStatus(siteId));
    }

    @GetMapping("/alarms")
    public ResponseEntity<List<RmsAlarm>> alarms(@RequestParam Long siteId,
                                                 @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(rmsService.listAlarms(siteId, Math.max(1, Math.min(limit, 20))));
    }
}
