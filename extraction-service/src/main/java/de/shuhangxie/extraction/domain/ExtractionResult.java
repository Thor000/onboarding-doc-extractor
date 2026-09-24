package de.shuhangxie.extraction.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExtractionResult(
        UUID id,
        String filename,
        Instant createdAt,
        ExtractionStatus status,
        CustomerData data,
        List<ValidationIssue> issues,
        OcrInfo ocr,
        String model,
        long durationMs
) {
}
