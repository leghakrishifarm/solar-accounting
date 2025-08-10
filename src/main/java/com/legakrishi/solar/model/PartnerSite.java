package com.legakrishi.solar.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(
        name = "partner_site",
        uniqueConstraints = @UniqueConstraint(name = "uniq_partner_site", columnNames = {"partner_id","site_id"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PartnerSite {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Partner partner;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Site site;

    // Optional: partner's share (%) in this site (0..100)
    @Column(precision = 5, scale = 2)
    private BigDecimal sharePercent;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));
        }
    }
}
