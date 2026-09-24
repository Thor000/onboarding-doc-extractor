package de.shuhangxie.extraction.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.shuhangxie.extraction.domain.CustomerData;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmResponseParserTest {

    private final LlmResponseParser parser = new LlmResponseParser(new ObjectMapper());

    @Test
    void mapsEmptyStringsToNullAndIgnoresUnknownFields() {
        CustomerData data = parser.parseContent("""
                {"companyName": "Kappa Automotive Systems GmbH", "street": "", "vatId": "DE445120871",
                 "standards": ["EN 300 220-2", " "], "confidence": 0.9}
                """);

        assertThat(data.companyName()).isEqualTo("Kappa Automotive Systems GmbH");
        assertThat(data.street()).isNull();
        assertThat(data.standards()).containsExactly("EN 300 220-2");
    }

    @Test
    void stripsMarkdownCodeFence() {
        CustomerData data = parser.parseContent("```json\n{\"companyName\": \"ACME\"}\n```");

        assertThat(data.companyName()).isEqualTo("ACME");
    }

    @Test
    void rejectsNonJsonContent() {
        assertThatThrownBy(() -> parser.parseContent("Ich konnte leider nichts finden."))
                .isInstanceOf(LlmException.class);
    }
}
