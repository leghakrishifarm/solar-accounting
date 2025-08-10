package com.legakrishi.solar.service;

import com.legakrishi.solar.model.AlertChannel;
import com.legakrishi.solar.model.AlertDelivery;
import com.legakrishi.solar.model.AlertEvent;
import com.legakrishi.solar.model.Site;
import com.legakrishi.solar.repository.AlertDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppService {

    @Value("${whatsapp.enabled:true}")
    private boolean enabled;

    @Value("${whatsapp.phone-number-id:}")
    private String phoneNumberId;

    @Value("${whatsapp.token:}")
    private String token;

    /** Global fallback recipient (dev/test number) */
    @Value("${whatsapp.to:}")
    private String defaultTo;

    /** Template language code (e.g., en_US, hi_IN). */
    @Value("${whatsapp.language:en_US}")
    private String templateLanguage;

    private final RestTemplate http = new RestTemplate();
    private final AlertDeliveryRepository deliveryRepo;

    /* ===================== PUBLIC API ===================== */

    /** Send a template (optionally with variables). */
    public boolean sendTemplate(String toOverride, String templateName, List<String> bodyParams) {
        if (!enabled) {
            log.info("WA: disabled in config");
            return false;
        }
        String to = (toOverride != null && !toOverride.isBlank()) ? normalize(toOverride) : normalize(defaultTo);
        if (to == null || to.isBlank()) {
            log.warn("WA: skip (no recipient configured)");
            return false;
        }
        if (isWaConfigMissing()) return false;

        String name = (templateName == null || templateName.isBlank()) ? "hello_world" : templateName;
        String url = "https://graph.facebook.com/v19.0/" + phoneNumberId + "/messages";

        Map<String,Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", to);

        Map<String,Object> template = new LinkedHashMap<>();
        template.put("name", name);
        template.put("language", Map.of("code", templateLanguage));

        // Only include body components when there are parameters (hello_world has none)
        if (bodyParams != null && !bodyParams.isEmpty()) {
            template.put("components", List.of(
                    Map.of("type","body", "parameters", toParams(bodyParams))
            ));
        }

        payload.put("type", "template");
        payload.put("template", template);

        HttpHeaders headers = stdHeaders();

        try {
            log.info("WA: POST {} (template={}, lang={}, to={}, vars={})",
                    url, name, templateLanguage, maskPhone(to), (bodyParams == null ? 0 : bodyParams.size()));

            ResponseEntity<String> resp = http.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(payload, headers), String.class);

            log.info("WA resp {} {}", resp.getStatusCodeValue(), resp.getBody());
            saveDelivery(null, true, resp.getStatusCodeValue(), "TEMPLATE_OK",
                    "Template '" + name + "' sent to " + maskPhone(to), trim(resp.getBody()));
            return resp.getStatusCode().is2xxSuccessful();
        } catch (HttpStatusCodeException e) {
            String body = e.getResponseBodyAsString();
            log.warn("WA send failed: status={} body={}", e.getStatusCode().value(), body);
            saveDelivery(null, false, e.getStatusCode().value(), "TEMPLATE_HTTP_" + e.getStatusCode().value(),
                    "Template '" + name + "' failed", trim(body));
            return false;
        } catch (ResourceAccessException e) {
            log.warn("WA send failed: connection/timeout: {}", e.getMessage());
            saveDelivery(null, false, null, "TEMPLATE_TIMEOUT", e.getMessage(), null);
            return false;
        } catch (Exception e) {
            log.warn("WA send failed: {}", e.toString());
            saveDelivery(null, false, null, "TEMPLATE_ERROR", e.toString(), null);
            return false;
        }
    }

    /** Send a **24-hour session** text message (non-template). Requires an open user session. */
    public boolean sendSessionText(String toOverride, String text) {
        if (!enabled) {
            log.info("WA: disabled in config");
            return false;
        }
        if (isWaConfigMissing()) return false;

        String to = (toOverride != null && !toOverride.isBlank()) ? normalize(toOverride) : normalize(defaultTo);
        if (to == null || to.isBlank()) {
            log.warn("WA: skip (no recipient configured)");
            saveDelivery(null, false, null, "SESSION_NO_RECIPIENT", "No WA recipient configured", null);
            return false;
        }

        String url = "https://graph.facebook.com/v19.0/" + phoneNumberId + "/messages";

        Map<String,Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", to);
        payload.put("type", "text");
        payload.put("text", Map.of(
                "preview_url", false,
                "body", text == null ? "" : text
        ));

        HttpHeaders headers = stdHeaders();

        try {
            log.info("WA: POST {} (session text to={})", url, maskPhone(to));

            ResponseEntity<String> resp = http.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(payload, headers), String.class);

            saveDelivery(null, true, resp.getStatusCodeValue(), "SESSION_OK",
                    "Session text sent to " + maskPhone(to), trim(resp.getBody()));
            return resp.getStatusCode().is2xxSuccessful();
        } catch (HttpStatusCodeException e) {
            String body = e.getResponseBodyAsString();
            saveDelivery(null, false, e.getStatusCode().value(), "SESSION_HTTP_" + e.getStatusCode().value(),
                    "Session text failed", trim(body));
            return false;
        } catch (ResourceAccessException e) {
            saveDelivery(null, false, null, "SESSION_TIMEOUT", e.getMessage(), null);
            return false;
        } catch (Exception e) {
            saveDelivery(null, false, null, "SESSION_ERROR", e.toString(), null);
            return false;
        }
    }

    /** Send alert using per-site settings if available/enabled; fallback to global. */
    public boolean sendAlert(AlertEvent alert, String toOverride) {
        if (!enabled) {
            log.info("WA: disabled in config");
            return false;
        }
        if (isWaConfigMissing()) return false;

        // Determine recipient & template
        String to = null;
        String template = "hello_world"; // safe default

        Site site = alert != null ? alert.getSite() : null;
        if (site != null && Boolean.TRUE.equals(site.getNotifyWhatsappEnabled())
                && site.getNotifyWhatsappTo() != null && !site.getNotifyWhatsappTo().isBlank()) {
            to = normalize(site.getNotifyWhatsappTo());
            if (site.getNotifyWhatsappTemplate() != null && !site.getNotifyWhatsappTemplate().isBlank()) {
                template = site.getNotifyWhatsappTemplate().trim();
            }
        }
        if (toOverride != null && !toOverride.isBlank()) to = normalize(toOverride);
        if (to == null || to.isBlank()) to = normalize(defaultTo);
        if (to == null || to.isBlank()) {
            log.info("WA: skip (no recipient, site-level off and global blank)");
            saveDelivery(alert, false, null, "NO_RECIPIENT", "No WA recipient configured", null);
            return false;
        }

        // Build variables for custom template (not for hello_world)
        List<String> params = Collections.emptyList();
        if (!"hello_world".equalsIgnoreCase(template) && alert != null) {
            // Keep order aligned with your approved `solar_offline` template:
            // 1) SITE, 2) DEVICE, 3) SINCE (HH:mm), 4) DURATION(min)
            String siteName = (site != null && site.getName() != null) ? site.getName() : (site != null ? "Site #" + site.getId() : "-");
            String device = alert.getDevice() != null
                    ? (nz(alert.getDevice().getName()) != null ? alert.getDevice().getName() : ("Device #" + alert.getDevice().getId()))
                    : "-";
            String since = alert.getTriggeredAt() != null ? alert.getTriggeredAt().toLocalTime().toString() : "-";
            String durationMin = "-"; // compute if you have a start time

            params = List.of(siteName, device, since, durationMin);
        }

        return doSendLinked(alert, to, template, params);
    }

    /* ===================== INTERNALS ===================== */

    private boolean doSendLinked(AlertEvent alert, String to, String templateName, List<String> bodyParams) {
        String url = "https://graph.facebook.com/v19.0/" + phoneNumberId + "/messages";

        Map<String,Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", to);

        Map<String,Object> template = new LinkedHashMap<>();
        template.put("name", templateName);
        template.put("language", Map.of("code", templateLanguage));
        if (bodyParams != null && !bodyParams.isEmpty()) {
            template.put("components", List.of(
                    Map.of("type","body", "parameters", toParams(bodyParams))
            ));
        }

        payload.put("type", "template");
        payload.put("template", template);

        HttpHeaders headers = stdHeaders();

        try {
            log.info("WA: POST {} (template={}, lang={}, to={}, vars={})",
                    url, templateName, templateLanguage, maskPhone(to), (bodyParams == null ? 0 : bodyParams.size()));

            ResponseEntity<String> resp = http.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(payload, headers), String.class);

            saveDelivery(alert, true, resp.getStatusCodeValue(), "TEMPLATE_OK",
                    "Template '" + templateName + "' sent to " + maskPhone(to), trim(resp.getBody()));
            return resp.getStatusCode().is2xxSuccessful();
        } catch (HttpStatusCodeException e) {
            String body = e.getResponseBodyAsString();
            saveDelivery(alert, false, e.getStatusCode().value(), "TEMPLATE_HTTP_" + e.getStatusCode().value(),
                    "Template '" + templateName + "' failed", trim(body));
            return false;
        } catch (ResourceAccessException e) {
            saveDelivery(alert, false, null, "TEMPLATE_TIMEOUT", e.getMessage(), null);
            return false;
        } catch (Exception e) {
            saveDelivery(alert, false, null, "TEMPLATE_ERROR", e.toString(), null);
            return false;
        }
    }

    private HttpHeaders stdHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private boolean isWaConfigMissing() {
        if (phoneNumberId == null || phoneNumberId.isBlank() || token == null || token.isBlank()) {
            log.warn("WA: skip (phone-number-id or token missing)");
            return true;
        }
        return false;
    }

    private List<Map<String,Object>> toParams(List<String> values) {
        List<Map<String,Object>> out = new ArrayList<>();
        for (String v : values) out.add(Map.of("type","text","text", v));
        return out;
    }

    private String maskPhone(String p) {
        if (p == null) return "";
        int n = p.length();
        return n <= 4 ? "****" : p.substring(0, Math.max(0, n - 4)) + "****";
    }

    private String normalize(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        return digits.isBlank() ? null : digits;
    }

    private String nz(String s) { return (s == null || s.isBlank()) ? "-" : s; }

    private String trim(String s) {
        if (s == null) return null;
        return s.length() > 4000 ? s.substring(0, 4000) : s;
    }

    private void saveDelivery(AlertEvent alert, boolean ok, Integer httpStatus,
                              String code, String message, String resp) {
        try {
            AlertDelivery row = AlertDelivery.builder()
                    .alert(alert)
                    .channel(AlertChannel.WHATSAPP)
                    .success(ok)
                    .httpStatus(httpStatus)
                    .code(code)
                    .message(message)
                    .responseBody(resp)
                    .build();
            deliveryRepo.save(row);
        } catch (Exception e) {
            log.warn("WA: failed to persist delivery log: {}", e.toString());
        }
    }
}
