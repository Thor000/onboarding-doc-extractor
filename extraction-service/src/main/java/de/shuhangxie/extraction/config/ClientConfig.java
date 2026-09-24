package de.shuhangxie.extraction.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.shuhangxie.extraction.llm.LlmExtractor;
import de.shuhangxie.extraction.llm.LlmResponseParser;
import de.shuhangxie.extraction.llm.OllamaExtractor;
import de.shuhangxie.extraction.ocr.OcrClient;
import de.shuhangxie.extraction.validation.CustomerDataValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;

@Configuration
public class ClientConfig {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    OcrClient ocrClient(RestClient.Builder builder, ExtractorProperties properties) {
        RestClient restClient = builder
                .baseUrl(properties.ocr().baseUrl())
                .requestFactory(requestFactory(properties.ocr().timeout()))
                .build();
        return new OcrClient(restClient);
    }

    @Bean
    LlmExtractor llmExtractor(RestClient.Builder builder, ExtractorProperties properties, ObjectMapper objectMapper) {
        RestClient restClient = builder
                .baseUrl(properties.llm().baseUrl())
                .requestFactory(requestFactory(properties.llm().timeout()))
                .build();
        return new OllamaExtractor(restClient, properties.llm().model(), new LlmResponseParser(objectMapper));
    }

    @Bean
    CustomerDataValidator customerDataValidator() {
        return new CustomerDataValidator();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    private static JdkClientHttpRequestFactory requestFactory(Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                // Ollama und Uvicorn sprechen HTTP/1.1; der Upgrade-Versuch auf HTTP/2 wird so vermieden
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);
        return factory;
    }
}
