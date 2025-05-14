package com.example.googol.controllers;

import java.rmi.RemoteException;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.example.googol.service.HackerNewsService;
import com.example.googol.service.OpenAIAnalysisService;

import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import search.*;

@Controller
public class SearchController {
    public static class SearchResult {
        private String link;
        private String titulo;
        private String citacao;

        public SearchResult(String link, String titulo, String citacao) {
            this.link = link;
            this.titulo = titulo;
            this.citacao = citacao;
        }

        public String getLink() {
            return link;
        }

        public String getTitulo() {
            return titulo;
        }

        public String getCitacao() {
            return citacao;
        }
    }

    @Autowired
    private IClientGateway gateway;

   // import com.example.googol.service.OpenAIAnalysisService;
    @Autowired
    private OpenAIAnalysisService analysisService;

    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping("/search")
    public String search(@RequestParam("query") String query,
            @RequestParam(value = "page", defaultValue = "1") int page, Model model) throws RemoteException {
        List<String> results = gateway.request_index(query, page);

        List<SearchResult> parsedResults = new ArrayList<>();
        int pages = 0;

        for (String result : results) {
            if (result.contains("URL:") && result.contains("Titulo:") && result.contains("Citacao:")) {
                try {
                    String[] urlSplit = result.split("URL: ");
                    String[] tituloSplit = urlSplit.length > 1 ? urlSplit[1].split(" Titulo: ")
                            : new String[] { "", "" };
                    String[] citacaoSplit = tituloSplit.length > 1 ? tituloSplit[1].split(" Citacao: ")
                            : new String[] { "", "" };

                    String link = tituloSplit[0].trim();
                    String titulo = citacaoSplit.length > 0 ? citacaoSplit[0].trim() : "";
                    String citacao = citacaoSplit.length > 1 ? citacaoSplit[1].trim() : "";

                    if (titulo.isEmpty()) {
                        titulo = link;
                    }

                    parsedResults.add(new SearchResult(link, titulo, citacao));
                } catch (Exception e) {
                    System.err.println("Erro ao processar resultado: " + result);
                }
            } else if (result.startsWith("tem ")) {
                try {
                    pages = Integer.parseInt(result.split(" ")[1].trim());
                } catch (NumberFormatException e) {
                    System.err.println("Erro ao converter número de páginas: " + result);
                }
            }
        }

        // Configuramos o usuário inicialmente sem análise
        model.addAttribute("analise", ""); 
        model.addAttribute("queryId", query.hashCode() + "-" + System.currentTimeMillis());
        model.addAttribute("currentPage", page);
        model.addAttribute("query", query);
        model.addAttribute("results", parsedResults);
        model.addAttribute("pages", pages);

        // Processamento assíncrono da análise Ollama - usando streaming
        List<String> snippets = parsedResults.stream()
            .map(SearchResult::getCitacao)
            .filter(c -> c != null && !c.isBlank())
            .limit(15)
            .toList();
            
        // Iniciar análise streaming de forma assíncrona
        analysisService.generateAnalysisStreaming(query, snippets, 
            query.hashCode() + "-" + System.currentTimeMillis());

        return "search_results";
    }

    @Autowired
    private HackerNewsService hackerNewsService;

    @PostMapping("/hn-index")
    public String indexHackerNews(@RequestParam("query") String query, Model model) {
        hackerNewsService.fetchTopStoriesMatching(query);
        return "redirect:/";
    }
}