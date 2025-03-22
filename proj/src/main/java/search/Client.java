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

            } catch (RemoteException e) {
                System.out.println("Erro: Problema na comunicação remota com o RMI Registry.");
            }

            tentativas++;
            if (tentativas < 3) { // dar retrys é uma boa pratica porque java rmi é at most once
                System.out.println("Tentando novamente em 2 segundos... (Tentativa " + (tentativas + 1) + "/3)");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    System.out.println("erro");
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
            URI uri = new URI(url);
            
            // Verifica se tem um esquema válido (http ou https)
            if (uri.getScheme() == null || (!uri.getScheme().equals("http") && !uri.getScheme().equals("https"))) {
                return false;
            }
            
            uri.toURL(); // Isso confirma que a URL pode ser convertida
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
        System.out.println("3 - Saber as páginas que apontam para uma certa página");
        System.out.println("4 - Consultar estatísticas do sistema");
        System.out.println("5 -  Sair");
        System.out.print("Escolha: ");

    }
    public static void main(String args[]) {
        try {
            Client client = new Client();  // Só prossegue se a conexão for bem-sucedida
            Scanner sc = new Scanner(System.in);

            try {
                StatisticsClient statsClient = new StatisticsClientImpl();
                Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                StatisticsService statsService = (StatisticsService) registry.lookup("StatisticsService");
                statsService.subscribeStatistics(statsClient);
                System.out.println("Cliente inscrito para receber atualizações de estatísticas.");
            } catch (Exception e) {
                e.printStackTrace();
            }

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
                                } else if(resposta.equals("n")){
                                    continuar = false;
                                }
                            }else{
                                continuar= false;
                            }
                        }
                        break;

                    case 3:
                        System.out.println("Introduza o link:");
                        String url1 = sc.nextLine();
                        if (isValidURL(url1)) {
                            client.links_quant(url1);
                        } else {
                            System.out.println("Erro: O que inseriste não é um URL válido!");
                        }
                        break;
                    case 4:
                        try {
                            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                            StatisticsService statsService = (StatisticsService) registry.lookup("StatisticsService");
                            Statistics stats = statsService.getStats();
                            if (stats != null) {
                                System.out.print(" \n ");
                                System.out.println("----------------------------------------------");
                                System.out.println("ESTATISTICAS!!:");
                                System.out.println("Total de pesquisas: " + stats.getNumSearches());
                                System.out.println("Barrels ativos: " + stats.getActiveBarrels());
                                System.out.println("Tempo médio de resposta: " + stats.getAverageResponseTime() + " ms");
                                System.out.println("Top 10 de pesquisas:");
                                for (String term : stats.getTop10Searches()) {
                                    System.out.println(" - " + term);
                                }
                                System.out.println("Tamanho dos indexs:"+ stats.getBarrelIndexSizes());
                                System.out.println("------------------------------------------");
                            } else {
                                System.out.println("Ainda não há estatísticas disponíveis.");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;


                    case 5:
                        System.out.println("A sair do Googol...");
                        sc.close();
                        return;
                   

                    default:
                        System.out.println("Opção inválida! Tente novamente.");
                }
            }
        } catch (RemoteException e) {
            System.out.println("O programa não pôde ser iniciado pois a conexão ao Gateway falhou!");
            return;
        }
    }
    public void links_quant(String link) throws RemoteException {
        if (gateway != null) {
            resultados = gateway.request_url_related(link);  
            System.out.println("Link enviada para o barrel via Gateway: " + link);
            
            if (resultados != null && !resultados.isEmpty()) {
                System.out.println("URLs que apontam para '" + link + "':");
                for (String url : resultados) {
                    System.out.println(" - " + url);
                }
            } else {
                System.out.println("Nenhuma URL encontrada para o link: " + link);
            }
        } else {
            System.out.println("Erro: Gateway não está conectado!");
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