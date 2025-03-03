package search;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;

public class Downloader {
    private static String[] stop_words;
    private static IBarrelGateway barrel;
    private static URLQueue queue;
    private static ConcurrentMap<String, Boolean> processedUrls = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        carregarStopWords("lib/stopwords.txt");

        try {
            Registry registry = LocateRegistry.getRegistry(1100);
            barrel = (IBarrelGateway) registry.lookup("Barrel");

            Registry urlregistry = LocateRegistry.getRegistry(1098);
            queue = (URLQueue) urlregistry.lookup("URLQueue");

            System.out.println("Downloaders iniciados.");

            int numDownloaders = 3;
            for (int i = 0; i < numDownloaders; i++) {
                new Thread(new DownloaderTask(queue, barrel)).start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class DownloaderTask implements Runnable {
        private URLQueue queue;
        private IBarrelGateway barrel;

        public DownloaderTask(URLQueue queue, IBarrelGateway barrel) {
            this.queue = queue;
            this.barrel = barrel;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    String url = queue.getNextURL();
                    if (url != null) {  
                        System.out.println(Thread.currentThread().getName() + " processando: " + url);
                        processarPagina(url, queue, barrel);
                    } else {
                        System.out.println(Thread.currentThread().getName() + " - Fila vazia, tentando novamente em 3s...");
                        Thread.sleep(3000);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void processarPagina(String url, URLQueue queue, IBarrelGateway barrel) {
        try {
            System.out.println(Thread.currentThread().getName() + " baixando: " + url);
            Document doc = Jsoup.connect(url).get();
            String texto = doc.text();
            Elements links = doc.select("a[href]");
            HashSet<String> uniqueUrls = new HashSet<>();

            // for (Element link : links) {
            //     String linkAbsoluto = link.absUrl("href");
            //     if (isValidURL(linkAbsoluto) && processedUrls.putIfAbsent(linkAbsoluto, true) == null) {
            //         queue.addURL(linkAbsoluto);
            //         System.out.println(Thread.currentThread().getName() + " encontrou nova URL: " + linkAbsoluto);
            //     }
            // }

            imprimirPalavras(texto, url, barrel);
        } catch (Exception e) {
            System.out.println("Erro ao processar a URL: " + url);
            e.printStackTrace();
        }
    }

    private static void imprimirPalavras(String texto, String url, IBarrelGateway barrel) {
        StringTokenizer tokenizer = new StringTokenizer(texto, " \t\n\r\f.,;:!?()[]\"'");
        while (tokenizer.hasMoreTokens()) {
            String palavra = tokenizer.nextToken().toLowerCase();
            if (palavra.matches("[a-záéíóúãõâêîôûç]+")) {
                try {
                    barrel.storeData(palavra, url);
                } catch (RemoteException e) {
                    System.out.println("Erro ao enviar palavra aos barrels.");
                    e.printStackTrace();
                }
            }
        }
    }

    private static void carregarStopWords(String caminhoFile) throws IOException {
        List<String> lines = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new FileReader(caminhoFile));
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line.strip());
        }
        reader.close();
        stop_words = lines.toArray(new String[0]);
        System.out.println("Total de palavras carregadas: " + stop_words.length);
    }

    public static boolean isValidURL(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            return scheme != null && (scheme.equals("http") || scheme.equals("https"));
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
