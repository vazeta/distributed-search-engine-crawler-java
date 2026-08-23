package search;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;


public interface ReliableMulticastService extends Remote {
    void sendReliableMessage(String message) throws RemoteException;
    void registerClient(ReliableMulticastClient client, String name) throws RemoteException;
    void unregisterClient(String name) throws RemoteException;
    Map<String,ReliableMulticastClient> getActive() throws RemoteException;
}