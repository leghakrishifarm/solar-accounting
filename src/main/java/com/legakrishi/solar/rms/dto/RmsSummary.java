// src/main/java/com/legakrishi/solar/rms/dto/RmsSummary.java
package com.legakrishi.solar.rms.dto;

import java.time.Instant;

public record RmsSummary(
        Double pr,                 // %
        Double cuf,                // %
        Double energyTodayKwh,
        Double energyMtdKwh,
        Double availability,       // %
        Instant updatedAtIso
) {}
