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
    private String fileName;
    private String fileName1;
    private static ReliableMulticastService multicastService = null;

    public Barrel(String name) throws RemoteException {
        super();
        this.barrelName = name;
        this.fileName = "paginas_" + barrelName + ".obj";
        this.fileName1 = "links_" + barrelName + ".obj";
        this.index = StorageUtil.loadData(fileName, new HashMap<>());
        this.linksCorr = StorageUtil.loadData(fileName1, new HashMap<>());
        System.out.println(barrelName + " carregou " + index.size() + " palavras do arquivo");
        registerWithGateway();
        registerWithReliableMulticastService();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                multicastService.unregisterClient(this.barrelName);
            } catch (Exception e) {
                System.out.println("Nao foi possivel desconectar");
            }
            System.out.println("Detectado encerramento. Salvando dados...");
            StorageUtil.saveData(index, fileName);
            StorageUtil.saveData(linksCorr, fileName1);
            System.out.println("Dados salvos com sucesso.");
        }));
    }
    
    private void registerWithGateway() {
        try {
            Registry registry = LocateRegistry.getRegistry(StorageUtil.getIP(),1099);
            IClientGateway gateway = (IClientGateway) registry.lookup("GatewayService");
            gateway.registerBarrel(this.barrelName, this);
            System.out.println("Barrel registrado via Gateway.");
        } catch (Exception e) {
            System.err.println("Erro ao registrar o Barrel via Gateway:");
        }
    }

    public synchronized void storeDataInIndex(String palavra, String infoAssociada) {
        index.computeIfAbsent(palavra, k -> new HashSet<>()).add(infoAssociada);
    }

    @Override
    public int getIndexSize() throws RemoteException {
        return index.size();
    }

    private void registerWithReliableMulticastService() {
        try {
            Registry registry = LocateRegistry.getRegistry(StorageUtil.getIP(),1097);
            multicastService = (ReliableMulticastService) registry.lookup("ReliableMulticast");
            multicastService.registerClient(this, this.barrelName);
            System.out.println(barrelName + " registrado para receber mensagens confiáveis.");
        } catch (RemoteException | NotBoundException e) {
            System.out.println("Erro ao registrar " + barrelName + " no ReliableMulticastService.");
        }
    }

    @Override
    public void receiveMessage(String message) throws RemoteException {
        if (message.startsWith("flag/")) {
            saveIndex(message);
        } else {
            processReceivedData(message);
        }
    }


    private void processReceivedData(String mensagem) {
        String[] partes = mensagem.split(";", 2);
        if (partes.length == 2) {
            storeDataInIndex(partes[0], partes[1]);
        } else {
            System.out.println(barrelName + " Mensagem recebida não está no formato esperado.");
        }
    }

    private synchronized void saveIndex(String mensagem) {
        String[] partes = mensagem.split(" ");
        if (partes.length == 3) {
            linksCorr.computeIfAbsent(partes[1], k -> new HashSet<>()).add(partes[2]);
        }
    }

    @Override
    public List<String> search(String word, int page) throws RemoteException {
        String[] words = word.split(" ");
        Set<String> resultsSet = null;

        for (String w : words) {
            if (index.containsKey(w)) {
                if (resultsSet == null) {
                    resultsSet = new HashSet<>(index.get(w));
                } else {
                    resultsSet.retainAll(index.get(w));
                }
            } else {
                return new ArrayList<>(); 
            }
        }

        List<String> results = new ArrayList<>(resultsSet);
        results.sort((a, b) -> {
            String urlA = a.split("URL: ")[1].split(" ")[0];
            String urlB = b.split("URL: ")[1].split(" ")[0];
            return Integer.compare(
                linksCorr.getOrDefault(urlB, new HashSet<>()).size(), 
                linksCorr.getOrDefault(urlA, new HashSet<>()).size()
            );
        });

        int totalPages = (int) Math.ceil(results.size() / 10.0);
        int start = (page - 1) * 10;
        int end = Math.min(start + 10, results.size());

        if (start >= results.size()) {
            return new ArrayList<>();
        }

        List<String> pagedResults = new ArrayList<>(results.subList(start, end));
        pagedResults.add("tem " + totalPages + " paginas");
        return pagedResults;
    }

    @Override
    public List<String> related_links(String link) throws RemoteException {
        return new ArrayList<>(linksCorr.getOrDefault(link, new HashSet<>()));
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: java search.Barrel <nomeDoBarrel>");
            return;
        }
        try {
            new Barrel(args[0]);
        } catch (RemoteException e) {
            System.out.println("Erro na criacao do Barrel.");
        }
    }
}
