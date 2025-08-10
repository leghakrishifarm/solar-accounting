package com.legakrishi.solar.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "whatsapp")
public class WhatsAppProps {
    private boolean enabled = false;
    private String phoneNumberId;
    private String token;
    private String to;           // default recipient (test)
    private String template = "hello_world";
    private String language = "en_US";
}
