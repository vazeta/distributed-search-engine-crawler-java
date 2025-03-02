package search;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;


public class Client extends UnicastRemoteObject implements IntClient { 
    private IClientGateway gateway;

    HashMap<String, HashSet<String>> index = new HashMap<String, HashSet<String>>(); // indice de palavras e urls associadas  //meter no barrel

    Stack<String> pilha = new Stack<>(); //pilha para armazenar urls


    public Client() throws RemoteException {
        super();
        if (!gatewayconnect()) {
            throw new RemoteException("Falha: Não foi possível conectar ao Gateway.");
        }
    }

    public boolean gatewayconnect() {
        int tentativas = 0;
        while (tentativas < 3) {
            try {
                Registry registry = LocateRegistry.getRegistry("localhost");
                gateway = (IClientGateway) registry.lookup("GatewayService");
                System.out.println("Conectado ao Gateway!");
                return true; // Conexão bem-sucedida

            } catch (NotBoundException e) {
                System.out.println("Erro: O serviço 'GatewayService' não está registrado no RMI Registry.");
                e.printStackTrace();

            } catch (RemoteException e) {
                System.out.println("Erro: Problema na comunicação remota com o RMI Registry.");
                e.printStackTrace();
            }

            tentativas++;
            if (tentativas < 3) {
                System.out.println("Tentando novamente em 2 segundos... (Tentativa " + (tentativas + 1) + "/3)");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }

        return false; // Se todas as tentativas falharem
    }


    public void enviarURL(String url) throws RemoteException {
        if (gateway != null) {
            gateway.addUrlToQueue(url);  
            System.out.println(" URL enviada para a queue via Gateway: " + url);
        } else {
            System.out.println(" Erro: Gateway não está conectado!");
        }
    }

   public static boolean isValidURL(String url) {
    try {
        new URI(url).toURL(); 
        return true;
    } catch (URISyntaxException | MalformedURLException e) {
        return false;
    }
}


    private static void menu(){
        System.out.println("\n ----Bem vindo ao GOOGOL!!!-----");
        System.out.println("Selecione uma das seguintes opções:");
        System.out.println("1 - Indexar um novo URL");
        System.out.println("2 - Fazer uma pesquisa");
        System.out.println("3 -  Sair");
        System.out.print(" Escolha: ");

    }
    
    

    

    public static void main(String args[]) {
        try {
            Client client = new Client();  // Só prossegue se a conexão for bem-sucedida
            Scanner sc = new Scanner(System.in);

            while (true) {
                menu();
                int input;
                try {
                    input = Integer.parseInt(sc.nextLine());
                } catch (NumberFormatException e) {
                    System.out.println("Entrada inválida! Digite um número.");
                    continue;
                }

                switch (input) {
                    case 1:
                        System.out.print(" Digite a URL para indexação: ");
                        String url = sc.nextLine();
                        if (isValidURL(url)) {
                            client.enviarURL(url);  
                        } else {
                            System.out.println("Erro: O que inseriste não é um URL válido!");
                        }
                        break;

                    case 2:
                        System.out.print("🔎 Digite a palavra a pesquisar: ");
                        String word = sc.nextLine();
                        client.searchWord(word);
                        break;

                    case 3:
                        System.out.println("A sair do Googol...");
                        sc.close();
                        return;

                    default:
                        System.out.println("Opção inválida! Tente novamente.");
                }
            }
        } catch (RemoteException e) {
            System.out.println("O programa não pôde ser iniciado pois a conexão ao Gateway falhou!");
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