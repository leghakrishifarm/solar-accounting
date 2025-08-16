// src/main/java/com/legakrishi/solar/rms/dto/RmsAlarm.java
package com.legakrishi.solar.rms.dto;

import java.time.Instant;

public record RmsAlarm(
        Instant timeIso,
        String device,
        String severity,  // CRITICAL | MAJOR | MINOR | INFO
        String title
) {}
