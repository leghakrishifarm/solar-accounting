package com.legakrishi.solar.service;

import com.legakrishi.solar.model.AlertChannel;
import com.legakrishi.solar.model.AlertDelivery;
import com.legakrishi.solar.model.AlertEvent;
import com.legakrishi.solar.repository.AlertDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertNotifyService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final AlertDeliveryRepository deliveryRepo;

    public void notifyIfConfigured(AlertEvent alert) {
        try {
            var site = alert.getSite();
            if (site == null || !Boolean.TRUE.equals(site.getNotifyWebhookEnabled())) {
                saveDelivery(alert, false, null, "DISABLED", "Webhook disabled or no site/url", null);
                return;
            }
            String url = site.getNotifyWebhookUrl();
            if (url == null || url.isBlank()) {
                saveDelivery(alert, false, null, "NO_URL", "Webhook URL empty", null);
                return;
            }

            Map<String,Object> payload = Map.of(
                    "siteId", site.getId(),
                    "siteName", site.getName(),
                    "type", alert.getType(),
                    "message", alert.getMessage(),
                    "triggeredAt", alert.getTriggeredAt()!=null ? alert.getTriggeredAt().toString() : null,
                    "deviceId", alert.getDevice()!=null ? alert.getDevice().getId() : null,
                    "deviceName", alert.getDevice()!=null ? alert.getDevice().getName() : null
            );

            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<String> resp = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(payload, h), String.class);

            saveDelivery(alert, resp.getStatusCode().is2xxSuccessful(),
                    resp.getStatusCodeValue(), "OK", "Webhook POST " + resp.getStatusCode(), trim(resp.getBody()));
        } catch (HttpStatusCodeException e) {
            saveDelivery(alert, false, e.getStatusCode().value(), "HTTP_" + e.getStatusCode().value(),
                    "Webhook failed", trim(e.getResponseBodyAsString()));
        } catch (Exception e) {
            saveDelivery(alert, false, null, "ERROR", e.toString(), null);
        }
    }

    private void saveDelivery(AlertEvent alert, boolean ok, Integer http,
                              String code, String message, String body) {
        try {
            deliveryRepo.save(AlertDelivery.builder()
                    .alert(alert)
                    .channel(AlertChannel.WEBHOOK)
                    .success(ok)
                    .httpStatus(http)
                    .code(code)
                    .message(message)
                    .responseBody(body)
                    .build());
        } catch (Exception ex) {
            log.warn("Webhook: failed to persist delivery log: {}", ex.toString());
        }
    }

    private String trim(String s) {
        if (s == null) return null;
        return s.length() > 4000 ? s.substring(0, 4000) : s;
    }
}
