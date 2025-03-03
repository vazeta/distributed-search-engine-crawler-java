package search;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import javax.print.DocFlavor.STRING;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;


public class Downloader {
    private static String[] stop_words;
    private static IBarrelGateway barrel;
    public static void main(String[] args) throws IOException{

        carregarStopWords("lib/stopwords.txt");
        try {

            Registry registry = LocateRegistry.getRegistry(1100);
            barrel = (IBarrelGateway) registry.lookup("Barrel");

            // Conectar ao RMI Registry na porta 1098 para acessar a URL Queue
            Registry urlregistry = LocateRegistry.getRegistry(1098);
            URLQueue queue = (URLQueue) urlregistry.lookup("URLQueue");

            System.out.println("Donlowader iniciado.");

            while (true) {
                String url = queue.getNextURL();
                if (url != null) {
                    System.out.println("URL obtida da queue: " + url);
                    processarPagina(url, queue);
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
    private static void processarPagina(String url, URLQueue queue) {
        try {
            System.out.println(" A baixar a página: " + url);
            Document doc = Jsoup.connect(url).get(); // Baixa a página HTML
            String texto = doc.text(); // Extrai apenas o texto puro
            Elements links = doc.select("a[href]");
            HashSet<String> uniqueUrls = new HashSet<>();

            for(Element link : links){
                String linkAbsoluto = link.absUrl("href");
                if (isValidURL(linkAbsoluto) && !uniqueUrls.contains(linkAbsoluto)) {
                    uniqueUrls.add(linkAbsoluto);
                    //System.out.println("Nova url enontrada e enviada para a queue: " + linkAbsoluto);  
                }
            }
            for(String urlNovo : uniqueUrls){
                queue.addURL(urlNovo);
                 System.out.println("Nova url enontrada e enviada para a queue: " + urlNovo);  

            }



            System.out.println(" Texto extraído da página:");
            imprimirPalavras(texto, url);

        } catch (Exception e) {
            System.out.println("Erro ao processar a URL: " + url);
            e.printStackTrace();
        }
    }

    private static boolean isStopWord(String palavra) {
        for (String stopWord : stop_words) {
            if (stopWord.equals(palavra)) {
                return true;
            }
        }
        return false;
    }
    
     
    private static void imprimirPalavras(String texto, String url) {
        StringTokenizer tokenizer = new StringTokenizer(texto, " \t\n\r\f.,;:!?()[]\"'"); // Divide o texto em palavras em espaços em quebras e tambem nas pontuações
        System.out.println(" Palavras extraídas:");
        while (tokenizer.hasMoreTokens()) {
            String palavra = tokenizer.nextToken().toLowerCase();
            if (palavra.matches("[a-záéíóúãõâêîôûç]+") ) { //de "a" a "z" mais as exceções da lingua portuguesa
                System.out.println(" - " + palavra);
            }
            try {
                barrel.storeData(palavra, url);
            } catch (RemoteException e ) {
                // TODO: handle exception
                System.out.println("Erro a envia a palavra aos barrels");
                e.printStackTrace();
            }
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

    public static boolean isValidURL(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
    
            if (scheme == null || !(scheme.equals("http") || scheme.equals("https"))) {
                return false;
            }
    
            uri.toURL(); // Converte para URL sem usar o construtor obsoleto
            return true;
        } catch (URISyntaxException | MalformedURLException e) {
            return false;
        }
    }

}
