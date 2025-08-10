package com.legakrishi.solar.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert_event")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AlertEvent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Site site;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Device device;

    @Column(nullable = false, length = 32)
    private String type;             // e.g. OFFLINE

    @Column(length = 512)
    private String message;          // human-readable note

    @Column(nullable = false)
    private LocalDateTime triggeredAt;

    @Column(nullable = false)
    private boolean acknowledged = false;
}
