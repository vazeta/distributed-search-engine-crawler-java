package search;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class Barrel extends UnicastRemoteObject implements IBarrelGateway {
    private static final long serialVersionUID = 1L;
    private HashMap<String, HashSet<String>> index;
    private static final String MULTICAST_GROUP = "230.0.0.1";
    private static final int MULTICAST_RECEIVE_PORT = 4447;
    private InetAddress group;
    private NetworkInterface networkInterface;
    private MulticastSocket socket;
    private String barrelName;
    private static final String PAGES_FILE = "paginas.obj";

    public Barrel(String name) throws RemoteException {
        super();
        this.barrelName = name;
        this.index = StorageUtil.loadData(PAGES_FILE, new HashMap<>());
        System.out.println(barrelName + "carregou" + index.size() + "palavras do aquivo");
        RegisterBarrel();

        try {
            // Obtém o grupo multicast
            group = InetAddress.getByName(MULTICAST_GROUP);

            // Cria o socket multicast
            socket = new MulticastSocket(MULTICAST_RECEIVE_PORT);
            socket.setReuseAddress(true); // Permite reuso da porta

            // Obtém a interface de rede
            networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            if (networkInterface == null) {
                System.out.println("Erro: Nenhuma interface de rede encontrada.");
                return;
            }

            // Junta-se ao grupo multicast (versão atualizada)
            SocketAddress groupAddress = new InetSocketAddress(group, MULTICAST_RECEIVE_PORT);
            socket.joinGroup(groupAddress, networkInterface);
            System.out.println(barrelName + " conectado ao grupo multicast " + MULTICAST_GROUP);

            // Inicia a Thread para escutar Multicast
            new Thread(() -> {
                System.out.println(barrelName + " escutando mensagens multicast...");
                listenForMulticast();
            }).start();

            System.out.println(barrelName + " criado com sucesso");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void RegisterBarrel() {
        try {
            Registry registry;

            try {
                registry = LocateRegistry.createRegistry(1100); // Usa o registry já existente
                System.out.println("Novo RMI Registry criado na porta 1100.");
            } catch (RemoteException e) {
                System.out.println("RMI Registry já existente. Conectando...");
                registry = LocateRegistry.getRegistry("127.0.0.1", 1100);
            }

            registry.rebind(barrelName, this);
            System.out.println( barrelName + " registrado no RMI.");
        } catch (RemoteException e) {
            System.out.println("Erro ao registrar " + barrelName + " no RMI.");
            e.printStackTrace();
        }
    }

    public synchronized void storeDataInIndex(String palavra, String infoAssociada) {
        if (index.containsKey(palavra)) {
            index.get(palavra).add(infoAssociada);
        } else {
            HashSet<String> infoSet = new HashSet<>();
            infoSet.add(infoAssociada);
            index.put(palavra, infoSet);
        }
        System.out.println(barrelName + " Armazenou: " + palavra + " -> " + infoAssociada);

    
        System.out.println("[" + barrelName + "] Armazenado: " + palavra + " -> " + infoAssociada);
    
        // Salvar índice atualizado
        StorageUtil.saveData(index, PAGES_FILE);

    }

    @Override
    public void print_index() throws RemoteException {
        System.out.println(" [" + barrelName + "] Índice Atual:");
        for (Map.Entry<String, HashSet<String>> entry : index.entrySet()) {
            System.out.println(" Palavra: " + entry.getKey());
            for (String url : entry.getValue()) {
                System.out.println("   - " + url);
            }
        }
    }

    private void listenForMulticast() {
        try {
            byte[] buffer = new byte[2048];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            while (true) {
                System.out.println( barrelName + " aguardando mensagens multicast...");
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.println( barrelName + " recebeu multicast: " + message);

                processReceivedData(message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processReceivedData(String mensagem) {
        String[] partes = mensagem.split(" ", 4);
        if (partes.length == 4) {
            String palavra = partes[0];
            String url = partes[1];
            String titulo = partes[2];
            String citacao = partes[3];
            String infoAssociada = url + " | Título: " + titulo + " | Citação: " + citacao;
            storeDataInIndex(palavra, infoAssociada);
        } else {
            System.out.println(barrelName + " Mensagem recebida não está no formato esperado.");
        }
    }

    @Override
    public List<String> search(String word, int page) throws RemoteException {
        List<String> results = new ArrayList<>(index.getOrDefault(word, new HashSet<>())) ;
        int start = (page - 1) * 10;
        int end = Math.min(start + 10, results.size());
        if (start >= results.size()) {
            return Collections.emptyList();
        }
        return results.subList(start, end);
    }

    public static void main(String[] args) {
        try {
            new Barrel("Barrel1");
            new Barrel("Barrel2");

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
