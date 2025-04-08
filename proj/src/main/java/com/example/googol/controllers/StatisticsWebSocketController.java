package com.example.googol.controllers;

import java.rmi.RemoteException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import search.Statistics;
import search.StatisticsService;

@Controller
public class StatisticsWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private StatisticsService statisticsService;
    
    // Dispara a cada 5 segundos (ajuste conforme necessário)
    @Scheduled(fixedDelay = 1)
    public void sendStats() {
        try {
            // Obtém as estatísticas atuais via RMI (pode incluir top 10, tempo de resposta, etc.)
            Statistics stats = statisticsService.getStats();
            // Envia para todos os clientes conectados no destino /topic/stats
            messagingTemplate.convertAndSend("/topic/stats", stats);
        } catch (RemoteException e) {
            System.err.println("Erro ao obter estatísticas: " + e.getMessage());
        }
    }
}
