package com.mindhaven.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public record AiProperties(String provider, String baseUrl, String apiKey, String model,
                           Duration connectTimeout, Duration requestTimeout, int maxTokens) {
    public AiProperties {
        if (!List.of("deepseek", "local").contains(provider == null ? "" : provider)) {
            throw new IllegalArgumentException("AI_PROVIDER must be deepseek or local");
        }
        if (model == null || model.isBlank() || baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("DeepSeek base URL and model must not be empty");
        }
        if (connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()
                || requestTimeout == null || requestTimeout.isNegative() || requestTimeout.isZero()
                || requestTimeout.compareTo(Duration.ofSeconds(90)) > 0 || maxTokens < 1 || maxTokens > 4096) {
            throw new IllegalArgumentException("AI timeouts must be positive, request timeout <= 90s, max tokens 1-4096");
        }
    }
}
