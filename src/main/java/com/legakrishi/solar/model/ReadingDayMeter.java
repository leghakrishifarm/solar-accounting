package com.legakrishi.solar.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(
        name = "reading_day_meter",
        uniqueConstraints = @UniqueConstraint(name = "uniq_site_meter_day", columnNames = {"site_id","meter_kind","day"}),
        indexes = {
                @Index(name = "idx_rdm_site_day", columnList = "site_id, day"),
                @Index(name = "idx_rdm_site_meter_day", columnList = "site_id, meter_kind, day")
        }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReadingDayMeter {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Site site;

    @Enumerated(EnumType.STRING)
    @Column(name = "meter_kind", length = 16, nullable = false)
    private MeterKind meterKind; // MAIN / STANDBY / CHECK

    @Column(nullable = false)
    private LocalDate day;

    @Column(precision = 16, scale = 3)
    private BigDecimal acActiveEnergyKwh;

    @Column(precision = 16, scale = 3)
    private BigDecimal acExportEnergyKwh;

    @Column(precision = 16, scale = 3)
    private BigDecimal acImportEnergyKwh;

    @Column(precision = 16, scale = 3)
    private BigDecimal dcEnergyKwh;

    @Column(precision = 12, scale = 3)
    private BigDecimal maxAcPowerKw;

    private LocalDateTime lastTs;
}
