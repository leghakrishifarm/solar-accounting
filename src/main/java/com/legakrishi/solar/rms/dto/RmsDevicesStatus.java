// src/main/java/com/legakrishi/solar/rms/dto/RmsDevicesStatus.java
package com.legakrishi.solar.rms.dto;

import java.time.Instant;

public record RmsDevicesStatus(
        Integer online,
        Integer offline,
        Instant lastPacketIso,
        Integer alertsOpen
) {}
