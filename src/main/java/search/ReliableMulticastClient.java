package search;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ReliableMulticastClient extends Remote {
    void receiveMessage(String message) throws RemoteException;
}
