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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.rmi.RemoteException;

public class Downloader {
    private static Set<String> stop_words = new HashSet<>();
    private IBarrelGateway barrel;
    private static URLQueue queue;
    private static ArrayList<String> palavras = new ArrayList<>();
    //private ConcurrentMap<String, Boolean> processedUrls = new ConcurrentHashMap<>();
    private static String titulo;
    private static String citacao;
    private static final String MULTICAST_GROUP = "230.0.0.1";
    private static final int MULTICAST_PORT = 4447;

    public static void main(String[] args) throws IOException {
        carregarStopWords("lib/stopwords.txt");
        try {
            Registry urlregistry = LocateRegistry.getRegistry(1098);
            queue = (URLQueue) urlregistry.lookup("URLQueue");

            System.out.println("Downloaders iniciados.");

            int numDownloaders = 3;
            for (int i = 0; i < numDownloaders; i++) {
                new Thread(new DownloaderTask(queue)).start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class DownloaderTask implements Runnable {
        private static URLQueue queue;

        public DownloaderTask(URLQueue queue) {
            this.queue = queue;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    String url = queue.getNextURL();
                    if (url != null) {  
                        System.out.println(Thread.currentThread().getName() + " processando: " + url);
                        processarPagina(url, queue);
                        try {
                            String mensagem = "URL: " + url + "\nTitle: " + titulo + "\nCitation: " + citacao + "\n.";
                            enviarReliableMulticast(mensagem);
                        } catch (Exception e) {
                            // TODO: handle exception
                        }
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

    private static void enviarReliableMulticast(String mensagem) {
        try {
            InetAddress group = InetAddress.getByName(MULTICAST_GROUP);
            DatagramSocket socket = new DatagramSocket();
            byte[] buffer = mensagem.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, MULTICAST_PORT);
            socket.send(packet);
            socket.close();
            System.out.println("Mensagem multicast enviada: " + mensagem);
        } catch (Exception e) {
            System.out.println("Erro ao enviar mensagem multicast");
            e.printStackTrace();
        }
    }
   


    private static void processarPagina(String url, URLQueue queue) {
        try {
            System.out.println(Thread.currentThread().getName() + " baixando: " + url);
            Document doc = Jsoup.connect(url).get();
            String texto = doc.text();
            titulo = doc.title();
            citacao = doc.select("meta[name=description]").attr("content");
            //Elements links = doc.select("a[href]");
            //HashSet<String> uniqueUrls = new HashSet<>();
            // for (Element link : links) {
            //     String linkAbsoluto = link.absUrl("href");
            //     if (isValidURL(linkAbsoluto) && processedUrls.putIfAbsent(linkAbsoluto, true) == null) {
            //         queue.addURL(linkAbsoluto);
            //         System.out.println(Thread.currentThread().getName() + " encontrou nova URL: " + linkAbsoluto);
            //     }
            // }
            processarPalavras(texto, url);
        } catch (Exception e) {
            System.out.println("Erro ao processar a URL: " + url);
            e.printStackTrace();
        }
    }

    private static void processarPalavras(String texto, String url) {
        StringTokenizer tokenizer = new StringTokenizer(texto, " \t\n\r\f.,;:!?()[]\"'");
        while (tokenizer.hasMoreTokens()) {
            String palavra = tokenizer.nextToken().toLowerCase();
            if (palavra.matches("[a-záéíóúãõâêîôûç]+") && !isStopWord(palavra) ) {
                palavras.add(palavra);
            }
        }
    }

    private static boolean isStopWord(String word) {
        return stop_words.contains(word);
      }

    private static void carregarStopWords(String filename) {
    try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
        String line;
        while ((line = reader.readLine()) != null) {
        String word = line.trim().toLowerCase();
        if (!word.isEmpty()) {
            stop_words.add((word));
        }
        }
    } catch (IOException e) {
        System.err.println("Erro a carregar stop words.");
        System.exit(1);
    }
    }

    public boolean isValidURL(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            return scheme != null && (scheme.equals("http") || scheme.equals("https"));
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
