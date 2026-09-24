package de.shuhangxie.extraction.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.shuhangxie.extraction.domain.CustomerData;

/** Liest die Antwort von Ollamas /api/chat und wandelt den JSON-Inhalt in {@link CustomerData} um. */
public class LlmResponseParser {

    private final ObjectMapper mapper;

    public LlmResponseParser(ObjectMapper mapper) {
        this.mapper = mapper.copy().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public CustomerData parseChatResponse(JsonNode response) {
        if (response == null) {
            throw new LlmException("Leere Antwort vom Sprachmodell");
        }
        String content = response.path("message").path("content").asText("");
        return parseContent(content);
    }

    public CustomerData parseContent(String content) {
        String json = stripCodeFence(content);
        if (json.isBlank()) {
            throw new LlmException("Sprachmodell hat keinen Inhalt geliefert");
        }
        try {
            return mapper.readValue(json, CustomerData.class);
        } catch (JsonProcessingException e) {
            throw new LlmException("Antwort des Sprachmodells ist kein gültiges JSON", e);
        }
    }

    /** Manche Modelle verpacken JSON trotz Schema in ```json … ``` – das wird hier entfernt. */
    static String stripCodeFence(String content) {
        String trimmed = content == null ? "" : content.strip();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).strip();
            }
        }
        return trimmed;
    }
}
