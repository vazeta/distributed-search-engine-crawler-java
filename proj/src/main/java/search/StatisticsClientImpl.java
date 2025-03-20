package search;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class StatisticsClientImpl extends UnicastRemoteObject implements StatisticsClient {

    // Armazena a última ordem do Top 10 (apenas os nomes das palavras)
    private List<String> lastTop10Order = new ArrayList<>();
    private long lastAvgResponseTime = -1;

    public StatisticsClientImpl() throws RemoteException {
        super();
    }

    @Override
    public void updateStats(Statistics stats) throws RemoteException {
        List<String> newTop10 = stats.getTop10Searches();
      
        List<String> newTop10Order = new ArrayList<>();
        for (String s : newTop10) {
            int idx = s.indexOf(" (");
            if (idx != -1) {
                newTop10Order.add(s.substring(0, idx));
            } else {
                newTop10Order.add(s);
            }
        }

        boolean top10OrderChanged = !newTop10Order.equals(lastTop10Order);
        long newAvgResponseTime = stats.getAverageResponseTime();
        boolean avgResponseTimeChanged = (newAvgResponseTime != lastAvgResponseTime);

        if (top10OrderChanged) {
            System.out.println("Top 10 atualizado:");
            for (String term : newTop10) {
                System.out.println(" - " + term);
            }
            System.out.println("----------------------------------------------------");
            lastTop10Order = new ArrayList<>(newTop10Order);
        }

        if (avgResponseTimeChanged) {
            System.out.println("Tempo médio de resposta atualizado: " + newAvgResponseTime + " ms");
            System.out.println("----------------------------------------------------");
            lastAvgResponseTime = newAvgResponseTime;
        }

        if (!top10OrderChanged && !avgResponseTimeChanged) {
        }
    }
}
