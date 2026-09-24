package de.shuhangxie.extraction.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.shuhangxie.extraction.domain.CustomerData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OllamaExtractorTest {

    private MockRestServiceServer server;
    private OllamaExtractor extractor;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://ollama:11434");
        server = MockRestServiceServer.bindTo(builder).build();
        extractor = new OllamaExtractor(builder.build(), "qwen2.5:3b", new LlmResponseParser(new ObjectMapper()));
    }

    @Test
    void sendsSchemaAndParsesStructuredAnswer() {
        server.expect(requestTo("http://ollama:11434/api/chat"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.model").value("qwen2.5:3b"))
                .andExpect(jsonPath("$.stream").value(false))
                .andExpect(jsonPath("$.options.temperature").value(0))
                .andExpect(jsonPath("$.format.properties.vatId.type").value("string"))
                .andExpect(jsonPath("$.format.properties.standards.type").value("array"))
                .andExpect(jsonPath("$.messages[1].content").value(org.hamcrest.Matchers.containsString("Nordlicht")))
                .andRespond(withSuccess("""
                        {"model": "qwen2.5:3b", "done": true,
                         "message": {"role": "assistant",
                                     "content": "{\\"companyName\\": \\"Nordlicht Funktechnik GmbH\\", \\"vatId\\": \\"DE812345673\\", \\"standards\\": []}"}}
                        """, MediaType.APPLICATION_JSON));

        CustomerData data = extractor.extract("Firma Nordlicht Funktechnik GmbH");

        assertThat(data.companyName()).isEqualTo("Nordlicht Funktechnik GmbH");
        assertThat(data.vatId()).isEqualTo("DE812345673");
        server.verify();
    }

    @Test
    void wrapsServerErrors() {
        server.expect(requestTo("http://ollama:11434/api/chat")).andRespond(withServerError());

        assertThatThrownBy(() -> extractor.extract("irgendein Text"))
                .isInstanceOf(LlmException.class)
                .hasMessageContaining("Ollama");
    }
}
