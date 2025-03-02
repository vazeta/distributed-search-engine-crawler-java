package search;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IClientGateway extends Remote {
    void connectClient(IntClient client) throws RemoteException;
    void addUrlToQueue(String url) throws RemoteException; 
    List<String> searchWord(String word) throws RemoteException;
}
