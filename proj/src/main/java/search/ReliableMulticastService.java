package search;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface ReliableMulticastService extends Remote {
    void sendReliableMessage(String message) throws RemoteException;
    void registerClient(ReliableMulticastClient client) throws RemoteException;
}