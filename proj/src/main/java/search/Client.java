package search;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.util.*;
import java.net.MalformedURLException; //tudo o que é java .net é para a validaçao dos urls
import java.net.URI;
import java.net.URISyntaxException;


public class Client extends UnicastRemoteObject implements IntClient { 
    private IClientGateway gateway;
    private static List<String> resultados = new ArrayList<>();
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

            } catch (NotBoundException e) { // este tipo de erro so é aplicado por causa do lookup pois nao é um problema de rede logo nao é tratado pelo outro
                System.out.println("Erro: O serviço 'GatewayService' não está registrado no RMI Registry.");
                e.printStackTrace();

            } catch (RemoteException e) {
                System.out.println("Erro: Problema na comunicação remota com o RMI Registry.");
                e.printStackTrace();
            }

            tentativas++;
            if (tentativas < 3) { // dar retrys é uma boa pratica porque java rmi é at most once
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
   public static boolean isValidURL(String url) {//funcao para validar urls usa os imports java.net
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
        System.out.print("Escolha: ");

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
                        System.out.print("Digite a palavra a pesquisar: ");
                        String word = sc.nextLine();
                        int page = 1;
                        boolean continuar = true;
                        while (continuar) {
                            client.pesquisar(word, page);
                            if(resultados.size() == 10){
                                System.out.print("Deseja ver a próxima página? (s/n): ");
                                String resposta = sc.nextLine().trim().toLowerCase();
                                if (resposta.equals("s")) {
                                    page++;
                                } else {
                                    continuar = false;
                                }
                            }else{
                                continuar= false;
                            }
                        }
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

    public void pesquisar(String word, int page) throws RemoteException {
        if (gateway != null) {
            resultados = gateway.request_index(word, page);  
            System.out.println("Palavra enviada para o barrel via Gateway: " + word);
            
            if (resultados != null && !resultados.isEmpty()) {
                System.out.println("URLs encontradas para '" + word + "':");
                for (String url : resultados) {
                    System.out.println(" - " + url);
                }
            } else {
                System.out.println("Nenhuma URL encontrada para a palavra: " + word);
            }
        } else {
            System.out.println("Erro: Gateway não está conectado!");
        }
    }

}