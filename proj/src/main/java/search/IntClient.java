package search;

import java.rmi.*;
import java.util.*;

public interface IntClient extends Remote {
    public String takeNext() throws RemoteException;
    public void putNew(String url) throws java.rmi.RemoteException;
    public void addToIndex(String word, String url) throws java.rmi.RemoteException;
    public List<String> searchWord(String word) throws java.rmi.RemoteException;
}
