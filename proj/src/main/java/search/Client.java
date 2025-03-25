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
                return true;

            } catch (NotBoundException e) {

                System.out.println("Erro: O serviço 'GatewayService' não está registrado no RMI Registry.");

            } catch (RemoteException e) {
                System.out.println("Erro: Problema na comunicação remota com o RMI Registry.");
            }

            tentativas++;
            if (tentativas < 3) {
                System.out.println("Tentando novamente em 2 segundos... (Tentativa " + (tentativas + 1) + "/3)");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    System.out.println("erro");
                }
            }
        }

        return false;
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

            if (uri.getScheme() == null || (!uri.getScheme().equals("http") && !uri.getScheme().equals("https"))) {
                return false;
            }

            uri.toURL();
            return true;
        } catch (URISyntaxException | MalformedURLException e) {
            return false;
        }
    }

    private static void menu() {
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
            Client client = new Client();
            Scanner sc = new Scanner(System.in);

            try {
                StatisticsClient statsClient = new StatisticsClientImpl();
                Registry registry = LocateRegistry.getRegistry("localhost", 1099);
                StatisticsService statsService = (StatisticsService) registry.lookup("StatisticsService");
                statsService.subscribeStatistics(statsClient);
                System.out.println("Cliente inscrito para receber atualizações de estatísticas.");
            } catch (Exception e) {
                System.out.println("Erro no registo do cliente no serviço de estatisticas.");
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
                        int totalPages = 0;

                        while (continuar) {
                            client.pesquisar(word, page);

                            if (resultados.isEmpty()) {
                                System.out.println("Nenhum URL encontrado.");
                                break;
                            }

                            if (resultados.get(resultados.size() - 1).startsWith("tem ")) {
                                totalPages = Integer.parseInt(resultados.get(resultados.size() - 1).split(" ")[1]);
                                resultados.remove(resultados.size() - 1);
                            }

                            for (String resultado : resultados) {
                                System.out.println(resultado);
                            }

                            if (page < totalPages) {
                                System.out.println("Deseja ver outra página? (s/n ou número da página)?");
                                System.out.println("Página " + page + " de " + totalPages);
                                String resposta = sc.nextLine().trim().toLowerCase();

                                if (resposta.equals("s")) {
                                    page++;
                                } else if (resposta.equals("n")) {
                                    continuar = false;
                                } else {
                                    try {
                                        int novaPagina = Integer.parseInt(resposta);
                                        if (novaPagina > 0 && novaPagina <= totalPages) {
                                            page = novaPagina;
                                        } else {
                                            System.out.println("Número de página inválido.");
                                        }
                                    } catch (NumberFormatException e) {
                                        System.out.println("Entrada inválida.");
                                    }
                                }
                            } else {
                                continuar = false;
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
                                System.out
                                        .println("Tempo médio de resposta: " + stats.getAverageResponseTime() + " ms");
                                System.out.println("Top 10 de pesquisas:");
                                for (String term : stats.getTop10Searches()) {
                                    System.out.println(" - " + term);
                                }
                                System.out.println("Tamanho dos indexs:" + stats.getBarrelIndexSizes());
                                System.out.println("------------------------------------------");
                            } else {
                                System.out.println("Ainda não há estatísticas disponíveis.");
                            }
                        } catch (Exception e) {
                            System.out.println("Erro na procura de Stats!");
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
            List<String> tempResultados = gateway.request_index(word, page);

            resultados = tempResultados;

        } else {
            System.out.println("Erro: Gateway não está conectado!");
        }
    }

}