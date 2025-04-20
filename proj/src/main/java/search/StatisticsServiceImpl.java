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

import com.example.googol.controllers.StatisticsWebSocketController;

import java.util.Arrays;


public class StatisticsServiceImpl extends UnicastRemoteObject implements StatisticsService {
    private List<StatisticsClient> subscribers;
    private Statistics currentStats;
    private List<String> lastTop10Order = new ArrayList<>();
    private long lastAvgResponseTime = -1;
    private Map<String, Integer> barrelIndexSizes = new HashMap<>();
    private transient StatisticsWebSocketController wsController;

    public void setWebSocketController(StatisticsWebSocketController controller) {
        this.wsController = controller;
    }

    public StatisticsServiceImpl() throws RemoteException {
        super();
        subscribers = new ArrayList<>();
    }

    @Override
    public void notifyIndexUpdate(String barrelName, int indexSize) throws RemoteException {
        boolean updated = false;

        synchronized (this) {
            if (currentStats == null) {
                currentStats = new Statistics(0, 1, 0);
                currentStats.setTop10Searches(new ArrayList<>());
                currentStats.setBarrelIndexSizes(new HashMap<>());
            }

            // Update the barrel index size
            Map<String, Integer> sizes = currentStats.getBarrelIndexSizes();
            Integer oldSize = sizes.get(barrelName);
            if (oldSize == null || oldSize != indexSize) {
                sizes.put(barrelName, indexSize);
                currentStats.setBarrelIndexSizes(sizes);
                updated = true;
            }
            currentStats.setActiveBarrels(sizes.size());
        }

        // Only send updates if something changed
        if (updated && wsController != null) {
            wsController.sendStats(currentStats);
        }
    }

    @Override
    public void subscribeStatistics(StatisticsClient client) throws RemoteException {
        subscribers.add(client);
        System.out.println("Cliente inscrito para receber estatisticas");
    }

    public void notifyStats(Statistics stats) {
        System.out.println("opaaaaa");
        if (!statsChanged(stats)) {
            System.out.println("nada");
            return;
        }

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

        if (wsController != null) {
            System.out.println("a mandar ");
            wsController.sendStats(stats);
        } else {
            System.out.println("esta a null");
        }
    }

    private boolean statsChanged(Statistics newStats) {
        List<String> newTop10Order = new ArrayList<>();
        for (String s : newStats.getTop10Searches()) {
            int idx = s.indexOf(" (");
            if (idx != -1) {
                newTop10Order.add(s.substring(0, idx));
            } else {
                newTop10Order.add(s);
            }
        }

        boolean top10Changed = !newTop10Order.equals(lastTop10Order);
        boolean avgRespTimeChanged = newStats.getAverageResponseTime() != lastAvgResponseTime;
        boolean barrelIndexSizesChanged = !newStats.getBarrelIndexSizes().equals(barrelIndexSizes);
        if (barrelIndexSizesChanged) {
            barrelIndexSizes = newStats.getBarrelIndexSizes();
        }

        if (top10Changed) {
            lastTop10Order = new ArrayList<>(newTop10Order);
        }
        if (avgRespTimeChanged) {
            lastAvgResponseTime = newStats.getAverageResponseTime();
        }
        return top10Changed || avgRespTimeChanged;
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
