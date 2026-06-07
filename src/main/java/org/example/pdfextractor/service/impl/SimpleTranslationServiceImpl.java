package org.example.pdfextractor.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.pdfextractor.service.TranslationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Translation service that uses OpenAI Chat Completions API (GPT model).
 * <p>
 * Configure via application.yml:
 * <pre>
 * open-ai:
 *   chat-model:
 *     api-key: sk-proj-...
 *     model: gpt-4o-mini        # optional, default: gpt-4o-mini
 *     api-url: https://api.openai.com/v1/chat/completions  # optional
 * </pre>
 * </p>
 */
@Slf4j
@Service
public class SimpleTranslationServiceImpl implements TranslationService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiUrl;
    private final String apiKey;
    private final String model;

    public SimpleTranslationServiceImpl(
            @Value("${open-ai.chat-model.api-key}") String apiKey,
            @Value("${open-ai.chat-model.model:gpt-4o-mini}") String model,
            @Value("${open-ai.chat-model.api-url:https://api.openai.com/v1/chat/completions}") String apiUrl
    ) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
        this.model = model;
        this.apiUrl = apiUrl;
    }

    @Override
    public String translate(String text, String sourceLang, String targetLang) {
        if (text == null || text.isBlank()) {
            return text;
        }

        log.debug("Translating text from {} to {} via OpenAI ({} chars)", sourceLang, targetLang, text.length());

        try {
            // Build the request payload for OpenAI Chat Completions API
            String jsonBody = buildTranslationPayload(text, sourceLang, targetLang);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode json = objectMapper.readTree(response.body());

                // OpenAI response structure:
                // { "choices": [ { "message": { "content": "translated text" } } ] }
                JsonNode choices = json.get("choices");
                if (choices != null && choices.isArray() && choices.size() > 0) {
                    JsonNode message = choices.get(0).get("message");
                    if (message != null && message.has("content")) {
                        String translatedText = message.get("content").asText().trim();
                        log.info("OpenAI translation complete: {} chars -> {} chars", text.length(), translatedText.length());
                        return translatedText;
                    }
                }

                log.warn("Unexpected OpenAI response format: {}", response.body());
                return fallbackTranslation(text);
            } else {
                log.warn("OpenAI API returned status {}: {}", response.statusCode(), response.body());
                return fallbackTranslation(text);
            }

        } catch (Exception e) {
            log.error("OpenAI API call failed: {}", e.getMessage(), e);
            return fallbackTranslation(text);
        }
    }

    /**
     * Builds the JSON payload for an OpenAI Chat Completions request with a translation prompt.
     */
    private String buildTranslationPayload(String text, String sourceLang, String targetLang) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", model);

        String systemPrompt = String.format(
                "You are a professional translator. Translate the following text from %s to %s accurately. " +
                "Preserve the original formatting, line breaks, and structure. " +
                "Return ONLY the translated text, without any additional explanations, notes, or metadata.",
                sourceLang, targetLang);

        ArrayNode messages = root.putArray("messages");
        ObjectNode systemMsg = messages.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);

        ObjectNode userMsg = messages.addObject();
        userMsg.put("role", "user");
        userMsg.put("content", text);

        // Use lower temperature for more deterministic translations
        root.put("temperature", 0.3);

        return root.toString();
    }

    /**
     * Fallback: returns original text with a marker if API is unavailable.
     */
    private String fallbackTranslation(String text) {
        String fallback = "[[RU: " + text + "]]";
        log.info("Using fallback translation (stub): {} chars", fallback.length());
        return fallback;
    }
}
