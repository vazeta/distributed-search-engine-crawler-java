package com.example.googol.service;

import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import java.net.http.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OllamaAnalysisService {

    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public CompletableFuture<Void> generateAnalysisStreaming(String query, List<String> snippets, String queryId) {
        return CompletableFuture.runAsync(() -> {
            try {
                String prompt = buildPrompt(query, snippets);
                HttpClient client = HttpClient.newHttpClient();
                
                Map<String, Object> requestBody = Map.of(
                    "model", "llama3",
                    "prompt", prompt,
                    "stream", true  // Habilita streaming
                );

                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writeValueAsString(requestBody);

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

                StringBuilder fullText = new StringBuilder();
                
                // Envia a solicitação e processa as respostas em tempo real
                client.send(request, HttpResponse.BodyHandlers.ofLines()).body().forEach(line -> {
                    try {
                        Map<String, Object> chunk = mapper.readValue(line, Map.class);
                        if (chunk.containsKey("response")) {
                            String partial = chunk.get("response").toString();
                            fullText.append(partial);
                            
                            // Envia a análise atual para o cliente WebSocket
                            messagingTemplate.convertAndSend("/topic/analysis/" + queryId, fullText.toString());
                        }
                    } catch (Exception e) {
                        System.err.println("Erro ao processar chunk: " + e.getMessage());
                    }
                });
                
                // Finaliza o streaming
                messagingTemplate.convertAndSend("/topic/analysis/" + queryId, fullText.toString());
                
            } catch (Exception e) {
                messagingTemplate.convertAndSend("/topic/analysis/" + queryId, 
                    "Erro ao gerar análise com Ollama: " + e.getMessage());
            }
        });
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