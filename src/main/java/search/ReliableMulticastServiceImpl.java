package search;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

class ReliableMulticastServiceImpl extends UnicastRemoteObject implements ReliableMulticastService {
    private Map<String, ReliableMulticastClient> clients;
    private Map<String, ReliableMulticastClient> Ativosclients;

    protected ReliableMulticastServiceImpl() throws RemoteException {
        super();
        this.clients = new HashMap<>();
        this.Ativosclients = new HashMap<>();
    }

    @Override
    public void sendReliableMessage(String message) throws RemoteException {
        while (Ativosclients.size() != clients.size()) {
            try {
                System.out.println("A tentar....");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RemoteException("Thread interrompida durante o timeout", e);
            }
        }

        for (ReliableMulticastClient client : clients.values()) {
            try {
                client.receiveMessage(message);
            } catch (RemoteException e) {
                System.out.println("Um dos barrels foi parado!!!!");
            }
        }
    }

    @Override
    public synchronized void registerClient(ReliableMulticastClient client, String name) throws RemoteException {
        if (clients.containsKey(name)) {
            System.out.println("JA EXISTE");
        }
        clients.put(name, client);
        if (Ativosclients.containsKey(name)) {
            System.out.println("JA EXISTE");
        }
        Ativosclients.put(name, client);
        System.out.println("Cliente " + name + " registrado para ReliableMulticastService.");
    }

    @Override
    public synchronized void unregisterClient(String name) throws RemoteException {
        if (Ativosclients.containsKey(name)) {
            Ativosclients.remove(name);
            System.out.println("Cliente " + name + " desregistrado do ReliableMulticastService.");
        } else {
            System.out.println("Cliente " + name + " não encontrado para desregistro.");
        }
    }

    @Override
    public synchronized Map<String,ReliableMulticastClient> getActive() throws RemoteException {
        return Ativosclients;
    }

}
