package search;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class Barrel extends UnicastRemoteObject implements IBarrelGateway, ReliableMulticastClient {
    private static final long serialVersionUID = 1L;
    private HashMap<String, HashSet<String>> index;
    private HashMap<String, HashSet<String>> linksCorr = new HashMap<>();
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
            Registry registry = LocateRegistry.getRegistry("localhost", 1099); // Substituir pelo IP/porta do GatewayService
            registry.rebind(barrelName, this);
            System.out.println(barrelName + " registrado no RMI.");
        } catch (RemoteException e) {
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
        // System.out.println(barrelName + " Armazenou: " + palavra + " -> " +
        // infoAssociada);

        // System.out.println("[" + barrelName + "] Armazenado: " + palavra + " -> " +
        // infoAssociada);

        // Salvar índice atualizado
        StorageUtil.saveData(index, PAGES_FILE);

    }

    private void registerWithReliableMulticastService() {
        try {
            Registry registry = LocateRegistry.getRegistry("127.0.0.1", 1097);
            ReliableMulticastService multicastService = (ReliableMulticastService) registry
                    .lookup("ReliableMulticastService");

            multicastService.registerClient(this);
            System.out.println(barrelName + " registrado para receber mensagens confiáveis.");

        } catch (RemoteException | NotBoundException e) {
            System.out.println("Erro ao registrar " + barrelName + " no ReliableMulticastService.");
            e.printStackTrace();
        }
    }

    @Override
    public void receiveMessage(String message) throws RemoteException {
        // System.out.println("[" + barrelName + "] Mensagem confiável recebida: " +
        // message);
        if (message.startsWith("flag/")) {
            saveIndex(message);
        } else {
            processReceivedData(message);
        }
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

    private void saveIndex(String mensagem) {
        String[] partes = mensagem.split(" ");
        if (partes.length == 3) {
            String link = partes[1];
            String origem = partes[2];
            if (linksCorr.containsKey(link)) {
                linksCorr.get(link).add(origem);
            } else {
                HashSet<String> links = new HashSet<>();
                links.add(origem);
                linksCorr.put(link, links);
            }
        }
    }

    @Override
    public List<String> search(String word, int page) throws RemoteException {

        ArrayList<String> results = new ArrayList<>(index.getOrDefault(word, new HashSet<>()));

        for (Map.Entry<String, HashSet<String>> entry : linksCorr.entrySet()) {
            System.out.println("Link: " + entry.getKey() + " -> Origem: " + entry.getValue());
        }

        System.out.println("ANTES DO SORT----------------------------------------");
        for (String s : results) {
            System.out.println(s);
        }

        results.sort((a, b) -> {
            // Extrair apenas o URL da string
            String urlA = a.split("URL: ")[1].split(" ")[0]; 
                                                             
            String urlB = b.split("URL: ")[1].split(" ")[0]; 

            int countA = linksCorr.getOrDefault(urlA, new HashSet<>()).size();
            int countB = linksCorr.getOrDefault(urlB, new HashSet<>()).size();
            return Integer.compare(countB, countA);
        });

        System.out.println("DEPOIS DO SORT-----------------------------------------------");
        for (String s : results) {
            System.out.println(s);
        }

        int start = (page - 1) * 10;
        int end = Math.min(start + 10, results.size());
        if (start >= results.size()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(results.subList(start, end));
    }

    @Override
    public List<String> related_links(String link) throws RemoteException {
        ArrayList<String> results = new ArrayList<>(linksCorr.getOrDefault(link, new HashSet<>()));
        return results;
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
