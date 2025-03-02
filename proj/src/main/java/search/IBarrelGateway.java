package search;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IBarrelGateway  extends Remote{
    void storeData(String palavra, String url) throws RemoteException;
    List<String> search(String word) throws RemoteException;
    void print_index() throws RemoteException;
}
