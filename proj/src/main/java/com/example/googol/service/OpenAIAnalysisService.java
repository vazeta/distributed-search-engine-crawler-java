package com.example.googol.service;

import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.net.http.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OpenAIAnalysisService {

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    
    private String apiKey = "";

    private final SimpMessagingTemplate messagingTemplate;

    public OpenAIAnalysisService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public CompletableFuture<Void> generateAnalysisStreaming(
            String query, List<String> snippets, String queryId) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Monta mensagens no formato Chat
                List<Map<String,String>> messages = new ArrayList<>();
                messages.add(Map.of(
                    "role", "system",
                    "content", "Você é um assistente útil que resume trechos de texto."
                ));
                messages.add(Map.of(
                    "role", "user",
                    "content", buildPrompt(query, snippets)
                ));

                Map<String,Object> body = Map.of(
                    "model",   "gpt-3.5-turbo",
                    "messages", messages,
                    "stream",  true
                );

                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writeValueAsString(body);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

                StringBuilder acumulador = new StringBuilder();
                // Processa linha a linha (Server-Sent Events)
                client.send(request, HttpResponse.BodyHandlers.ofLines())
                      .body()
                      .forEach(line -> {
                          try {
                              if (line.isBlank() || !line.startsWith("data:")) return;
                              String s = line.substring(5).trim();
                              if ("[DONE]".equals(s)) return;
                              Map<?,?> chunk = mapper.readValue(s, Map.class);
                              List<?> choices = (List<?>) chunk.get("choices");
                              Map<?,?> delta = (Map<?,?>) ((Map<?,?>)choices.get(0)).get("delta");
                              if (delta.containsKey("content")) {
                                  String texto = delta.get("content").toString();
                                  acumulador.append(texto);
                                  messagingTemplate
                                      .convertAndSend("/topic/analysis/" + queryId,
                                                      acumulador.toString());
                              }
                          } catch (Exception e) {
                              // ignora pedaços mal-formados
                          }
                      });

                // Envia texto final (caso não tenha sido enviado no loop)
                messagingTemplate
                    .convertAndSend("/topic/analysis/" + queryId,
                                    acumulador.toString());

            } catch (Exception e) {
                messagingTemplate
                    .convertAndSend("/topic/analysis/" + queryId,
                                    "Erro ao gerar análise com OpenAI: " + e.getMessage());
            }
        });
    }

    private String buildPrompt(String query, List<String> snippets) {
        StringBuilder sb = new StringBuilder("Termo da pesquisa: \"" + query + "\".\n");
        sb.append("Resultados relevantes:\n");
        for (String snip : snippets) {
            sb.append("- ").append(snip).append("\n");
        }
        sb.append("Com base nos termos e trechos acima, gere um breve resumo em português "
                  + "para ser exibido no browser. Não inclua instruções na resposta.");
        return sb.toString();
    }
}
