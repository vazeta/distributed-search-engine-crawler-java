# Googol - Relatório Meta 2

## 1. Arquitetura de Software

### 1.1 Visão Geral

O projeto Googol é um motor de busca web implementado com uma arquitetura distribuída baseada em RMI (Remote Method Invocation) e integrada com Spring Boot para fornecer uma interface web. O sistema está estruturado com base no padrão MVC (Model-View-Controller), com uma clara separação entre os componentes de front-end e back-end.

### 1.2 Componentes Principais

#### 1.2.1 Módulo de Cliente Web (Spring Boot)

Este módulo é responsável pela interface web do usuário e pela comunicação com o servidor RMI. Os principais componentes são:

* **Controllers** : Responsáveis por processar as requisições HTTP e coordenar as respostas
* **Services** : Encapsulam a lógica de negócio e a comunicação com serviços externos
* **Configuration** : Classes de configuração para RMI, WebSockets e outros aspectos do sistema

#### 1.2.3 Serviços Externos

O sistema também se integra com serviços externos:

* **Hacker News API** : Para indexação de conteúdo do Hacker News
* **OpenAI API** : Para análise e resumo de resultados de busca

### 1.3 Organização do Código

```
com.example.googol
├── ServingWebContentApplication.java (Aplicação principal)
├── config
│   ├── RMIConfig.java (Configuração RMI)
│   ├── RMIRegistrar.java (Registro de serviços RMI)
│   ├── StatisticsServiceConfigurator.java (Configurador de serviços de estatísticas)
│   └── WebSocketConfig.java (Configuração WebSocket)
├── controllers
│   ├── HomeController.java (Controlador da página inicial)
│   ├── IndexController.java (Controlador para indexação de URLs)
│   ├── RelatedLinksController.java (Controlador para links relacionados)
│   ├── SearchController.java (Controlador de buscas)
│   └── StatisticsWebSocketController.java (Controlador de estatísticas via WebSocket)
└── service
    ├── HackerNewsService.java (Serviço para integração com Hacker News)
    └── OpenAIAnalysisService.java (Serviço para análise de resultados via OpenAI)
```

## 2. Integração do Spring Boot com o Servidor RMI

### 2.1 Configuração RMI

A integração com o servidor RMI é feita principalmente através da classe `RMIConfig`, que configura os beans necessários para a comunicação RMI:

```java
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class RMIConfig {

    @Bean
    public IClientGateway gateway() throws RemoteException, NotBoundException, MalformedURLException {
        return (IClientGateway) Naming.lookup("GatewayService");
    }

    @Bean
    public StatisticsServiceImpl statisticsServiceImpl() throws RemoteException {
        return new StatisticsServiceImpl();
    }

    // Configuração do WebSocketController no serviço
    @Bean
    public StatisticsServiceConfigurator statisticsServiceConfigurator(
            StatisticsServiceImpl service,
            StatisticsWebSocketController controller) {
        service.setWebSocketController(controller);
        System.out.println("WebSocketController configurado com sucesso!");
        return new StatisticsServiceConfigurator();
    }
}
```

### 2.2 Registro de Serviços RMI

A classe `RMIRegistrar` é responsável por registar os serviços implementados localmente no registro RMI, tornando-os disponíveis para clientes remotos:

```java
@Component
public class RMIRegistrar {

    @Autowired
    private StatisticsServiceImpl statisticsServiceImpl;

    @PostConstruct
    public void registerRMI() {
        try {
            Registry registry = LocateRegistry.createRegistry(1100);
            registry.rebind("StatisticsService", statisticsServiceImpl);
            System.out.println("Registrado no RMI com sucesso!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### 2.3 Utilização dos Serviços RMI

Os controladores utilizam os serviços RMI injetados pelo Spring para realizar operações como busca, indexação e recuperação de estatísticas:

Por exemplo:

```java
@Controller
public class IndexController {
    @Autowired
    private IClientGateway gateway;

    @PostMapping("/add-url")
    public String addUrl(@RequestParam("url") String url, Model model) throws RemoteException {
        try {
            gateway.addUrlToQueue(url);
            model.addAttribute("message", "O link está a ser processado!");
        } catch (Exception e) {
            model.addAttribute("error", "O indexamento do link falhou.");
        }
        return "greeting";
    }
}
```

## 3. Programação de WebSockets e Integração com RMI

### 3.1 Configuração WebSocket

A configuração WebSocket é feita através da classe `WebSocketConfig`:

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
  
    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        // Habilita um broker simples com destino "/topic"
        config.enableSimpleBroker("/topic");
        // Destinos enviados pelo cliente serão prefixados com "/app"
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        // Define o endpoint para conexão dos clientes com fallback SockJS
        registry.addEndpoint("/ws").withSockJS();
    }
}
```

### 3.2 Controlador WebSocket

O `StatisticsWebSocketController` gerencia a comunicação em tempo real com os clientes e envia atualizações de estatísticas:

```java
@Controller
public class StatisticsWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private StatisticsService statisticsService;

    @MessageMapping("/requestStats")
    public void requestStats() {
        try {
            Statistics stats = statisticsService.getStats();
            sendStats(stats);
        } catch (RemoteException e) {
            // Log do erro
            System.err.println("Error requesting statistics: " + e.getMessage());
            e.printStackTrace();
      
            // Envio de mensagem de erro para o cliente
            messagingTemplate.convertAndSend("/topic/error", "Failed to retrieve statistics: " + e.getMessage());
        }
    }

    public void sendStats(Statistics stats) {
        if (stats == null) {
            System.err.println("Warning: Attempted to send null statistics");
            messagingTemplate.convertAndSend("/topic/stats", new Statistics(0, 0, 0));
        } else {
            System.out.println("Sending statistics: " + stats.getActiveBarrels() + " active barrels");
            messagingTemplate.convertAndSend("/topic/stats", stats);
        }
    }
}
```

### 3.3 Integração entre WebSockets e RMI

A integração entre WebSockets e RMI ocorre através do serviço de estatísticas:

1. O serviço `StatisticsServiceImpl` implementa a interface RMI `StatisticsService`
2. O serviço é configurado para utilizar o controlador WebSocket em `RMIConfig.statisticsServiceConfigurator`
3. Quando os dados são atualizados via RMI, o serviço notifica o controlador WebSocket
4. O controlador WebSocket envia os dados atualizados para os clientes conectados

Esta integração permite que as estatísticas do sistema sejam atualizadas em tempo real na interface do usuário, sem necessidade de recarregar a página.

## 4. Integração com Serviços REST

### 4.1 Serviço Hacker News

O `HackerNewsService` integra-se com a API REST do Hacker News para buscar histórias e indexá-las no sistema:

```java
@Service
public class HackerNewsService {

    @Autowired
    private IClientGateway gateway;
    private final ExecutorService executor = Executors.newFixedThreadPool(20); 
    private final RestTemplate restTemplate = new RestTemplate();
    private static final String BASE_URL = "https://hacker-news.firebaseio.com/v0/";

    @Async
    public void fetchTopStoriesMatching(String query) {
        String[] terms = query.toLowerCase().split("\\s+");
        List<String> matchedUrls = new ArrayList<>();

        String[] storyIds = restTemplate.getForObject(BASE_URL + "topstories.json", String[].class);
        if (storyIds == null)
            return;

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < Math.min(storyIds.length, 5000); i++) {
            String id = storyIds[i];
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    Map<String, Object> story = restTemplate.getForObject(BASE_URL + "item/" + id + ".json", Map.class);
                    if (story == null || !story.containsKey("text") || !story.containsKey("url"))
                        return;

                    String title = story.get("text").toString().toLowerCase();
                    for (String term : terms) {
                        if (!title.contains(term))
                            return;
                    }

                    synchronized (matchedUrls) {
                        matchedUrls.add(story.get("url").toString());
                    }
                } catch (Exception e) {
                    System.out.println("Erro ao processar story ID: " + id);
                }
            }, executor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        System.out.println(matchedUrls.size() + " links vindos da query - " + query);
        for (String url : matchedUrls) {
            try {
                gateway.addUrlToQueue(url);
            } catch (Exception e) {
                System.out.println("Erro no hacker news a adicionar à queue");
            }
        }
    }
}
```

Principais características:

* Utiliza `RestTemplate` para fazer requisições HTTP
* Processa assincronamente as histórias usando `CompletableFuture` e um pool de threads
* Filtra histórias com base nos termos de busca
* Adiciona URLs relevantes à fila de indexação via RMI

### 4.2 Serviço OpenAI

O `OpenAIAnalysisService` integra-se com a API REST da OpenAI para gerar resumos dos resultados de busca:

```java
@Service
public class OpenAIAnalysisService {

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
    private String apiKey = StorageUtil.getkey();
    private final SimpMessagingTemplate messagingTemplate;

    public OpenAIAnalysisService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public CompletableFuture<Void> generateAnalysisStreaming(
            String query, List<String> snippets, String queryId) {
        return CompletableFuture.runAsync(() -> {
            try {
                // Monta mensagens no formato Chat
                List<Map<String,String>> messages = new ArrayList<>();
                messages.add(Map.of(
                    "role", "system",
                    "content", "Você é um assistente útil que resume trechos de texto."
                ));
                messages.add(Map.of(
                    "role", "user",
                    "content", buildPrompt(query, snippets)
                ));

                Map<String,Object> body = Map.of(
                    "model",   "gpt-3.5-turbo",
                    "messages", messages,
                    "stream",  true
                );

                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writeValueAsString(body);

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

                StringBuilder acumulador = new StringBuilder();
                // Processa linha a linha (Server-Sent Events)
                client.send(request, HttpResponse.BodyHandlers.ofLines())
                      .body()
                      .forEach(line -> {
                          // Processamento de respostas streaming
                          // ...
                      });

                // Envia texto final
                messagingTemplate.convertAndSend("/topic/analysis/" + queryId, acumulador.toString());

            } catch (Exception e) {
                messagingTemplate
                    .convertAndSend("/topic/analysis/" + queryId,
                                    "Erro ao gerar análise com OpenAI: " + e.getMessage());
            }
        });
    }
}
```

Principais características:

* Utiliza `HttpClient` da Java 11 para fazer requisições HTTP
* Implementa streaming de respostas da OpenAI
* Envia os resultados para os clientes via WebSocket
* Utiliza processamento assíncrono com `CompletableFuture`

## 5. Fluxo da Aplicação

### 5.1 Fluxo de Indexação

1. O usuário submete uma URL para indexação através da interface web
2. O `IndexController` recebe a requisição e encaminha para o gateway RMI
3. O gateway RMI adiciona a URL à fila de indexação
4. O servidor RMI processa a URL e atualiza o índice

### 5.2 Fluxo de Busca

1. O usuário submete uma consulta de busca através da interface web
2. O `SearchController` recebe a requisição e encaminha para o gateway RMI
3. O gateway RMI consulta o índice e retorna os resultados
4. O controlador processa os resultados e renderiza a página de resultados
5. Paralelamente, o serviço OpenAI é chamado para gerar uma análise dos resultados
6. Os resultados da análise são enviados para o cliente via WebSocket

### 5.3 Fluxo de Estatísticas

1. O cliente conecta-se ao endpoint WebSocket
2. O cliente envia uma requisição de estatísticas
3. O `StatisticsWebSocketController` recebe a requisição e consulta o serviço RMI
4. O serviço RMI retorna as estatísticas atuais
5. O controlador envia as estatísticas para o cliente via WebSocket
6. As estatísticas são atualizadas em tempo real na interface do usuário

## 6. Testes da Plataforma

Testes feitos e passados

6.1 Testes Funcionais

* **Indexação de URLs** : Verificar se as URLs são adicionadas corretamente à fila de indexação
* **Busca de Conteúdo** : Verificar se as consultas retornam resultados relevantes
* **Links Relacionados** : Verificar se os links relacionados são exibidos corretamente
* **Estatísticas em Tempo Real** : Verificar se as estatísticas são atualizadas em tempo real
* **Análise de Resultados** : Verificar se as análises de resultados são geradas corretamente

### 6.2 Testes de Integração

* **Integração RMI** : Verificar a comunicação entre o cliente web e o servidor RMI
* **Integração WebSocket** : Verificar a comunicação em tempo real via WebSocket
* **Integração Hacker News** : Verificar a integração com a API do Hacker News
* **Integração OpenAI** : Verificar a integração com a API da OpenAI

## 7. Considerações Finais

O sistema Googol é uma aplicação distribuída que utiliza várias tecnologias para fornecer um serviço de busca web robusto e eficiente. A arquitetura baseada em RMI permite a separação dos componentes de front-end e back-end, enquanto a integração com WebSocket e serviços REST enriquece a experiência do usuário.

Pontos fortes do sistema:

* **Arquitetura Distribuída** : Permite escalabilidade e separação de responsabilidades
* **Integração RMI-WebSocket** : Facilita a atualização em tempo real de dados
* **Processamento Assíncrono** : Melhora a responsividade da interface do usuário
* **Integração com Serviços Externos** : Enriquece o conteúdo e a funcionalidade do sistema
