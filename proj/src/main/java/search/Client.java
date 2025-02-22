package search;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.concurrent.*;
import java.io.*;
import java.util.*;

public class Client extends UnicastRemoteObject implements IntClient {
    private IClientGateway gateway;

    HashMap<String, HashSet<String>> index = new HashMap<String, HashSet<String>>();

    Stack<String> pilha = new Stack<>();

    public Client() throws RemoteException {
        super();
        gatewayconnect();

        try {
            
        } catch (RemoteException e) {
            // TODO: handle exception
            System.out.println("Error in remote connection");
        }
        //This structure has a number of problems. The first is that it is fixed size. Can you enumerate the others?            
    }

    public void gatewayconnect() throws RemoteException {
        try {
            // Assuming the Gateway is registered with the name "GatewayService" in the RMI Registry
            Registry registry = LocateRegistry.getRegistry("localhost"); // Connect to the RMI registry running on localhost
            gateway = (IClientGateway) registry.lookup("GatewayService"); // Lookup the service
            System.out.println("Connected to Gateway");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RemoteException("Unable to connect to the Gateway", e);
        }
    }
    

    

    public static void main(String args[]) {
        try {
            Client server = new Client();
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
        if (!index.containsKey(word)) {
            index.put(word, new HashSet<String>());
        }
        index.get(word).add(url);
    }

    public List<String> searchWord(String word) throws java.rmi.RemoteException {
        //USAR O HASHSET !!!!!!!
        HashSet<String> urls = index.get(word);
        if (urls == null) {
            return new ArrayList<String>();
        }

        return new ArrayList<String>(urls);
    }

}