package com.legakrishi.solar.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reading_day",
        uniqueConstraints = @UniqueConstraint(name = "uniq_site_day", columnNames = {"site_id", "day"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReadingDay {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Site site;

    @Column(nullable = false)
    private LocalDate day;           // e.g., 2025-08-09

    private Double energyTodayKwh;   // computed: max(energy)-min(energy) for the day
    private Double maxPowerKw;       // computed: max(power) for the day

    private LocalDateTime lastTs;    // timestamp of latest reading used
}
