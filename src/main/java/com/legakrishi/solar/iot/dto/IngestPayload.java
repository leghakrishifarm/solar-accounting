package com.legakrishi.solar.iot.dto;

import lombok.Data;
import com.legakrishi.solar.model.MeterKind;
import java.math.BigDecimal;

@Data
public class IngestPayload {
    private Long deviceId;          // device sending data
    private String deviceToken;     // per-device API key (from Device.apiToken)

    // Which meter? default MAIN at service if null
    private MeterKind meter;

    private Double powerKw;
    private Double energyKwh;
    private Double dcVoltage;
    private Double dcCurrent;
    private Double acVoltage;
    private Double acCurrent;
    private Double temperature;
    private String status;          // OK/WARN/FAULT

    // New metrics (nullable)
    private BigDecimal totalAcActivePowerKw;
    private BigDecimal totalAcActiveEnergyKwh;
    private BigDecimal dailyAcActiveEnergyKwh;
    private BigDecimal dailyAcActiveExportEnergyKwh;
    private BigDecimal totalAcActiveExportEnergyKwh;
    private BigDecimal dailyAcActiveImportEnergyKwh;
    private BigDecimal totalAcActiveImportEnergyKwh;
    private BigDecimal dailyDcEnergyKwh;
    private BigDecimal totalDcEnergyKwh;

    // optional client timestamp (seconds since epoch)
    private Long tsEpoch;
}
