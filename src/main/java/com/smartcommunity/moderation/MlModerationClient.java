package com.smartcommunity.moderation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Component
public class MlModerationClient {

    @Value("${app.ml-service.url:http://localhost:8000}")
    private String mlServiceUrl;

    @Value("${app.ml-service.confidence-threshold:0.75}")
    private double defaultConfidenceThreshold;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final Set<String> HIGH_SEVERITY_WORDS = Set.of(
            "kill", "die", "attack", "bomb", "shoot", "threat"
    );

    private static final Set<String> PROFANITY_WORDS = Set.of(
            "hate", "idiot", "stupid", "moron", "trash", "loser"
    );

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassificationResult {
        private String category;
        private double confidenceScore;
        private String modelVersion;
        private boolean flagged;
    }

    public ClassificationResult classifyText(String text) {
        if (text == null || text.isBlank()) {
            return ClassificationResult.builder()
                    .category("non-toxic")
                    .confidenceScore(0.99)
                    .modelVersion("heuristic-fallback-v1")
                    .flagged(false)
                    .build();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> requestBody = Map.of("text", text);
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            Map<?, ?> response = restTemplate.postForObject(mlServiceUrl + "/predict", entity, Map.class);

            if (response != null && response.containsKey("category")) {
                String category = (String) response.get("category");
                Number scoreNum = (Number) response.get("confidence_score");
                double score = scoreNum != null ? scoreNum.doubleValue() : 0.85;
                Object versionObj = response.get("model_version");
                String version = versionObj != null ? versionObj.toString() : "fastapi-classifier-v1";

                boolean flagged = !"non-toxic".equalsIgnoreCase(category) && score >= defaultConfidenceThreshold;

                return ClassificationResult.builder()
                        .category(category)
                        .confidenceScore(score)
                        .modelVersion(version)
                        .flagged(flagged)
                        .build();
            }
        } catch (Exception ex) {
            log.warn("FastAPI ML service unreachable at {}. Activating heuristic moderation fallback. Reason: {}",
                    mlServiceUrl, ex.getMessage());
        }

        return evaluateHeuristics(text);
    }

    private ClassificationResult evaluateHeuristics(String text) {
        String lower = text.toLowerCase();

        for (String word : HIGH_SEVERITY_WORDS) {
            if (Pattern.compile("\\b" + Pattern.quote(word) + "\\b").matcher(lower).find()) {
                return ClassificationResult.builder()
                        .category("threat")
                        .confidenceScore(0.88)
                        .modelVersion("heuristic-fallback-v1")
                        .flagged(true)
                        .build();
            }
        }

        for (String word : PROFANITY_WORDS) {
            if (Pattern.compile("\\b" + Pattern.quote(word) + "\\b").matcher(lower).find()) {
                return ClassificationResult.builder()
                        .category("vulgar")
                        .confidenceScore(0.80)
                        .modelVersion("heuristic-fallback-v1")
                        .flagged(true)
                        .build();
            }
        }

        return ClassificationResult.builder()
                .category("non-toxic")
                .confidenceScore(0.95)
                .modelVersion("heuristic-fallback-v1")
                .flagged(false)
                .build();
    }
}
