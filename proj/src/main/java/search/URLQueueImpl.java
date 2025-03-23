package search;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class URLQueueImpl extends UnicastRemoteObject implements URLQueue {
    private static final long serialVersionUID = 1L;
    private final Queue<String> urlQueue;
    private final Set<String> processedUrls;

    protected URLQueueImpl() throws RemoteException {
        super();
        urlQueue = new LinkedList<>();
        processedUrls = new HashSet<>();
    }

    @Override
    public synchronized void addURL(String url) throws RemoteException {
        if (!processedUrls.contains(url)) {
            urlQueue.add(url);
            processedUrls.add(url);
            System.out.println("URL adicionada à fila: " + url);
            notify();
        } else {
            System.out.println("URL já foi processada: " + url);
        }
    }

    @Override
    public synchronized String getNextURL() throws RemoteException {
        while (urlQueue.isEmpty()) {
            try {
                System.out.println("Downloader esperando por URLs...");
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        String url = urlQueue.poll();
        System.out.println("Enviando URL para downloader: " + url);
        return url;
    }

    public static void main(String[] args) {
        try {
            Registry registry = null;
            try {
                registry = LocateRegistry.getRegistry(1099);
                registry.list();
            } catch (RemoteException e) {
                System.out.println("RMI Registry não encontrado.");
            }

            URLQueueImpl queue = new URLQueueImpl();
            registry.rebind("URLQueue", queue);
            System.out.println("Fila de URLs pronta para uso!");

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
