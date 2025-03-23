package search;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

class ReliableMulticastServiceImpl extends UnicastRemoteObject implements ReliableMulticastService {
    private Map<String, ReliableMulticastClient> clients;
    private final Queue<String> mensagensPendentes;

    protected ReliableMulticastServiceImpl() throws RemoteException {
        super();
        this.mensagensPendentes = new ConcurrentLinkedQueue<>();
        this.clients = new HashMap<>();
        iniciarMonitoramento();
    }

    @Override
    public synchronized void sendReliableMessage(String message) throws RemoteException {
        if (!todosClientesDisponiveis()) {
            System.out.println("Nem todos os clientes estão disponíveis. Armazenando mensagem pendente.->" + "->"+ message);
            mensagensPendentes.add(message);
            return;
        }

        enviarParaTodos(message);
    }

    private boolean todosClientesDisponiveis() {
        for (Map.Entry<String, ReliableMulticastClient> entry : clients.entrySet()) {
            ReliableMulticastClient client = entry.getValue();
            try {
                client.ping();
            } catch (RemoteException e) {
                System.out.println("Cliente " + entry.getKey() + " está offline.");
                return false;
            }
        }
        return true;
    }

    private void enviarParaTodos(String message) {
        for (Map.Entry<String, ReliableMulticastClient> entry : clients.entrySet()) {
            try {
                entry.getValue().receiveMessage(message);
            } catch (RemoteException e) {
                System.out.println("Erro ao enviar mensagem para " + entry.getKey());
            }
        }
    }

    private void reenviarMensagensPendentes() {
        if (!todosClientesDisponiveis()) return;

        while (!mensagensPendentes.isEmpty()) {
            String mensagem = mensagensPendentes.poll();
            enviarParaTodos(mensagem);
        }
    }

    private void iniciarMonitoramento() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000);
                    reenviarMensagensPendentes();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }

    @Override
    public synchronized void registerClient(ReliableMulticastClient client, String name) throws RemoteException {
        if(clients.containsKey(name)){
            System.out.println("JA EXISTE");
        }
        clients.put(name, client);
        System.out.println("Cliente " + name + " registrado para ReliableMulticastService.");
    }
}
