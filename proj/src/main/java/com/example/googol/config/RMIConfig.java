package com.example.googol.config;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;
import java.rmi.Naming;
import java.net.MalformedURLException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import search.*;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass=true)
public class RMIConfig {

    @Bean
    public IClientGateway gateway() throws RemoteException, NotBoundException, MalformedURLException {
        return (IClientGateway) Naming.lookup("GatewayService");
    }

}
