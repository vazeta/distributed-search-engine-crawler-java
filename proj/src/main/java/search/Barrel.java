package search;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class Barrel extends UnicastRemoteObject implements IBarrelGateway, ReliableMulticastClient  {
    private static final long serialVersionUID = 1L;
    private HashMap<String, HashSet<String>> index;
    private String barrelName;
    private static final String PAGES_FILE = "paginas.obj";

    public Barrel(String name) throws RemoteException {
        super();
        this.barrelName = name;
        this.index = StorageUtil.loadData(PAGES_FILE, new HashMap<>());
        System.out.println(barrelName + "carregou" + index.size() + "palavras do aquivo");
        RegisterBarrel();
        registerWithReliableMulticastService();
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

    private void registerWithReliableMulticastService() {
        try {
            Registry registry = LocateRegistry.getRegistry("127.0.0.1", 1097);
            ReliableMulticastService multicastService = (ReliableMulticastService) registry.lookup("ReliableMulticastService");

            multicastService.registerClient(this);
            System.out.println(barrelName + " registrado para receber mensagens confiáveis.");

        } catch (RemoteException | NotBoundException e) {
            System.out.println("Erro ao registrar " + barrelName + " no ReliableMulticastService.");
            e.printStackTrace();
        }
    }

    @Override
    public void receiveMessage(String message) throws RemoteException {
        System.out.println("[" + barrelName + "] Mensagem confiável recebida: " + message);
        processReceivedData(message);
    }


    private void processReceivedData(String mensagem) {
        String[] partes = mensagem.split(";", 2);
        if (partes.length == 2) {
            String palavra = partes[0];
            String info = partes[1];
            storeDataInIndex(palavra, info);
        } else {
            System.out.println(barrelName + " Mensagem recebida não está no formato esperado.");
        }
    }

    @Override
public List<String> search(String word, int page) throws RemoteException {

    HashMap<String, HashSet<String>> linkGraph = StorageUtil.loadData("RelacionamentoLinks.obj", new HashMap<>());
    ArrayList<String> results = new ArrayList<>(index.getOrDefault(word, new HashSet<>()));

    results.sort((a, b) -> Integer.compare(
            linkGraph.getOrDefault(b, new HashSet<>()).size(),
            linkGraph.getOrDefault(a, new HashSet<>()).size()
    ));
    
    int start = (page - 1) * 10;
    int end = Math.min(start + 10, results.size());
    if (start >= results.size()) {
        return new ArrayList<>();
    }
    return new ArrayList<>(results.subList(start, end));
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
