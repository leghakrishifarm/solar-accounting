package com.legakrishi.solar.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "energy_sample",
        indexes = {
                @Index(name="idx_es_site_kind_time", columnList="siteId,meterKind,sampleTime"),
                @Index(name="idx_es_site_time", columnList="siteId,sampleTime")
        })
public class EnergySample {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long siteId;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private MeterKind meterKind;      // MAIN / STANDBY / CHECK

    // Instant in UTC recommended
    private Instant sampleTime;       // point-in-time

    // Instantaneous
    private Double totalAcPowerKw;    // kW

    // Daily cumulative counters (resets at midnight device local tz)
    private Double dailyAcEnergyKwh;     // kWh
    private Double dailyAcExportKwh;     // kWh
    private Double dailyAcImportKwh;     // kWh
    private Double dailyDcEnergyKwh;     // kWh

    // Optional telemetry metadata
    private String deviceId;
    private String firmware;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSiteId() { return siteId; }
    public void setSiteId(Long siteId) { this.siteId = siteId; }

    public MeterKind getMeterKind() { return meterKind; }
    public void setMeterKind(MeterKind meterKind) { this.meterKind = meterKind; }

    public Instant getSampleTime() { return sampleTime; }
    public void setSampleTime(Instant sampleTime) { this.sampleTime = sampleTime; }

    public Double getTotalAcPowerKw() { return totalAcPowerKw; }
    public void setTotalAcPowerKw(Double totalAcPowerKw) { this.totalAcPowerKw = totalAcPowerKw; }

    public Double getDailyAcEnergyKwh() { return dailyAcEnergyKwh; }
    public void setDailyAcEnergyKwh(Double dailyAcEnergyKwh) { this.dailyAcEnergyKwh = dailyAcEnergyKwh; }

    public Double getDailyAcExportKwh() { return dailyAcExportKwh; }
    public void setDailyAcExportKwh(Double dailyAcExportKwh) { this.dailyAcExportKwh = dailyAcExportKwh; }

    public Double getDailyAcImportKwh() { return dailyAcImportKwh; }
    public void setDailyAcImportKwh(Double dailyAcImportKwh) { this.dailyAcImportKwh = dailyAcImportKwh; }

    public Double getDailyDcEnergyKwh() { return dailyDcEnergyKwh; }
    public void setDailyDcEnergyKwh(Double dailyDcEnergyKwh) { this.dailyDcEnergyKwh = dailyDcEnergyKwh; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getFirmware() { return firmware; }
    public void setFirmware(String firmware) { this.firmware = firmware; }

}
