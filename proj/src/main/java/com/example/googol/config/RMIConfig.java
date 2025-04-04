package com.example.googol.config;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.rmi.Naming;
import java.net.MalformedURLException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import java.util.List;
import search.*;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass=true)  // Isso força o uso de proxies baseados em classe.
public class RMIConfig {

    @Bean
    public IClientGateway gateway() throws RemoteException, NotBoundException, MalformedURLException {
        return (IClientGateway) Naming.lookup("GatewayService");
    }

}
