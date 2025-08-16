// src/main/java/com/legakrishi/solar/rms/RmsService.java
package com.legakrishi.solar.rms;

import com.legakrishi.solar.rms.dto.*;

import java.util.List;

public interface RmsService {
    RmsSummary getSummary(Long siteId);
    RmsDevicesStatus getDevicesStatus(Long siteId);
    List<RmsAlarm> listAlarms(Long siteId, int limit);
}
