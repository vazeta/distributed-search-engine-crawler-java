package search;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Gateway extends UnicastRemoteObject implements IClientGateway {

    private Map<String, Integer> searchCounts = new HashMap<>();
    private ReliableMulticastService multicastService;

    private long totalResponseTime = 0;
    private int totalResponseCount = 0;

    public Gateway() throws RemoteException {
        super();
        multicastService = new ReliableMulticastServiceImpl();
        gatewayReg();
        loadSearchCounts();
    }

    private static final String SEARCH_DATA_FILE = "..//data//procuras.obj";

    private void loadSearchCounts() {
        File file = new File(SEARCH_DATA_FILE);
        if (!file.exists())
            return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Object obj = ois.readObject();
            if (obj instanceof Map) {
                Map<?, ?> tempMap = (Map<?, ?>) obj;
                searchCounts = new HashMap<>();

                for (Map.Entry<?, ?> entry : tempMap.entrySet()) {
                    if (entry.getKey() instanceof String && entry.getValue() instanceof Integer) {
                        searchCounts.put((String) entry.getKey(), (Integer) entry.getValue());
                    }
                }
                System.out.println("Dados de pesquisa carregados com sucesso.");
            } else {
                System.err.println("Formato de dados inválido no arquivo.");
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erro ao carregar os dados de pesquisa.");
        }
    }

    private void saveSearchCounts() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SEARCH_DATA_FILE))) {
            oos.writeObject(searchCounts);
        } catch (IOException e) {
            System.err.println("Erro ao salvar os dados de pesquisa.");
        }
    }

    private void gatewayReg() {
        try {
            Registry registry;
            Registry registry1;

            try {
                registry = LocateRegistry.createRegistry(1099);
                System.out.println("Novo RMI Registry criado na porta 1099.");
            } catch (RemoteException e) {
                System.out.println("RMI Registry já existente na porta 1099. Conectando...");
                registry = LocateRegistry.getRegistry(StorageUtil.getIP(),1099);
            }

            try {
                registry1 = LocateRegistry.createRegistry(1097);
                System.out.println("Novo RMI Registry criado na porta 1097.");
            } catch (RemoteException e) {
                System.out.println("RMI Registry já existente na porta 1097. Conectando...");
                registry1 = LocateRegistry.getRegistry(StorageUtil.getIP(),1097);
            }

            registry.rebind("GatewayService", this);
            registry1.rebind("ReliableMulticast", multicastService);
            System.out.println("Gateway RMI registrado com sucesso.");
        } catch (RemoteException e) {
            System.out.println("Erro ao registrar o Gateway no RMI!");
        }
    }

    @Override
    public void connectClient(IntClient client) throws RemoteException {
        System.out.println("Cliente conectado com sucesso: " + client);
    }

    @Override
    public void addUrlToQueue(String url) throws RemoteException {
        try {
            Registry registry = LocateRegistry.getRegistry(StorageUtil.getIP(),1099);
            URLQueue queue = (URLQueue) registry.lookup("URLQueue");
            queue.addURL(url);
            System.out.println("Gateway: URL " + url + " enviada para a fila");
        } catch (Exception e) {
            System.out.println("Erro ao encontrar a QUEUE.");
        }
    }

    public void registerBarrel(String barrelName, IBarrelGateway barrelStub) throws RemoteException {
        try {
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(barrelName, barrelStub);
            System.out.println("Barrel " + barrelName + " registrado via Gateway.");
        } catch (RemoteException e) {
            System.err.println("Erro ao registrar o Barrel " + barrelName);
        }
    }

    @Override
    public List<String> request_url_related(String link) throws RemoteException {
        Registry registry;
        try {
            registry = LocateRegistry.getRegistry(StorageUtil.getIP(),1099);
        } catch (Exception e) {
            throw new RemoteException("Erro ao conectar ao RMI Registry!", e);
        }
        while (true) {
            try {
                Map<String, ReliableMulticastClient> activeClients = multicastService.getActive();
                List<String> listaBarrels = new ArrayList<>(activeClients.keySet());

                for (String selectedBarrel : listaBarrels) {
                    try {
                        System.out.println("A tentar conectar ao Barrel: " + selectedBarrel);
                        IBarrelGateway barrel = (IBarrelGateway) registry.lookup(selectedBarrel);
                        System.out.println("Consegui");
                        List<String> results = barrel.related_links(link);
                        return results;
                    } catch (Exception e) {
                        System.out.println("Erro ao conectar ao Barrel " + selectedBarrel + ". Tentando outro...");
                    }
                }
                System.out.println("Todos os Barrels falharam. Tentando novamente em 2 segundos...");
                Thread.sleep(2000);
            } catch (Exception e) {
                System.out.println("Erro ao buscar palavra.");
            }
        }
    }

    @Override
    public List<String> request_index(String word, int page) throws RemoteException {

        if (page == 1) {
            String[] terms = word.split("\\s+");
            for (String term : terms) {
                updateSearchCount(term);
            }
        }

        Registry registry;
        List<String> results = null;
        try {
            registry = LocateRegistry.getRegistry(StorageUtil.getIP(),1099);
        } catch (Exception e) {
            throw new RemoteException("Erro ao conectar ao RMI Registry!", e);
        }

        long startTime = System.currentTimeMillis();

        while (true) {
            try {
                Map<String, ReliableMulticastClient> activeClients = multicastService.getActive();
                List<String> listaBarrels = new ArrayList<>(activeClients.keySet());
                Collections.shuffle(listaBarrels, new Random());
                for (String selectedBarrel : listaBarrels) {
                    try {
                        System.out.println("A tentar conectar ao Barrel: " + selectedBarrel);
                        IBarrelGateway barrel = (IBarrelGateway) registry.lookup(selectedBarrel);
                        System.out.println("Consegui");
                        results = barrel.search(word, page);
                        break;
                    } catch (Exception e) {
                        System.out.println("Erro ao conectar ao Barrel " + selectedBarrel + ". Tentando outro...");
                    }
                }
                if (results != null) {
                    break;
                }
                System.out.println("Todos os Barrels falharam. Tentando novamente em 2 segundos...");
                Thread.sleep(2000);
            } catch (Exception e) {
                System.out.println("Erro ao buscar palavra.");
            }
        }

        long responseTime = (System.currentTimeMillis() - startTime);

        if (page == 1) {
            totalResponseTime += responseTime;
            totalResponseCount++;
        }
        long avgResponseTime = (totalResponseCount > 0) ? totalResponseTime / totalResponseCount : 0;
        int totalSearches = searchCounts.values().stream().mapToInt(Integer::intValue).sum();
        Map<String, Integer> barrelIndexSizes = new HashMap<>();
        try {
            Map<String, ReliableMulticastClient> activeClients = multicastService.getActive();
            List<String> listaBarrels = new ArrayList<>(activeClients.keySet());
            for (String Selectedbarrel : listaBarrels) {
                try {
                    IBarrelGateway barrel = (IBarrelGateway) registry.lookup(Selectedbarrel);
                    int size = barrel.getIndexSize();
                    barrelIndexSizes.put(Selectedbarrel, size);
                } catch (Exception ex) {
                    System.out.println("Erro ao conectar ao Barrel " + Selectedbarrel + ". Tentando outro...");
                }
            }
        } catch (Exception e) {
            System.out.println("Erro no aceso aos barrels.");
        }
        List<String> top10 = getTop10Searches();
        Statistics stats = new Statistics(totalSearches, barrelIndexSizes.size(), avgResponseTime);
        stats.setTop10Searches(top10);
        stats.setBarrelIndexSizes(barrelIndexSizes);

        try {
            StatisticsService statsService = (StatisticsService) registry.lookup("StatisticsService");
            statsService.notifyStats(stats);
        } catch (Exception e) {
            System.out.println("Erro no acesso e notificacao de novas estatisticas.");
        }

        return results;
    }

    private synchronized void updateSearchCount(String term) {
        int count = searchCounts.getOrDefault(term, 0) + 1;
        searchCounts.put(term, count);
        saveSearchCounts();
    }

    private synchronized List<String> getTop10Searches() {
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(searchCounts.entrySet());
        entries.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        List<String> top10 = new ArrayList<>();
        int limit = Math.min(10, entries.size());
        for (int i = 0; i < limit; i++) {
            Map.Entry<String, Integer> entry = entries.get(i);
            top10.add(entry.getKey() + " (" + entry.getValue() + ")");
        }
        return top10;
    }

    public static void main(String[] args) {
        try {
            new Gateway();
            StatisticsServiceImpl statsService = new StatisticsServiceImpl();
            Registry registry = LocateRegistry.getRegistry(1099);
            registry.rebind("StatisticsService", statsService);
            System.out.println("Serviço de Estatísticas registrado com sucesso.");
        } catch (RemoteException e) {
            System.out.println("Erro na criacao de gateway.");
        }
    }
}
