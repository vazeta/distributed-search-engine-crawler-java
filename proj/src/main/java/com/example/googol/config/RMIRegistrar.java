package com.example.googol.config;

import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import search.StatisticsServiceImpl;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

@Component
public class RMIRegistrar {

    @Autowired
    private StatisticsServiceImpl statisticsServiceImpl;

    @PostConstruct
    public void registerRMI() {
        try {
            Registry registry = LocateRegistry.createRegistry(1100);
            registry.rebind("StatisticsService", statisticsServiceImpl);
            System.out.println("Registrado no RMI com sucesso!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
