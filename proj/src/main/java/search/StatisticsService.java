package search;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface StatisticsService extends Remote {

    void subscribeStatistics(StatisticsClient client) throws RemoteException;
    void notifyIndexUpdate(String barrelName, int indexSize) throws RemoteException;
    void notifyStats(Statistics stats) throws RemoteException;
    Statistics getStats() throws RemoteException;
}
