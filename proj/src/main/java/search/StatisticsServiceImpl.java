package search;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Arrays;

public class StatisticsServiceImpl extends UnicastRemoteObject implements StatisticsService {
    private List<StatisticsClient> subscribers;
    private Statistics currentStats;

    public StatisticsServiceImpl() throws RemoteException {
        super();
        subscribers = new ArrayList<>();
    }

    @Override
    public void subscribeStatistics(StatisticsClient client) throws RemoteException {
        subscribers.add(client);
        System.out.println("Cliente inscrito para receber estatisticas");
    }

    public void notifyStats(Statistics stats) {
        currentStats = stats;
        Iterator<StatisticsClient> it = subscribers.iterator();
        while (it.hasNext()) {
            try {
                it.next().updateStats(stats);
            } catch (RemoteException e) {
                it.remove();
                System.out.println("Removido cliente inacessível.");
            }
        }
    }

    @Override
    public synchronized Statistics getStats() throws RemoteException {
        try {
            Registry registry = LocateRegistry.getRegistry(StorageUtil.getIP(), 1099);
            String[] names = registry.list();
            Map<String, Integer> barrelIndexSizes = new HashMap<>();
            for (String name : names) {
                if (name.startsWith("Barrel")) {
                    try {
                        IBarrelGateway barrel = (IBarrelGateway) registry.lookup(name);
                        barrelIndexSizes.put(name, barrel.getIndexSize());
                    } catch (Exception e) {
                        System.err.println("Erro ao obter índice de " + name + ": " + e.getMessage());
                    }
                }
            }
            
            if (currentStats == null) {
                currentStats = new Statistics(0, barrelIndexSizes.size(), 0);
                // Carrega os top 10 persistidos ou inicializa com dados padrão se não houver
                List<String> persistedTop10 = StorageUtil.loadData("top10.obj", new ArrayList<>());
                if (persistedTop10.isEmpty()) {
                    persistedTop10 = Arrays.asList("Nenhuma pesquisa realizada");
                }
                currentStats.setTop10Searches(persistedTop10);
            }
            
            currentStats.setActiveBarrels(barrelIndexSizes.size());
            currentStats.setBarrelIndexSizes(barrelIndexSizes);
            
            // Opcional: salve os dados atuais para persistência
            StorageUtil.saveData(currentStats.getTop10Searches(), "top10.obj");
            
            return currentStats;
        } catch (Exception e) {
            throw new RemoteException("Erro ao obter as estatísticas", e);
        }
    }

}
