package com.legakrishi.solar.service;

import com.legakrishi.solar.model.AlertChannel;
import com.legakrishi.solar.model.AlertDelivery;
import com.legakrishi.solar.model.AlertEvent;
import com.legakrishi.solar.repository.AlertDeliveryRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertEmailService {

    private final JavaMailSender mailSender;
    private final AlertDeliveryRepository deliveryRepo;

    @Value("${app.alerts.mail.from:no-reply@leghakrishi.local}")
    private String fromAddress;

    @PostConstruct
    void logMailConfig() {
        try {
            if (mailSender instanceof JavaMailSenderImpl j) {
                log.info("Mail config → host={}, port={}, username={}", j.getHost(), j.getPort(), j.getUsername());
            } else {
                log.info("Mail config → custom JavaMailSender in use");
            }
        } catch (Exception ignored) {}
    }

    public void sendIfConfigured(AlertEvent alert) {
        try {
            var site = alert.getSite();
            if (site == null) {
                log.info("Email: skip (no site)");
                saveDelivery(alert, false, "NO_SITE", "No site on alert", null);
                return;
            }

            Boolean enabled = site.getNotifyEmailEnabled();
            String to = site.getNotifyEmailTo();
            if (enabled == null || !enabled || to == null || to.isBlank()) {
                log.info("Email: skip (disabled or empty recipient) for site {}", site.getId());
                saveDelivery(alert, false, "DISABLED_OR_EMPTY", "Email disabled or recipient empty", null);
                return;
            }

            String subject = "[Solar Alert] " + alert.getType()
                    + " · Site #" + site.getId()
                    + (site.getName() != null ? (" (" + site.getName() + ")") : "");

            StringBuilder body = new StringBuilder();
            body.append("Type: ").append(alert.getType()).append("\n");
            body.append("Site: ").append(site.getId());
            if (site.getName() != null && !site.getName().isBlank()) body.append(" (").append(site.getName()).append(")");
            body.append("\nDevice: ").append(alert.getDevice() != null ? alert.getDevice().getId() + " (" + alert.getDevice().getName() + ")" : "-");
            body.append("\nTime: ").append(alert.getTriggeredAt() != null ? alert.getTriggeredAt().toString() : "-");
            body.append("\nMessage: ").append(alert.getMessage() != null ? alert.getMessage() : "");

            log.info("Email: sending '{}' to {}", alert.getType(), to);

            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(to.trim());
            msg.setSubject(subject);
            msg.setText(body.toString());

            mailSender.send(msg);
            log.info("Email: sent to {}", to);
            saveDelivery(alert, true, "OK", "Sent to " + to, null);
        } catch (Exception e) {
            log.warn("Email send failed: {}", e.toString());
            saveDelivery(alert, false, "ERROR", e.toString(), null);
        }
    }

    private void saveDelivery(AlertEvent alert, boolean ok, String code, String message, @Nullable String raw) {
        try {
            deliveryRepo.save(AlertDelivery.builder()
                    .alert(alert)
                    .channel(AlertChannel.EMAIL)
                    .success(ok)
                    .code(code)
                    .message(message)
                    .responseBody(raw)
                    .build());
        } catch (Exception ex) {
            log.warn("Email: failed to persist delivery log: {}", ex.toString());
        }
    }
}
