package de.shuhangxie.extraction.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "extractor")
public record ExtractorProperties(
        Endpoint ocr,
        Llm llm,
        double minOcrConfidence
) {
    public record Endpoint(String baseUrl, Duration timeout) {
    }

    public record Llm(String baseUrl, String model, Duration timeout) {
    }
}
