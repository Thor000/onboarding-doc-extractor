package de.shuhangxie.extraction.ocr;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** REST-Client für den OCR-Microservice. */
public class OcrClient {

    private final RestClient restClient;

    public OcrClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public OcrResponse recognize(String filename, byte[] content) {
        MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        try {
            OcrResponse response = restClient.post()
                    .uri("/ocr")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(parts)
                    .retrieve()
                    .body(OcrResponse.class);
            if (response == null) {
                throw new OcrException("Leere Antwort vom OCR-Service", null);
            }
            return response;
        } catch (RestClientException e) {
            throw new OcrException("OCR-Service nicht erreichbar oder Fehler bei der Texterkennung: " + e.getMessage(), e);
        }
    }
}
