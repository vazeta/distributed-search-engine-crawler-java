package com.example.googol.service;

import org.springframework.stereotype.Service;
import java.net.http.*;
import java.net.URI;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import search.StorageUtil;

@Service
public class OpenAIAnalysisService {

    private static final String API_KEY = StorageUtil.getkey();
    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    public String generateAnalysis(String query, List<String> snippets) throws Exception {
        String prompt = buildPrompt(query, snippets);
        System.out.println(API_KEY);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_URL))
                .header("Authorization", "Bearer " + API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                            "model": "gpt-3.5-turbo",
                            "messages": [
                                {"role": "system", "content": "És um assistente que analisa resultados de pesquisa."},
                                {"role": "user", "content": %s}
                            ],
                            "max_tokens": 300
                        }
                        """.formatted(new ObjectMapper().writeValueAsString(prompt))))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> result = new ObjectMapper().readValue(response.body(), Map.class);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) result.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> choice = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) choice.get("message");
            if (message != null && message.get("content") != null) {
                return message.get("content").toString();
            }
        }
        throw new RuntimeException("Resposta inesperada da OpenAI: " + response.body());

    }

    private String buildPrompt(String query, List<String> snippets) {
        StringBuilder sb = new StringBuilder("Termo da pesquisa: \"" + query + "\".\n");
        sb.append("Resultados relevantes:\n");
        for (String snippet : snippets) {
            sb.append("- ").append(snippet).append("\n");
        }
        sb.append("Gere uma análise baseada nos termos e nos resultados acima.");
        return sb.toString();
    }
}
