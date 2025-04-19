package com.example.googol.config;

import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.rmi.Naming;
import java.net.MalformedURLException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import com.example.googol.controllers.StatisticsWebSocketController;

import search.*;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class RMIConfig {

    @Bean
    public IClientGateway gateway() throws RemoteException, NotBoundException, MalformedURLException {
        return (IClientGateway) Naming.lookup("GatewayService");
    }

    @Bean
    public StatisticsServiceImpl statisticsServiceImpl() throws RemoteException {
        return new StatisticsServiceImpl(); // Spring vai controlar essa instância
    }

    // Bean separado para configurar o controlador no serviço
    @Bean
    public StatisticsServiceConfigurator statisticsServiceConfigurator(
            StatisticsServiceImpl service,
            StatisticsWebSocketController controller) {
        service.setWebSocketController(controller);
        System.out.println("WebSocketController configurado com sucesso!");
        return new StatisticsServiceConfigurator();
    }
}
