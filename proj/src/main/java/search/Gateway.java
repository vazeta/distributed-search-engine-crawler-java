package search;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Gateway extends UnicastRemoteObject implements IClientGateway {
    
    public Gateway() throws RemoteException {
        super();
        gatewayReg(); // Registra o serviço RMI
    }

    private void gatewayReg() {
        try {
            Registry registry;
            try {
                // Criar um novo RMI Registry na porta 1099
                registry = LocateRegistry.createRegistry(1099);
                System.out.println("Novo RMI Registry criado na porta 1099.");
            } catch (RemoteException e) {
                // Se já existir, conecta-se a ele
                System.out.println("RMI Registry já existente. Conectando...");
                registry = LocateRegistry.getRegistry(1099);
            }

            // Registra o `Gateway` no RMI
            registry.rebind("GatewayService", this);
            System.out.println("Gateway RMI registrado com sucesso.");
            
        } catch (RemoteException e) {
            System.out.println("Erro ao registrar o Gateway no RMI!");
            e.printStackTrace();
        }
    }

    @Override
    public void connectClient(IntClient client) throws RemoteException {
        System.out.println("Cliente conectado com sucesso: " + client);
    }

    @Override
    public void addUrlToQueue(String url) throws RemoteException {
        try {
            Registry registry = LocateRegistry.getRegistry(1099);
            URLQueue queue = (URLQueue) registry.lookup("URLQueue");
            queue.addURL(url);
            System.out.println("Gateway: URL " + url + " enviada para a fila");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<String> request_url_related(String link) throws RemoteException {
        Registry registry;
        String[] barrels;

        try {
            registry = LocateRegistry.getRegistry(1100);
        } catch (Exception e) {
            throw new RemoteException("Erro ao conectar ao RMI Registry!", e);
        }

        while (true) { 
            try {
                barrels = registry.list(); 

                if (barrels.length == 0) {
                    System.out.println("Nenhum Barrel disponível. Tentando novamente em 2 segundos...");
                    Thread.sleep(2000);
                }

                List<String> listaBarrels = new ArrayList<>(Arrays.asList(barrels));

                for (String selectedBarrel : listaBarrels) {
                    try {
                        System.out.println("A tentar conectar ao Barrel: " + selectedBarrel);
                        IBarrelGateway barrel = (IBarrelGateway) registry.lookup(selectedBarrel);
                        System.out.println("consegui");
                        List<String> results = barrel.related_links(link);
                        return results;
                    } catch (Exception e) {
                        System.out.println("Erro ao conectar ao Barrel " + selectedBarrel + ". Tentando outro...");
                        e.printStackTrace();
                    }
                }

                System.out.println("Todos os Barrels falharam. Tentando novamente em 2 segundos...");
                Thread.sleep(2000);

            } catch (Exception e) {
                e.printStackTrace();
                throw new RemoteException("Erro ao buscar palavra.", e);
            }
        }
    }

    @Override
    public List<String> request_index(String word, int page) throws RemoteException {
        Registry registry;
        String[] barrels;

        try {
            registry = LocateRegistry.getRegistry(1099);
        } catch (Exception e) {
            throw new RemoteException("Erro ao conectar ao RMI Registry!", e);
        }

        while (true) { 
            try {
                barrels = registry.list(); 

                if (barrels.length == 0) {
                    System.out.println("Nenhum Barrel disponível. Tentando novamente em 2 segundos...");
                    Thread.sleep(2000);
                }

                List<String> listaBarrels = new ArrayList<>(Arrays.asList(barrels));
                List<String> filteredBarrels = new ArrayList<>();
                for (String barrel : listaBarrels) {
                    if (barrel.startsWith("Barrel")) {
                        filteredBarrels.add(barrel);
                    }
                }
                listaBarrels = filteredBarrels;


                for (String selectedBarrel : listaBarrels) {
                    try {
                        System.out.println("A tentar conectar ao Barrel: " + selectedBarrel);
                        IBarrelGateway barrel = (IBarrelGateway) registry.lookup(selectedBarrel);
                        System.out.println("consegui");
                        List<String> results = barrel.search(word, page);
                        return results;
                    } catch (Exception e) {
                        System.out.println("Erro ao conectar ao Barrel " + selectedBarrel + ". Tentando outro...");
                    }
                }

                System.out.println("Todos os Barrels falharam. Tentando novamente em 2 segundos...");
                Thread.sleep(2000);

            } catch (Exception e) {
                e.printStackTrace();
                throw new RemoteException("Erro ao buscar palavra.", e);
            }
        }
    }

    public static void main(String[] args) {
        try {
            new Gateway();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
