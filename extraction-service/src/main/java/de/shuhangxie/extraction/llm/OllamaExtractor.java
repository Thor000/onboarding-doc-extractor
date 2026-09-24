package de.shuhangxie.extraction.llm;

import com.fasterxml.jackson.databind.JsonNode;
import de.shuhangxie.extraction.domain.CustomerData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Ruft ein lokal laufendes Sprachmodell über die Ollama-REST-API auf.
 * Es verlassen keine Dokumentdaten das eigene Netz.
 */
public class OllamaExtractor implements LlmExtractor {

    private static final Logger log = LoggerFactory.getLogger(OllamaExtractor.class);

    private final RestClient restClient;
    private final String model;
    private final LlmResponseParser parser;

    public OllamaExtractor(RestClient restClient, String model, LlmResponseParser parser) {
        this.restClient = restClient;
        this.model = model;
        this.parser = parser;
    }

    @Override
    public CustomerData extract(String documentText) {
        Map<String, Object> request = Map.of(
                "model", model,
                "stream", false,
                "format", ExtractionPrompt.jsonSchema(),
                // Temperatur 0: dieselbe Eingabe soll dieselbe Ausgabe liefern
                "options", Map.of("temperature", 0),
                "messages", List.of(
                        Map.of("role", "system", "content", ExtractionPrompt.SYSTEM),
                        Map.of("role", "user", "content", ExtractionPrompt.userMessage(documentText))));

        long started = System.currentTimeMillis();
        JsonNode response;
        try {
            response = restClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            throw new LlmException("Ollama nicht erreichbar oder Fehler bei der Anfrage: " + e.getMessage(), e);
        }
        log.info("Ollama ({}) antwortete nach {} ms", model, System.currentTimeMillis() - started);
        return parser.parseChatResponse(response);
    }

    @Override
    public String modelName() {
        return model;
    }
}
