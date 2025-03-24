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
    // private static final String PAGES_FILE = "paginas.obj";

    public Barrel(String name) throws RemoteException {
        super();
        this.barrelName = name;
        String fileName = "paginas_" + barrelName + ".obj";
        this.index = StorageUtil.loadData(fileName, new HashMap<>());
        System.out.println(barrelName + "carregou" + index.size() + "palavras do aquivo");
        registerWithGateway();
        registerWithReliableMulticastService();
    }

    private void registerWithGateway() {
        try {
            // Altere para o IP do PC Windows onde o Gateway está rodando, por exemplo,
            // "192.168.71.1"
            Registry registry = LocateRegistry.getRegistry(1099);
            IClientGateway gateway = (IClientGateway) registry.lookup("GatewayService");
            gateway.registerBarrel(this.barrelName, this);
            System.out.println("Barrel registrado via Gateway.");
        } catch (Exception e) {
            System.err.println("Erro ao registrar o Barrel via Gateway:");
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
        String fileName = "paginas_" + barrelName + ".obj";
        StorageUtil.saveData(index, fileName);

    }

    @Override
    public int getIndexSize() throws RemoteException {
        return index.size();
    }

    private void registerWithReliableMulticastService() {
        try {
            Registry registry = LocateRegistry.getRegistry(1097);
            ReliableMulticastService multicastService = (ReliableMulticastService) registry
                    .lookup("ReliableMulticast");

            multicastService.registerClient(this, this.barrelName);
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

    @Override
    public void ping() throws RemoteException {

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

            int countA = linksCorr.getOrDefault(urlA, new HashSet<>()).size();
            int countB = linksCorr.getOrDefault(urlB, new HashSet<>()).size();
            return Integer.compare(countB, countA);
        });

        int totalPages = (int) Math.ceil(results.size() / 10.0);
        int start = (page - 1) * 10;
        int end = Math.min(start + 10, results.size());

        if (start >= results.size()) {
            return new ArrayList<>();
        }

        
        List<String> pagedResults = new ArrayList<>(results.subList(start, end));
        
        pagedResults.add("tem " + (totalPages) + " paginas");

        return pagedResults;
    }

    @Override
    public List<String> related_links(String link) throws RemoteException {
        ArrayList<String> results = new ArrayList<>(linksCorr.getOrDefault(link, new HashSet<>()));
        return results;
    }

    public static void main(String[] args) {

        if (args.length < 1) {
            System.out.println("Uso: java search.Barrel <nomeDoBarrel>");
            return;
        }
        try {
            new Barrel(args[0]);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}