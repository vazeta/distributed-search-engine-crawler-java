package com.example.googol.controllers;

import java.rmi.RemoteException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import search.Statistics;
import search.StatisticsService;

@Controller
public class StatisticsWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

     @Autowired
    private StatisticsService statisticsService;


    @MessageMapping("/requestStats")
    public void requestStats() throws RemoteException{
        try {
            Statistics stats = statisticsService.getStats();
            sendStats(stats);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void sendStats(Statistics stats) {
        if (stats == null) {
            System.out.println("opa");
        } else {
            messagingTemplate.convertAndSend("/topic/stats", stats);
        }
    }
}
