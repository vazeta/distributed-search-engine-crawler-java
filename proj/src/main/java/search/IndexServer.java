package search;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.concurrent.*;
import java.io.*;
import java.util.*;

public class IndexServer extends UnicastRemoteObject implements Index {
    Dictionary<String, ArrayList<String>> indexedItems = new Hashtable<String, ArrayList<String>>();
    HashMap<String, HashSet<String>> index = new HashMap<String, HashSet<String>>();
    Stack<String> pilha = new Stack<>();    
    public IndexServer() throws RemoteException {
        super();
        //This structure has a number of problems. The first is that it is fixed size. Can you enumerate the others?            
    }

    public static void main(String args[]) {
        try {
            IndexServer server = new IndexServer();
            Registry registry = LocateRegistry.createRegistry(8183);
            registry.rebind("index", server);
            System.out.println("Server ready. Waiting for input...");
            // Scanner sc = new Scanner(System.in);
            // String url_ins = sc.nextLine();
            // server.putNew(url_ins);
            
            server.putNew("https://pt.wikipedia.org/wiki/Wikip%C3%A9dia:P%C3%A1gina_principal");

            //sc.close();
            //server.putNew(sc.nextLine());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private long counter = 0, timestamp = System.currentTimeMillis();;

    public String takeNext() throws RemoteException {
        //TODO: not implemented fully. Prefer structures that return in a push/pop fashion
        return pilha.pop();
    }

    public void putNew(String url) throws java.rmi.RemoteException {
        //TODO: Example code. Must be changed to use structures that have primitives such as .add(...)
        pilha.add(url);

    }

    public void addToIndex(String word, String url) throws java.rmi.RemoteException {
        //TODO: not implemented
        //USAR O HASHSET !!!!!!!
        if(indexedItems.get(word) == null){
            indexedItems.put(word, new ArrayList<String>());
            indexedItems.get(word).add(url);
        }else{
            indexedItems.get(word).add(url);
        }
    }

    
    public List<String> searchWord(String word) throws java.rmi.RemoteException {
        //TODO: not implemented
        List<String> urls = new ArrayList<>();
        //USAR O HASHSET !!!!!!!!
        urls=(indexedItems.get(word));
        return urls;
    }
}
