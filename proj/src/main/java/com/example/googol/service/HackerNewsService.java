// src/main/java/com/example/googol/service/HackerNewsService.java
package com.example.googol.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import search.IClientGateway;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class HackerNewsService {

    @Autowired
    private IClientGateway gateway;
    private final ExecutorService executor = Executors.newFixedThreadPool(20); // ajusta conforme os recursos
                                                                               // disponíveis
    private final RestTemplate restTemplate = new RestTemplate();
    private static final String BASE_URL = "https://hacker-news.firebaseio.com/v0/";

    @Async
    public void fetchTopStoriesMatching(String query) {
        String[] terms = query.toLowerCase().split("\\s+");
        List<String> matchedUrls = new ArrayList<>();

        String[] storyIds = restTemplate.getForObject(BASE_URL + "topstories.json", String[].class);
        if (storyIds == null)
            return;

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < Math.min(storyIds.length, 5000); i++) { // 10000 é um exagero
            String id = storyIds[i];
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    Map<String, Object> story = restTemplate.getForObject(BASE_URL + "item/" + id + ".json", Map.class);
                    if (story == null || !story.containsKey("text") || !story.containsKey("url"))
                        return;

                    String title = story.get("text").toString().toLowerCase();
                    for (String term : terms) {
                        if (!title.contains(term))
                            return;
                    }

                    synchronized (matchedUrls) {
                        matchedUrls.add(story.get("url").toString());
                    }
                } catch (Exception e) {
                    System.out.println("Erro ao processar story ID: " + id);
                }
            }, executor));
        }

        // Espera por todas as tarefas
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        System.out.println(matchedUrls.size() + " links vindos da query - " + query);
        for (String url : matchedUrls) {
            try {
                gateway.addUrlToQueue(url);
            } catch (Exception e) {
                System.out.println("Erro no hacker news a adicionar à queue");
            }
        }
    }

}
