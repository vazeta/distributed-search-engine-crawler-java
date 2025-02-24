package search;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Gateway extends UnicastRemoteObject implements IClientGateway {
    
    public Gateway() throws RemoteException {
        super();
        gatewayReg(); // Chama o método que regista o serviço RMI
    }

    private void gatewayReg() {
        try {
            Registry registry;
            try {
                // Primeiro, tenta criar um novo RMI Registry
                registry = LocateRegistry.createRegistry(1099);
                System.out.println("Novo RMI Registry criado na porta 1099.");
            } catch (RemoteException e) {
                // Se já existir, apenas conecta-se a ele
                System.out.println("RMI Registry já existente. Conectando...");
                registry = LocateRegistry.getRegistry(1099);
            }

            // Regista o `Gateway` no RMI
            registry.rebind("GatewayService", this);
            System.out.println("Gateway RMI registrado com sucesso.");
            
        } catch (RemoteException e) {
            System.out.println("Erro ao registrar o Gateway no RMI!");
            e.printStackTrace();
        }
    }

    @Override
    public void connectClient(IntClient client) throws RemoteException {
        System.out.println("Cliente conectado com sucesso: " + client);
    }

    public static void main(String[] args) {
        try {
            new Gateway();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
