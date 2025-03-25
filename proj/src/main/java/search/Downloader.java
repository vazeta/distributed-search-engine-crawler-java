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
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import org.jsoup.nodes.Element;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.HttpStatusException;

public class Downloader {
    private static Set<String> stop_words = new HashSet<>();
    private static URLQueue queue;
    private static final String LISTA_URLS_FILE = "ListaUrls.obj";
    private static HashSet<String> urlListFile;
    private static ReliableMulticastService multicastService1;
    private static int size_inicial;
    private static HashSet<String> uniqueUrls = new HashSet<>();
    
        public static void main(String[] args) throws IOException {
            urlListFile = StorageUtil.loadData("ListaUrls.obj", new HashSet<>());
            size_inicial = urlListFile.size();
            carregarStopWords("lib/stopwords.txt");
            try {
    
                Registry registry;
                try {
    
                    registry = LocateRegistry.getRegistry("localhost", 1097);
                    System.out.println("Conectando ao registro existente na porta 1097...");
                    registry.list();
                } catch (Exception e) {
    
                    System.out.println("Falha ao conectar ao RMI Registry na porta 1097.");
                    e.printStackTrace();
                    return;
                }
    
                try {
                    multicastService1 = (ReliableMulticastService) registry.lookup("ReliableMulticast");
                    System.out.println("ReliableMulticastService encontrado com sucesso.");
                } catch (NotBoundException e) {
                    System.out.println("ReliableMulticastService não encontrado no RMI Registry na porta 1097.");
                    e.printStackTrace();
                    return;
                }
                Registry urlregistry = LocateRegistry.getRegistry("localhost", 1099);
                queue = (URLQueue) urlregistry.lookup("URLQueue");
    
                System.out.println("Downloaders iniciados.");
    
                int numDownloaders = 5;
                for (int i = 0; i < numDownloaders; i++) {
                    new Thread(new DownloaderTask(queue), "Downloader-" + (i + 1)).start();
                }
    
            } catch (Exception e) {
                e.printStackTrace();
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Ctrl+C detectado! Salvando estado antes de sair...");
                Guardar(size_inicial);
            }));
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
    
                        }
    
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    
        private static void processarPagina(String url, URLQueue queue) {
            
            try {
                if (!urlListFile.contains(url)) {
                    System.out.println(Thread.currentThread().getName() + " baixando: " + url);
                    
    
                uniqueUrls.add(url);

                Document doc = Jsoup.connect(url).get();
                String texto = doc.text();
                String titulo = doc.title();
                String citacao = doc.select("meta[name=description]").attr("content");

                if (citacao == null || citacao.isEmpty()) {
                    Element primeiroParagrafo = doc.select("p").first();
                    if (primeiroParagrafo != null) {
                        citacao = primeiroParagrafo.text();
                    }
                }

                Elements links = doc.select("a[href]");
                for (Element link : links) {
                    String linkAbsoluto = link.absUrl("href");

                    int hashIndex = linkAbsoluto.indexOf("#");
                    if (hashIndex != -1) {
                        linkAbsoluto = linkAbsoluto.substring(0, hashIndex);
                    }

                    if (isValidURL(linkAbsoluto) && !urlListFile.contains(linkAbsoluto)
                            && !uniqueUrls.contains(linkAbsoluto)) {
                        queue.addURL(linkAbsoluto);
                        uniqueUrls.add(linkAbsoluto);
                        saveRelation(url, linkAbsoluto);
                    }
                }
                processarPalavras(texto, url, titulo, citacao);
                System.out.println("LINK PROCESSADO!!!!!");
            } else {
                System.out.println("URL: " + url + " já foi processado.");
            }

        } catch (UnsupportedMimeTypeException e) {

            System.out.println("Ignorando URL não suportada (" + e.getMimeType() + "): " + url);

        } catch (HttpStatusException e) {

            if (e.getStatusCode() == 404) {
                System.out.println("Erro 404: Página não encontrada - " + url);
            } else {
                System.out.println("Erro HTTP " + e.getStatusCode() + " ao acessar: " + url);
            }

        } catch (IOException e) {
            System.out.println("Erro de conexão ao acessar a URL: " + url);

        } catch (Exception e) {
            System.out.println("Erro inesperado ao processar a URL: " + url);
            e.printStackTrace();
        }
    }

    private static void processarPalavras(String texto, String url, String titulo, String citacao) {
        StringTokenizer tokenizer = new StringTokenizer(texto, " \t\n\r\f.,;:!?()[]\"'");
        while (tokenizer.hasMoreTokens()) {
            String palavra = tokenizer.nextToken().toLowerCase();
            if (palavra.matches("[a-záéíóúãõâêîôûç]+") && !isStopWord(palavra)) {
                String mensagem = palavra + ";URL: " + url + " Titulo: " + titulo + " Citacao: " + citacao;
                try {
                    System.out.println(Thread.currentThread().getName() + " enviando: " + mensagem);
                    multicastService1.sendReliableMessage(mensagem);
                } catch (RemoteException e) {
                    System.out.println("Erro ao enviar mensagem via ReliableMulticastService!");
                }

            }
        }
        urlListFile.add(url);
    }

    private static void Guardar(int sizeInicial) {
        List<String> urlList = new ArrayList<>(urlListFile);
        for (int i = sizeInicial; i < urlList.size(); i++) {
            salvarURL(urlList.get(i));
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

    public static boolean isValidURL(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            return scheme != null && (scheme.equals("http") || scheme.equals("https"));
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static void salvarURL(String url) {
        HashSet<String> urlList = StorageUtil.loadData(LISTA_URLS_FILE, new HashSet<>());

        if (urlList.add(url)) {
            StorageUtil.saveData(urlList, LISTA_URLS_FILE);
            System.out.println(" URL salva: " + url);
        }

    }

    private static void saveRelation(String urlOrigem, String atual) {
        String mens = "flag/" + " " + atual + " " + urlOrigem;
        System.out.println(Thread.currentThread().getName() + " enviando: " + mens);
        try {
            multicastService1.sendReliableMessage(mens);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }
}
