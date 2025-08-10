package com.legakrishi.solar.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.ZoneId;

@Entity
@Table(name = "site")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Site {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;              // e.g., "Legha Krishi Farm – Bhunia"

    @Column(length = 256)
    private String location;          // optional: address/coords

    @Column(length = 64)
    private String timezone;          // e.g., "Asia/Kolkata"

    @Column(length = 32)
    private String status;            // ACTIVE / INACTIVE

    @Column(name = "capacity_kw")
    private Double capacityKw;

    @Column(name = "daylight_start")
    private java.time.LocalTime daylightStart;

    @Column(name = "daylight_end")
    private java.time.LocalTime daylightEnd;

    @Column(name = "zero_threshold_kw")
    private Double zeroThresholdKw;

    @Column(name = "offline_threshold_minutes")
    private Integer offlineThresholdMinutes;

    @Column(name = "notify_webhook_enabled")
    private Boolean notifyWebhookEnabled;

    @Column(name = "notify_webhook_url", length = 512)
    private String notifyWebhookUrl;

    @Column(name = "notify_email_enabled")
    private Boolean notifyEmailEnabled;

    @Column(name = "notify_email_to", length = 320)
    private String notifyEmailTo;

    // --- WhatsApp alert settings ---
    @jakarta.persistence.Column(name = "notify_whatsapp_enabled")
    private Boolean notifyWhatsappEnabled;   // null/false = disabled

    @jakarta.persistence.Column(name = "notify_whatsapp_to", length = 32)
    private String notifyWhatsappTo;         // E.164, e.g. +9198XXXXXXXX

    @jakarta.persistence.Column(name = "notify_whatsapp_template", length = 80)
    private String notifyWhatsappTemplate;   // e.g. "hello_world" (no variables)

    @PrePersist
    public void prePersist() {
        if (timezone == null || timezone.isBlank()) {
            timezone = ZoneId.systemDefault().getId(); // default
        }
        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
    }
}
