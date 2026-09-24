package de.shuhangxie.extraction.service;

import de.shuhangxie.extraction.TestData;
import de.shuhangxie.extraction.config.ExtractorProperties;
import de.shuhangxie.extraction.domain.ExtractionResult;
import de.shuhangxie.extraction.domain.ExtractionStatus;
import de.shuhangxie.extraction.domain.ValidationIssue;
import de.shuhangxie.extraction.llm.LlmExtractor;
import de.shuhangxie.extraction.ocr.OcrClient;
import de.shuhangxie.extraction.ocr.OcrResponse;
import de.shuhangxie.extraction.validation.CustomerDataValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExtractionServiceTest {

    private final OcrClient ocrClient = mock(OcrClient.class);
    private final LlmExtractor llm = mock(LlmExtractor.class);
    private final ResultRepository repository = new ResultRepository();
    private ExtractionService service;

    @BeforeEach
    void setUp() {
        ExtractorProperties properties = new ExtractorProperties(null, null, 70);
        Clock clock = Clock.fixed(Instant.parse("2026-09-21T10:00:00Z"), ZoneOffset.UTC);
        service = new ExtractionService(ocrClient, llm, new CustomerDataValidator(), repository, properties, clock);
        when(llm.modelName()).thenReturn("test-model");
    }

    @Test
    void validDocumentIsAcceptedAndStored() {
        when(ocrClient.recognize(anyString(), any())).thenReturn(ocr(91.5));
        when(llm.extract(TestData.OCR_TEXT)).thenReturn(TestData.validCustomer());

        ExtractionResult result = service.process("scan.png", new byte[]{1});

        assertThat(result.status()).isEqualTo(ExtractionStatus.VALID);
        assertThat(result.issues()).isEmpty();
        assertThat(result.ocr().method()).isEqualTo("ocr");
        assertThat(result.model()).isEqualTo("test-model");
        assertThat(repository.findById(result.id())).contains(result);
    }

    @Test
    void wrongCheckDigitRequiresReview() {
        String text = TestData.OCR_TEXT + "\nDE812345674";
        when(ocrClient.recognize(anyString(), any())).thenReturn(ocr(text, 91.5));
        when(llm.extract(text)).thenReturn(TestData.withVatId("DE812345674"));

        ExtractionResult result = service.process("scan.png", new byte[]{1});

        assertThat(result.status()).isEqualTo(ExtractionStatus.NEEDS_REVIEW);
        assertThat(result.issues()).extracting(ValidationIssue::code).containsExactly("INVALID_CHECKSUM");
    }

    @Test
    void lowOcrConfidenceIsReported() {
        when(ocrClient.recognize(anyString(), any())).thenReturn(ocr(55.0));
        when(llm.extract(TestData.OCR_TEXT)).thenReturn(TestData.validCustomer());

        ExtractionResult result = service.process("blurry.jpg", new byte[]{1});

        assertThat(result.status()).isEqualTo(ExtractionStatus.NEEDS_REVIEW);
        assertThat(result.issues()).extracting(ValidationIssue::code).containsExactly("LOW_OCR_CONFIDENCE");
    }

    private static OcrResponse ocr(double confidence) {
        return ocr(TestData.OCR_TEXT, confidence);
    }

    private static OcrResponse ocr(String text, double confidence) {
        return new OcrResponse("scan.png", text,
                List.of(new OcrResponse.Page(1, "ocr", confidence, text.length())), confidence, 1200);
    }
}
