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

    public Barrel() throws RemoteException {
        super();
        index = new HashMap<>();
        try {
            group = InetAddress.getByName(MULTICAST_GROUP);
            socket = new MulticastSocket(MULTICAST_RECEIVE_PORT);
            networkInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            SocketAddress groupAddress = new InetSocketAddress(group, MULTICAST_RECEIVE_PORT);
            socket.joinGroup(groupAddress, networkInterface);
            new Thread(this::listenForMulticast).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void RegisterBarrel(String barrelName) {
        try {
            Registry registry = LocateRegistry.getRegistry(1100); // Usa o registry já existente
            registry.rebind(barrelName, this);  // Registra o objeto Barrel com nome único
            System.out.println(barrelName + " registrado com sucesso");
        } catch (RemoteException e) {
            System.out.println("Erro ao registrar " + barrelName + " no rmi");
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
    }

    @Override
    public void print_index() throws RemoteException {
        for (Map.Entry<String, HashSet<String>> entry : index.entrySet()) {
            String palavra = entry.getKey();
            HashSet<String> urls = entry.getValue();
            System.out.println("Palavra: " + palavra);
            System.out.println("URLs associadas:");
            for (String url : urls) {
                System.out.println(" - " + url);
            }
        }
    }

    private void listenForMulticast() {
        try {
            byte[] buffer = new byte[256];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
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
            System.out.println("Mensagem recebida não está no formato esperado");
        }
    }

    @Override
    public List<String> search(String word) throws RemoteException {
        HashSet<String> urls = index.getOrDefault(word, new HashSet<>());
        return new ArrayList<>(urls);
    }

    public static void main(String[] args) {
        try {
            Barrel barrel1 = new Barrel();
            barrel1.RegisterBarrel("Barrel1");  // Registra com nome "Barrel1"
            
            Barrel barrel2 = new Barrel();
            barrel2.RegisterBarrel("Barrel2");  // Registra com nome "Barrel2"
            
            System.out.println("Dois Barrels criados!");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
