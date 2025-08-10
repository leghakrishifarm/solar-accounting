package com.legakrishi.solar.model;

import com.legakrishi.solar.model.MeterKind;
import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reading",
        indexes = {
                @Index(name = "idx_site_ts", columnList = "site_id, ts"),
                @Index(name = "idx_device_ts", columnList = "device_id, ts"),
                @Index(name = "idx_site_meter_ts", columnList = "site_id, meter_kind, ts")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Reading {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "meter_kind", length = 16)
    private MeterKind meterKind;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Site site;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Device device;

    @Column(nullable = false)
    private LocalDateTime ts;      // timestamp in site timezone (we’ll use Asia/Kolkata)

    // Instantaneous total AC Active Power (kW)
    @Column(name = "total_ac_active_power_kw", precision = 12, scale = 3)
    private BigDecimal totalAcActivePowerKw;

    // Cumulative total AC Active Energy (kWh)
    @Column(name = "total_ac_active_energy_kwh", precision = 16, scale = 3)
    private BigDecimal totalAcActiveEnergyKwh;

    // Per-day AC Active Energy (kWh)
    @Column(name = "daily_ac_active_energy_kwh", precision = 12, scale = 3)
    private BigDecimal dailyAcActiveEnergyKwh;

    // Per-day AC Active Export Energy (kWh)
    @Column(name = "daily_ac_active_export_energy_kwh", precision = 12, scale = 3)
    private BigDecimal dailyAcActiveExportEnergyKwh;

    // Cumulative AC Active Export Energy (kWh)
    @Column(name = "total_ac_active_export_energy_kwh", precision = 16, scale = 3)
    private BigDecimal totalAcActiveExportEnergyKwh;

    // Per-day AC Active Import Energy (kWh)
    @Column(name = "daily_ac_active_import_energy_kwh", precision = 12, scale = 3)
    private BigDecimal dailyAcActiveImportEnergyKwh;

    // Cumulative AC Active Import Energy (kWh)
    @Column(name = "total_ac_active_import_energy_kwh", precision = 16, scale = 3)
    private BigDecimal totalAcActiveImportEnergyKwh;

    // Per-day DC Energy (kWh)
    @Column(name = "daily_dc_energy_kwh", precision = 12, scale = 3)
    private BigDecimal dailyDcEnergyKwh;

    // Cumulative DC Energy (kWh)
    @Column(name = "total_dc_energy_kwh", precision = 16, scale = 3)
    private BigDecimal totalDcEnergyKwh;

    // telemetry
    private Double powerKw;        // instantaneous power
    private Double energyKwh;      // cumulative energy (totalizer)
    private Double dcVoltage;
    private Double dcCurrent;
    private Double acVoltage;
    private Double acCurrent;
    private Double temperature;

    @Column(length = 32)
    private String status;         // OK/WARN/FAULT/etc.
}
