package search;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface StatisticsClient extends Remote{
        void updateStats(Statistics Stats) throws RemoteException;

}
