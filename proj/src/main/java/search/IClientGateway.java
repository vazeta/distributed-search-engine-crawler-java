package search;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IClientGateway extends Remote {
    void connectClient(IntClient client) throws RemoteException;
}
