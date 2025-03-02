package search;

import java.rmi.*;


public interface IntClient extends Remote {
    public String takeNext() throws RemoteException;
    
}
