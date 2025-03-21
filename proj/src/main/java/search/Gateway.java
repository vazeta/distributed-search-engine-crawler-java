package search;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Gateway extends UnicastRemoteObject implements IClientGateway {

    // Contador para os termos pesquisados
    private Map<String, Integer> searchCounts = new HashMap<>();
    
    // Campos para o cálculo do tempo médio (cumulativo)
    private long totalResponseTime = 0;
    private int totalResponseCount = 0;

    public Gateway() throws RemoteException {
        super();
        gatewayReg(); 
    }

    private void gatewayReg() {
        try {
            Registry registry;
            try {
                registry = LocateRegistry.createRegistry(1099);
                System.out.println("Novo RMI Registry criado na porta 1099.");
            } catch (RemoteException e) {
                System.out.println("RMI Registry já existente. Conectando...");
                registry = LocateRegistry.getRegistry(1099);
            }
            registry.rebind("GatewayService", this);
            System.out.println("Gateway RMI registrado com sucesso.");
        } catch (RemoteException e) {
            System.out.println("Erro ao registrar o Gateway no RMI!");
            e.printStackTrace();
        }
    }

    @Override
    public void connectClient(IntClient client) throws RemoteException {
        System.out.println("Cliente conectado com sucesso: " + client);
    }

    @Override
    public void addUrlToQueue(String url) throws RemoteException {
        try {
            Registry registry = LocateRegistry.getRegistry(1099);
            URLQueue queue = (URLQueue) registry.lookup("URLQueue");
            queue.addURL(url);
            System.out.println("Gateway: URL " + url + " enviada para a fila");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<String> request_url_related(String link) throws RemoteException {
        Registry registry;
        String[] barrels;
        try {
            registry = LocateRegistry.getRegistry(1099);
        } catch (Exception e) {
            throw new RemoteException("Erro ao conectar ao RMI Registry!", e);
        }
        while (true) {
            try {
                barrels = registry.list();
                if (barrels.length == 0) {
                    System.out.println("Nenhum Barrel disponível. Tentando novamente em 2 segundos...");
                    Thread.sleep(2000);
                }
                List<String> listaBarrels = new ArrayList<>(Arrays.asList(barrels));
                List<String> filteredBarrels = new ArrayList<>();
                for (String barrel : listaBarrels) {
                    if (barrel.startsWith("Barrel")) {
                        filteredBarrels.add(barrel);
                    }
                }
                listaBarrels = filteredBarrels;
                
                for (String selectedBarrel : listaBarrels) {
                    try {
                        System.out.println("A tentar conectar ao Barrel: " + selectedBarrel);
                        IBarrelGateway barrel = (IBarrelGateway) registry.lookup(selectedBarrel);
                        System.out.println("Consegui");
                        List<String> results = barrel.related_links(link);
                        return results;
                    } catch (Exception e) {
                        System.out.println("Erro ao conectar ao Barrel " + selectedBarrel + ". Tentando outro...");
                        e.printStackTrace();
                    }
                }
                System.out.println("Todos os Barrels falharam. Tentando novamente em 2 segundos...");
                Thread.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RemoteException("Erro ao buscar palavra.", e);
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
        String[] barrels;
        List<String> results = null;
        try {
            registry = LocateRegistry.getRegistry(1099);
        } catch (Exception e) {
            throw new RemoteException("Erro ao conectar ao RMI Registry!", e);
        }
        
        // Inicia a medição do tempo de resposta
        long startTime = System.currentTimeMillis();
        
        while (true) {
            try {
                barrels = registry.list();
                if (barrels.length == 0) {
                    System.out.println("Nenhum Barrel disponível. Tentando novamente em 2 segundos...");
                    Thread.sleep(2000);
                }
                List<String> listaBarrels = new ArrayList<>(Arrays.asList(barrels));
                List<String> filteredBarrels = new ArrayList<>();
                for (String barrel : listaBarrels) {
                    if (barrel.startsWith("Barrel")) {
                        filteredBarrels.add(barrel);
                    }
                }
                listaBarrels = filteredBarrels;

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
                e.printStackTrace();
                throw new RemoteException("Erro ao buscar palavra.", e);
            }
        }
        
        // Calcula o tempo de resposta desta pesquisa
        long responseTime = System.currentTimeMillis() - startTime;
        
        // Atualiza os campos cumulativos para o tempo médio
        if (page == 1) {  // Só contabiliza a primeira página para evitar duplicações
            totalResponseTime += responseTime;
            totalResponseCount++;
        }
        long avgResponseTime = (totalResponseCount > 0) ? totalResponseTime / totalResponseCount : 0;

        // Consolida as estatísticas: total de pesquisas, número de Barrels ativos e tamanhos dos índices
        int totalSearches = searchCounts.values().stream().mapToInt(Integer::intValue).sum();
        int activeBarrels = 0;
        Map<String, Integer> barrelIndexSizes = new HashMap<>();
        try {
            String[] allNames = registry.list();
            for (String name : allNames) {
                if (name.startsWith("Barrel")) {
                    activeBarrels++;
                    try {
                        IBarrelGateway barrel = (IBarrelGateway) registry.lookup(name);
                        int size = barrel.getIndexSize();
                        barrelIndexSizes.put(name, size);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<String> top10 = getTop10Searches();
        Statistics stats = new Statistics(totalSearches, activeBarrels, avgResponseTime);
        stats.setTop10Searches(top10);
        stats.setBarrelIndexSizes(barrelIndexSizes);

        // Notifica os clientes inscritos no serviço de estatísticas
        try {
            StatisticsService statsService = (StatisticsService) registry.lookup("StatisticsService");
            statsService.notifyStats(stats);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }

    private synchronized void updateSearchCount(String term) {
        int count = searchCounts.getOrDefault(term, 0) + 1;
        searchCounts.put(term, count);
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
            // Registra o serviço de estatísticas utilizando a implementação adequada
            StatisticsServiceImpl statsService = new StatisticsServiceImpl();
            Registry registry = LocateRegistry.getRegistry(1099);
            registry.rebind("StatisticsService", statsService);
            System.out.println("Serviço de Estatísticas registrado com sucesso.");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
