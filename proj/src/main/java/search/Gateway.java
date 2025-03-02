package search;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

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

    @Override
    public void addUrlToQueue(String url) throws RemoteException{
        try{
            Registry registry = LocateRegistry.getRegistry(1098);
            URLQueue queue = (URLQueue) registry.lookup("URLQueue");
            queue.addURL(url);
            System.out.println("Gateway: URL " + url +" enviado para a fila");
        }catch(Exception e){
            e.printStackTrace();
        }

    }
    
    @Override
    public List<String> request_index(String word) throws RemoteException {
        try {
            Registry registry = LocateRegistry.getRegistry(1100);
            IBarrelGateway barrel = (IBarrelGateway) registry.lookup("Barrel");
            return barrel.search(word);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Erro ao buscar palavra.", e);
        }
    }
    
    public static void main(String[] args) {
        try {
            new Gateway();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
