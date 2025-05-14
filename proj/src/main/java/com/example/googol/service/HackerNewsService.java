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

@Service
public class HackerNewsService {

    @Autowired
    private IClientGateway gateway;

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String BASE_URL = "https://hacker-news.firebaseio.com/v0/";
    @Async
    public void fetchTopStoriesMatching(String query) {
        String[] terms = query.toLowerCase().split("\\s+");
        List<String> matchedUrls = new ArrayList<>();

        String[] storyIds = restTemplate.getForObject(BASE_URL + "topstories.json", String[].class);

        if (storyIds == null) return;

        for (int i = 0; i < Math.min(storyIds.length, 10000); i++) {
            String id = storyIds[i];
            Map<String, Object> story = restTemplate.getForObject(BASE_URL + "item/" + id + ".json", Map.class);
            if (story == null || !story.containsKey("text") || !story.containsKey("url")) continue;

            String title = story.get("text").toString().toLowerCase();
            boolean allTermsMatch = true;
            for (String term : terms) {
                if (!title.contains(term)) {
                    allTermsMatch = false;
                    break;
                }
            }

            if (allTermsMatch) {
                matchedUrls.add(story.get("url").toString());
            }
        }
        System.out.println(matchedUrls.size()+" links vindos da query" + "-"+query);
        for(String url:matchedUrls){
            try {
                gateway.addUrlToQueue(url);
            } catch (Exception e) {
                System.out.println("Erro no hacker news a adicionar a queue");
            }
            
        }
    }
}
