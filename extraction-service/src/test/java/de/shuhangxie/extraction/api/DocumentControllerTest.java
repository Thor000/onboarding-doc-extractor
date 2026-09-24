package de.shuhangxie.extraction.api;

import de.shuhangxie.extraction.TestData;
import de.shuhangxie.extraction.domain.ExtractionResult;
import de.shuhangxie.extraction.domain.ExtractionStatus;
import de.shuhangxie.extraction.domain.OcrInfo;
import de.shuhangxie.extraction.llm.LlmException;
import de.shuhangxie.extraction.llm.LlmExtractor;
import de.shuhangxie.extraction.ocr.OcrClient;
import de.shuhangxie.extraction.service.ExtractionService;
import de.shuhangxie.extraction.service.ResultRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ExtractionService extractionService;
    @MockitoBean
    private ResultRepository repository;
    // Nur damit der Kontext nicht versucht, echte HTTP-Clients aufzubauen
    @MockitoBean
    private OcrClient ocrClient;
    @MockitoBean
    private LlmExtractor llmExtractor;

    private final MockMultipartFile scan =
            new MockMultipartFile("file", "scan.png", "image/png", new byte[]{1, 2, 3});

    @Test
    void uploadReturnsCreatedResult() throws Exception {
        ExtractionResult result = result();
        when(extractionService.process(eq("scan.png"), any())).thenReturn(result);

        mvc.perform(multipart("/api/documents").file(scan))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/documents/" + result.id()))
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.data.vatId").value("DE812345673"))
                .andExpect(jsonPath("$.data.standards.length()").value(3));
    }

    @Test
    void emptyUploadIsBadRequest() throws Exception {
        mvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile("file", "leer.png", "image/png", new byte[0])))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Ungültiger Upload"));
    }

    @Test
    void llmFailureIsBadGateway() throws Exception {
        when(extractionService.process(any(), any())).thenThrow(new LlmException("Ollama nicht erreichbar"));

        mvc.perform(multipart("/api/documents").file(scan))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("Ollama nicht erreichbar"));
    }

    @Test
    void unknownIdIsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        mvc.perform(get("/api/documents/{id}", id))
                .andExpect(status().isNotFound());
    }

    private static ExtractionResult result() {
        return new ExtractionResult(UUID.randomUUID(), "scan.png", Instant.parse("2026-09-21T10:00:00Z"),
                ExtractionStatus.VALID, TestData.validCustomer(), List.of(),
                new OcrInfo("ocr", 91.5, 1, 420), "qwen2.5:3b", 5300);
    }
}
