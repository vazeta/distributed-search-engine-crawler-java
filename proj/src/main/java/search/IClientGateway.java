package search;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IClientGateway extends Remote {
    void connectClient(IntClient client) throws RemoteException;
    void addUrlToQueue(String url) throws RemoteException; 
    List<String> request_index(String word, int page) throws RemoteException;
    List<String> request_url_related(String link) throws RemoteException;
    void registerBarrel(String barrelName, IBarrelGateway barrelStub) throws RemoteException;
}
