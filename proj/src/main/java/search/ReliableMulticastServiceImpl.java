package search;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.Set;

class ReliableMulticastServiceImpl extends UnicastRemoteObject implements ReliableMulticastService {
    private Set<ReliableMulticastClient> clients;

    protected ReliableMulticastServiceImpl() throws RemoteException {
        super();
        this.clients = new HashSet<>();
    }

    @Override
    public synchronized void sendReliableMessage(String message) throws RemoteException {
        //System.out.println("Enviando mensagem confiável: " + message);
        for (ReliableMulticastClient client : clients) {
            try {
                client.receiveMessage(message);
            } catch (RemoteException e) {
                System.out.println("Erro ao enviar mensagem para um cliente.");
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                String stackTrace = sw.toString();

                // Agora você pode imprimir a pilha de erro ou manipulá-la como desejar
                System.out.println(stackTrace);
            }
        }
    }

    @Override
    public synchronized void registerClient(ReliableMulticastClient client) throws RemoteException {
        clients.add(client);
        System.out.println("Cliente registrado para ReliableMulticastService.");
    }
}
