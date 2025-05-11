package com.example.googol.service;

import org.springframework.stereotype.Service;
import java.net.http.*;
import java.net.URI;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OllamaAnalysisService {

    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";

    public String generateAnalysis(String query, List<String> snippets) throws Exception {
        String prompt = buildPrompt(query, snippets);

        HttpClient client = HttpClient.newHttpClient();
        Map<String, Object> requestBody = Map.of(
            "model", "llama3",
            "prompt", prompt,
            "stream", false
        );

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(OLLAMA_URL))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        Map<String, Object> result = mapper.readValue(response.body(), Map.class);
        if (!result.containsKey("response")) {
            throw new RuntimeException("Erro ao extrair resposta da Ollama: " + response.body());
        }

        return result.get("response").toString().trim();
    }

    private String buildPrompt(String query, List<String> snippets) {
        StringBuilder sb = new StringBuilder("Termo da pesquisa: \"" + query + "\".\n");
        sb.append("Resultados relevantes:\n");
        for (String snippet : snippets) {
            sb.append("- ").append(snippet).append("\n");
        }
        sb.append("Gere um texto pequeno para um browser em portugues baseada nos termos e nos resultados acima. Nao inclua esta informacao na sua resposta.");
        return sb.toString();
    }
}
