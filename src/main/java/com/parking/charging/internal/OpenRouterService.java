package com.parking.charging.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
class OpenRouterService {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterService.class);

    private final RestClient restClient;

    OpenRouterService(@Value("${openrouter.api-key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl("https://openrouter.ai/api/v1")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Sends a base64-encoded image to a vision model and asks it to extract the license plate.
     * Returns the raw text response from the model (the plate number, or empty if not found).
     */
    String extractLicensePlate(String base64Image) {
        var body = Map.of(
                "model", "google/gemini-2.0-flash-001",
                "messages", List.of(
                        Map.of("role", "user", "content", List.of(
                                Map.of("type", "image_url",
                                        "image_url", Map.of("url", "data:image/jpeg;base64," + base64Image)),
                                Map.of("type", "text",
                                        "text", "This image may show a handwritten license plate on a piece of paper. " +
                                                "Read the text written on the paper and return it as a license plate number. " +
                                                "Return ONLY the alphanumeric characters, no spaces, dashes, or extra text. " +
                                                "If nothing is readable, return empty string.")
                        ))
                )
        );

        try {
            var response = restClient.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            @SuppressWarnings("unchecked")
            var choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) return "";

            @SuppressWarnings("unchecked")
            var message = (Map<String, Object>) choices.get(0).get("message");
            if (message == null) return "";

            String content = (String) message.get("content");
            log.info("LLM raw response: '{}'", content);
            String normalized = content == null ? "" : content.trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
            log.info("LLM normalized plate: '{}'", normalized);
            return normalized;
        } catch (Exception e) {
            log.error("OpenRouter plate extraction failed", e);
            return "";
        }
    }
}
