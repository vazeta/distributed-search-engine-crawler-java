package search;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class Barrel extends UnicastRemoteObject implements IBarrelGateway{
    private static final long serialVersionUID = 1L;
    private HashMap<String, HashSet<String>> index;
    
    public Barrel() throws RemoteException{
        super();
        index = new HashMap<>();
        RegisterBarrel();
        
    }

    private void RegisterBarrel(){
        try{
            Registry registry = LocateRegistry.createRegistry(1100);
            registry.rebind("Barrel",this);
            System.out.println("Barrel registrado com sucesso");
        }catch(RemoteException e){
            System.out.println("Erro ao registrar barrel no rmi");
            e.printStackTrace();
        }

    }
    @Override
    public void storeData(String palavra, String url) throws RemoteException {
        if (index.containsKey(palavra)) {
            index.get(palavra).add(url);
        } else {
            HashSet<String> urls = new HashSet<>();
            urls.add(url);
            index.put(palavra, urls);
        }
    }

    @Override
    public void print_index() throws RemoteException {
        for (Map.Entry<String, HashSet<String>> entry : index.entrySet()) {
            String palavra = entry.getKey();  
            HashSet<String> urls = entry.getValue();  
            System.out.println("Palavra: " + palavra);
            System.out.println("URLs associadas:");
            for (String url : urls) {
                System.out.println(" - " + url);
            }
        }
    }
    
    @Override
    public List<String> search(String word) throws RemoteException {
        HashSet<String> urls = index.getOrDefault(word, new HashSet<>());
        return new ArrayList<>(urls);
    }

    public static void main(String[] args) {
        try {
            new Barrel();  // Inicia um novo Storage Barrel
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

}
