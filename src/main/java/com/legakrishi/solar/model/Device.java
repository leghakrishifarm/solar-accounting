package com.legakrishi.solar.model;

import jakarta.persistence.*;
import lombok.*;
import com.legakrishi.solar.model.MeterKind;
import java.time.LocalDateTime;

@Entity
@Table(name = "device")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Device {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Site site;

    @Column(nullable = false, length = 128)
    private String name;              // e.g., "Gateway #1" / "Inverter-1"

    @Column(length = 64)
    private String type;              // GATEWAY / INVERTER / METER

    @Column(length = 128)
    private String serialNo;

    @Column(nullable = false, length = 64, unique = true)
    private String apiToken;          // per-device secret key

    private Boolean active;           // default true

    private LocalDateTime lastSeen;   // updated when data received

    @Enumerated(EnumType.STRING)
    @Column(name = "default_meter_kind", length = 16)
    private MeterKind defaultMeterKind;

    @PrePersist
    public void prePersist() {
        if (active == null) active = true;
        if (type == null || type.isBlank()) type = "GATEWAY";
    }
}
