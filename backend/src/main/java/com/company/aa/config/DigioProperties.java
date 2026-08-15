package com.company.aa.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "digio")
public record DigioProperties(
        String baseUrl,
        String username,
        String password,
        String templateId,
        String webhookSecret,
        int connectTimeoutMs,
        int readTimeoutMs
) {
}
