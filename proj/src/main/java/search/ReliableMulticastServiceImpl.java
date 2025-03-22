package search;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

class ReliableMulticastServiceImpl extends UnicastRemoteObject implements ReliableMulticastService {
    private Map<String, ReliableMulticastClient> clients;

    protected ReliableMulticastServiceImpl() throws RemoteException {
        super();
        this.clients = new HashMap<>();
    }

    @Override
    public synchronized void sendReliableMessage(String message) throws RemoteException {
        int attempt = 0;
        boolean allAvailable = false;
        
        while (!allAvailable) {
            attempt++;
            System.out.println("Tentativa " + attempt + " de verificar a disponibilidade dos clientes.");
            
            boolean hasUnavailableClients = false;
            
            for (Map.Entry<String, ReliableMulticastClient> entry : clients.entrySet()) {
                String clientName = entry.getKey();
                ReliableMulticastClient client = entry.getValue();
                try {
                    client.ping(); // Método fictício para testar conectividade
                } catch (RemoteException e) {
                    hasUnavailableClients = true;
                    System.out.println("Cliente " + clientName + " inacessível, tentando novamente.");
                }
            }
            
            if (!hasUnavailableClients) {
                allAvailable = true;
            } else {
                try {
                    Thread.sleep(1000); // Espera antes de tentar novamente
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RemoteException("Thread interrompida durante o timeout", e);
                }
            }
        }
        
        // Agora que todos estão disponíveis, enviamos a mensagem
        for (ReliableMulticastClient client : clients.values()) {
            try {
                client.receiveMessage(message);
            } catch (RemoteException e) {
                throw new RemoteException("Falha ao enviar a mensagem após todos os clientes estarem disponíveis", e);
            }
        }
    }

    @Override
    public synchronized void registerClient(ReliableMulticastClient client, String name) throws RemoteException {
        clients.put(name, client);
        System.out.println("Cliente " + name + " registrado para ReliableMulticastService.");
    }
}
