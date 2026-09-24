package de.shuhangxie.extraction.service;

import de.shuhangxie.extraction.config.ExtractorProperties;
import de.shuhangxie.extraction.domain.CustomerData;
import de.shuhangxie.extraction.domain.ExtractionResult;
import de.shuhangxie.extraction.domain.ExtractionStatus;
import de.shuhangxie.extraction.domain.OcrInfo;
import de.shuhangxie.extraction.domain.ValidationIssue;
import de.shuhangxie.extraction.llm.LlmExtractor;
import de.shuhangxie.extraction.ocr.OcrClient;
import de.shuhangxie.extraction.ocr.OcrResponse;
import de.shuhangxie.extraction.validation.CustomerDataValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Ablauf: Dokument → OCR-Service → Sprachmodell → Validierung → Ergebnis.
 */
@Service
public class ExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ExtractionService.class);

    private final OcrClient ocrClient;
    private final LlmExtractor llmExtractor;
    private final CustomerDataValidator validator;
    private final ResultRepository repository;
    private final ExtractorProperties properties;
    private final Clock clock;

    public ExtractionService(OcrClient ocrClient, LlmExtractor llmExtractor, CustomerDataValidator validator,
                             ResultRepository repository, ExtractorProperties properties, Clock clock) {
        this.ocrClient = ocrClient;
        this.llmExtractor = llmExtractor;
        this.validator = validator;
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    public ExtractionResult process(String filename, byte[] content) {
        Instant started = clock.instant();

        OcrResponse ocr = ocrClient.recognize(filename, content);
        List<ValidationIssue> issues = new ArrayList<>();
        if (ocr.meanConfidence() != null && ocr.meanConfidence() < properties.minOcrConfidence()) {
            issues.add(ValidationIssue.warning("document", "LOW_OCR_CONFIDENCE",
                    "Geringe OCR-Qualität (%.0f %%) – Werte besonders sorgfältig prüfen".formatted(ocr.meanConfidence())));
        }

        CustomerData data = llmExtractor.extract(ocr.text());
        issues.addAll(validator.validate(data, ocr.text()));

        ExtractionStatus status = issues.isEmpty() ? ExtractionStatus.VALID : ExtractionStatus.NEEDS_REVIEW;
        long durationMs = clock.millis() - started.toEpochMilli();
        ExtractionResult result = new ExtractionResult(
                UUID.randomUUID(),
                filename,
                started,
                status,
                data,
                List.copyOf(issues),
                new OcrInfo(ocr.method(), ocr.meanConfidence(),
                        ocr.pages() == null ? 0 : ocr.pages().size(),
                        ocr.text() == null ? 0 : ocr.text().length()),
                llmExtractor.modelName(),
                durationMs);

        log.info("{} verarbeitet: {} ({} Hinweise, {} ms)", filename, status, issues.size(), durationMs);
        return repository.save(result);
    }
}
