package search;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.Queue;

public class URLQueueImpl extends UnicastRemoteObject implements URLQueue {
    private static final long serialVersionUID = 1L;
    private Queue<String> urlQueue;

    protected URLQueueImpl() throws RemoteException {
        super();
        urlQueue = new LinkedList<>();
    }

    @Override
    public synchronized void addURL(String url) throws RemoteException {
        urlQueue.add(url);
        System.out.println("URL adicionada à fila: " + url);
        notifyAll();
    }

    @Override
    public synchronized String getNextURL() throws RemoteException {
        while (urlQueue.isEmpty()) {
            try{
                System.out.println("Downloader a espera de URLS...");
                wait();
            }catch(InterruptedException e){
                Thread.currentThread().interrupt();
            }
            
        }
        String url = urlQueue.poll();
        System.out.println("A enviar URL para downloader: "+ url);
        return url;
    }

    public static void main(String[] args) {
        try {
            URLQueueImpl queue = new URLQueueImpl();
            Registry registry = LocateRegistry.createRegistry(1098); // Porta específica para fila
            registry.rebind("URLQueue", queue);
            System.out.println("Fila de URLs pronta!");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
