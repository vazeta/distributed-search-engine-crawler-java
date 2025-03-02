package search;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;

public class Downloader {
    private static String[] stop_words;
    public static void main(String[] args) throws IOException{

        carregarStopWords("lib/stopwords.txt");
        try {

            // Conectar ao RMI Registry na porta 1098 para acessar a URL Queue
            Registry registry = LocateRegistry.getRegistry(1098);
            URLQueue queue = (URLQueue) registry.lookup("URLQueue");

            System.out.println("Donlowader iniciado.");

            while (true) {
                String url = queue.getNextURL();
                if (url != null) {
                    System.out.println("URL obtida da queue: " + url);
                    processarPagina(url);
                } else {
                    System.out.println("A fila de URLs está vazia! Tento de novo daqui a 3 segundos...");
                    Thread.sleep(3000);
                }
                
            }
            //  Downloaders ainda não processam as páginas 
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
    private static void processarPagina(String url) {
        try {
            System.out.println(" A baixar a página: " + url);
            Document doc = Jsoup.connect(url).get(); // Baixa a página HTML
            String texto = doc.text(); // Extrai apenas o texto puro

            System.out.println(" Texto extraído da página:");
            imprimirPalavras(texto);

        } catch (Exception e) {
            System.out.println("Erro ao processar a URL: " + url);
            e.printStackTrace();
        }
    }

    private static void imprimirPalavras(String texto) {
        StringTokenizer tokenizer = new StringTokenizer(texto, " \t\n\r\f.,;:!?()[]\"'"); // Divide o texto em palavras

        System.out.println(" Palavras extraídas:");
        while (tokenizer.hasMoreTokens()) {
            String palavra = tokenizer.nextToken().toLowerCase();
            System.out.println(" - " + palavra);
        }
        System.out.println(" Fim da extração de palavras.");
    }

    private static void carregarStopWords(String caminhoFile) throws IOException{
        List<String> lines = new ArrayList<>();

        BufferedReader reader = new BufferedReader(new FileReader(caminhoFile));
        String line;

        while((line = reader.readLine()) != null){
            line = line.strip();
            lines.add(line);
        }
        reader.close();
        stop_words = lines.toArray(new String[0]);

        System.out.println("Total de palavras carregadas: " + stop_words.length);
    }

}
