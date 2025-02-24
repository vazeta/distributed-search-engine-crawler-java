package search;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.concurrent.*;
import java.io.*;
import java.util.*;

public class Client extends UnicastRemoteObject implements IntClient { 
    private IClientGateway gateway;

    HashMap<String, HashSet<String>> index = new HashMap<String, HashSet<String>>(); // indice de palavras e urls associadas

    Stack<String> pilha = new Stack<>(); //pilha para armazenar urls


    public Client() throws RemoteException {
        super();
        
        try {
            gatewayconnect();
            
        } catch (RemoteException e) {
            // TODO: handle exception
            System.out.println("Error in remote connection");
        }

        
        //This structure has a number of problems. The first is that it is fixed size. Can you enumerate the others?            
    }

    public void gatewayconnect() throws RemoteException {
        int tentativas = 0;
        while (tentativas < 3) {
            try {
                // Conectar ao RMI Registry rodando no localhost
                Registry registry = LocateRegistry.getRegistry("localhost");
                gateway = (IClientGateway) registry.lookup("GatewayService"); // Procurar pelo serviço
                
                System.out.println("Connected to Gateway");
                return; // Conectado com sucesso, sai do método
    
            } catch (NotBoundException e) {
                System.out.println("Erro: O serviço 'GatewayService' não está registrado no RMI Registry.");
                e.printStackTrace();
    
                tentativas++; // Incrementa tentativas
    
            } catch (RemoteException e) {
                System.out.println("Erro: Problema na comunicação remota com o RMI Registry.");
                e.printStackTrace();
    
                tentativas++; // Incrementa tentativas
            }
    
            // Espera antes de tentar novamente
            if (tentativas < 3) {
                System.out.println("Tentando novamente em 2 segundos... (Tentativa " + (tentativas + 1) + "/3)");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
        
        // Se todas as tentativas falharem, lança um erro fatal
        throw new RemoteException(" Falha: Não foi possível conectar ao Gateway após 3 tentativas.");
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
        if (pilha.isEmpty()) {
            System.out.println("Aviso: Nenhuma URL disponível no momento.");
            return null; // Retorna null em vez de lançar erro
        }
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