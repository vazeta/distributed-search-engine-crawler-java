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
    public static void main(String[] args) throws IOException {
        urlListFile = StorageUtil.loadData("ListaUrls.obj", new HashSet<>());
        size_inicial=urlListFile.size();
        carregarStopWords("lib/stopwords.txt");
        try {
            ReliableMulticastService multicastService = new ReliableMulticastServiceImpl();
            Registry registry;

            try {
                // Tenta obter um registro existente
                registry = LocateRegistry.getRegistry(1097);
                registry.list(); // Testa se o registro já está disponível
            } catch (Exception e) {
                // Se não existir, cria um novo
                registry = LocateRegistry.createRegistry(1097);
            }
            registry.rebind("ReliableMulticastService", multicastService);
            System.out.println("ReliableMulticastService registrado com sucesso.");

            try {
                multicastService1 = (ReliableMulticastService) registry.lookup("ReliableMulticastService");
            } catch (NotBoundException e) {
                System.out.println("Reliable multicast nao encontrado");
                e.printStackTrace();
            }
   
            Registry urlregistry = LocateRegistry.getRegistry(1098);
            queue = (URLQueue) urlregistry.lookup("URLQueue");

            System.out.println("Downloaders iniciados.");

            int numDownloaders = 2;
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
                
                // Faz o download da página
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
    
                // Captura todos os links na página
                    
                
                Elements links = doc.select("a[href]");
                HashSet<String> uniqueUrls = new HashSet<>();
                for (Element link : links) {
                    String linkAbsoluto = link.absUrl("href");
                    if (isValidURL(linkAbsoluto) && !urlListFile.contains(linkAbsoluto)) {
                        queue.addURL(linkAbsoluto);
                        uniqueUrls.add(linkAbsoluto);
                        System.out.println(Thread.currentThread().getName() + " encontrou nova URL: " + linkAbsoluto);
                    }
                }
                saveRelation(url, uniqueUrls);
                
                processarPalavras(texto, url, titulo, citacao);
                System.out.println("LINK PROCESSADO!!!!!");
            } else {
                System.out.println("URL: " + url + " já foi processado.");
            }
            
        } catch (UnsupportedMimeTypeException e) {
            // Ignora PDFs e outros tipos não suportados
            System.out.println("Ignorando URL não suportada (" + e.getMimeType() + "): " + url);
            
        } catch (HttpStatusException e) {
            // Trata erro 404 ou outros erros HTTP
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
                    multicastService1.sendReliableMessage(mensagem);
                } catch (RemoteException  e) {
                    System.out.println("Erro ao enviar mensagem via ReliableMulticastService");
                    e.printStackTrace();
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

    synchronized private static void saveRelation(String urlOrigem, HashSet<String> linksInternos) {
        for(String atual : linksInternos){
            String mens = "flag/" +" "+ atual + " " + urlOrigem;
            try {
                multicastService1.sendReliableMessage(mens);
            } catch (RemoteException e) {
               e.printStackTrace();
            }
            
        }
    }
}
