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


public class StatisticsServiceImpl extends UnicastRemoteObject implements StatisticsService{
    private List<StatisticsClient> subscribers;
    private Statistics currentStats;
    public StatisticsServiceImpl() throws RemoteException{
        super();
        subscribers = new ArrayList<>();
    }
    @Override
    public void subscribeStatistics(StatisticsClient client) throws RemoteException{
        subscribers.add(client);
        System.out.println("Cliente inscrito para receber estatisticas");
    }

    public void notifyStats(Statistics stats){
        currentStats = stats;
        Iterator<StatisticsClient> iterator = subscribers.iterator();
        while (iterator.hasNext()) {
            try {
                StatisticsClient client = iterator.next();
                client.updateStats(stats);
            } catch (RemoteException e) {
                iterator.remove();
                System.out.println("Removido cliente inacessível.");
            }
        }
    }
    @Override
    public Statistics getStats() throws RemoteException {
        if (currentStats != null) {
            return currentStats;
        } else {
            try {
                Registry registry = LocateRegistry.getRegistry(1099);
                String[] allNames = registry.list();
                int activeBarrels = 0;
                Map<String, Integer> barrelIndexSizes = new HashMap<>();
                for (String name : allNames) {
                    if (name.startsWith("Barrel")) {
                        try {
                            IBarrelGateway barrel = (IBarrelGateway) registry.lookup(name);
                            int size = barrel.getIndexSize();
                            barrelIndexSizes.put(name, size);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                
                Statistics stats = new Statistics(0, barrelIndexSizes.size(), 0);
                stats.setTop10Searches(new ArrayList<>());
                stats.setBarrelIndexSizes(barrelIndexSizes);
                return stats;
            } catch (Exception e) {
                throw new RemoteException("Erro ao obter as estatísticas", e);
            }
        }
    }

}

