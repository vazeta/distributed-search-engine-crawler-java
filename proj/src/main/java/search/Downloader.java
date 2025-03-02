package search;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.rmi.RemoteException;

public class Downloader {
    public static void main(String[] args) {
        try {
            // Conectar ao RMI Registry na porta 1098 para acessar a URL Queue
            Registry registry = LocateRegistry.getRegistry(1098);
            URLQueue queue = (URLQueue) registry.lookup("URLQueue");

            // Obter uma URL da fila e imprimir no console
            String url = queue.getNextURL();
            if (url != null) {
                System.out.println("🔗 URL obtida da queue: " + url);
            } else {
                System.out.println("⚠️ A fila de URLs está vazia!");
            }

            // ---- Comentado: Downloaders ainda não processam as páginas ----
            /*
            // Conectar ao IndexStorageBarrel via RMI
            IBarrelGateway barrel = (IBarrelGateway) LocateRegistry.getRegistry(1100).lookup("Barrel");

            // Conectar ao Client via RMI
            IntClient client = (IntClient) LocateRegistry.getRegistry(8183).lookup("index");

            while (true) {
                String url = client.takeNext(); // Obter um URL da fila do Client
                System.out.println("Processando URL: " + url);

                Document doc = Jsoup.connect(url).get(); // Baixa a página HTML
                String text = doc.text(); // Remove todas as tags HTML e mantém apenas o texto

                StringTokenizer tokenizer = new StringTokenizer(text, " "); // Divide o texto em palavras

                while (tokenizer.hasMoreTokens()) {
                    String word = tokenizer.nextToken().toLowerCase(); // Converte para minúsculas
                    barrel.storeData(word, url); // Envia palavra + URL para o Storage Barrel
                }
            }
            */
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
