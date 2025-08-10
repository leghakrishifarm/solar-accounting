package com.legakrishi.solar.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert_delivery",
        indexes = {
                @Index(name = "idx_alert_delivery_alert", columnList = "alert_id"),
                @Index(name = "idx_alert_delivery_channel", columnList = "channel"),
                @Index(name = "idx_alert_delivery_attempted_at", columnList = "attemptedAt")
        })
@Getter @Setter
@Builder @NoArgsConstructor @AllArgsConstructor
public class AlertDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Parent alert (OFFLINE / ZERO_POWER / TEST, etc.) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alert_id")
    private AlertEvent alert;

    /** Channel used (WHATSAPP / EMAIL / WEBHOOK) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertChannel channel;

    /** True if channel accepted/sent OK */
    @Column(nullable = false)
    private boolean success;

    /** Optional HTTP status (for WA/webhook) */
    private Integer httpStatus;

    /** Short machine-readable code, e.g. OK, TIMEOUT, INVALID_TEMPLATE */
    @Column(length = 64)
    private String code;

    /** Human readable one-line summary */
    @Column(length = 512)
    private String message;

    /** Raw response or error snippet (trim to avoid huge rows) */
    @Lob
    private String responseBody;

    /** When we attempted the send */
    @CreationTimestamp
    private LocalDateTime attemptedAt;
}
