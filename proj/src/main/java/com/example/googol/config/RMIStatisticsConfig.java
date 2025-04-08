package com.example.googol.config;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import search.StatisticsService;

@Configuration
public class RMIStatisticsConfig {

    @Bean
    public StatisticsService statisticsService() throws RemoteException, NotBoundException, MalformedURLException {
        // Certifique-se de que a URL do lookup está correta, conforme registrado no RMI Registry.
        return (StatisticsService) Naming.lookup("StatisticsService");
    }
}
