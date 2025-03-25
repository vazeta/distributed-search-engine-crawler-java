# **Relatório Projeto \- Meta 1- Sistemas Distribuídos**

**Licenciatura Engenharia informática 2024/25**
**João António Faustino Vaz 2022231087**
**João Maria Moreira Dias 2022225061**

# **Introdução**

O presente relatório explica o desenvolvimento da primeira meta do projeto Googol, um motor de pesquisa de páginas Web. O software foi projetado para realizar a indexação automática de páginas através dos Downloaders e permitir a busca eficiente de informações usando um índice invertido.O objetivo principal desta fase do projeto foi a criação de um sistema distribuído baseado em RPC/RMI, garantindo a disponibilidade e consistência dos dados por meio de replicação e reliable multicast. A implementação envolveu a conceção de uma arquitetura cliente-servidor com múltiplos componentes, incluindo Downloaders, Storage Barrels, uma Gateway RPC/RMI e cliente. Este documento detalha a abordagem adotada para a implementação, a estrutura da solução, as decisões técnicas tomadas, bem como os desafios enfrentados e as soluções aplicadas.

# Arquitetura do Sistema de Busca Distribuído - Relatório Detalhado

## 1. Visão Geral da Arquitetura

A nossa arquitetura é projetada para ser escalável, distribuída e tolerante a falhas, e tem os seguintes componentes principais:

* Gateway
* URL Queue
* Downloader
* Barrels (Indexadores)
* Cliente

## 2. Padrões de Comunicação

### 2.1 Remote Method Invocation (RMI)

* Utiliza RMI para comunicação distribuída
* Portas de comunicação:
  * Porta 1099: Registro principal de serviços
  * Porta 1097: Serviços de multicast confiável, utilizaso essencialmente pelos Downloaders e Barrels

### 2.2 Reliable Multicast

* Implementação personalizada de envio de mensagens entre componentes
* Garante entrega de mensagens entre Downloader e Barrels

## 3. Componentes do Sistema

### 3.1 Gateway (Gateway.java)

**Responsabilidades:**

* Ponto central de coordenação
* Gerenciamento de fila de URLs
* Roteamento de solicitações de pesquisa
* Agregação de estatísticas

**Características Principais:**

* Implementa `IClientGateway`
* Registro dinâmico de Barrels
* Balanceamento de carga entre Barrels
* Rastreamento de estatísticas de pesquisa

### 3.2 URL Queue (URLQueueImpl.java)

**Responsabilidades:**

* Gerenciamento de fila de URLs a serem processadas
* Controle de URLs únicas
* Sincronização de acesso à fila

**Características Principais:**

* Fila thread-safe (`ConcurrentLinkedQueue`)
* Mecanismo de espera e notificação
* Prevenção de processamento de URLs duplicadas

### 3.3 Downloader (Downloader.java)

**Responsabilidades:**

* Baixar e processar páginas web
* Extração de links
* Indexação de conteúdo

**Características Principais:**

* Múltiplas threads de download
* Processamento de palavras
* Filtro de stop words
* Envio de dados para indexação via multicast

### 3.4 Barrels (Barrel.java)

**Responsabilidades:**

* Indexação de documentos
* Armazenamento de índices invertidos
* Processamento de consultas

**Características Principais:**

* Persistência de índices em disco
* Suporte a pesquisas com múltiplas palavras
* Ranqueamento de resultados baseado em links

### 3.5 Cliente (Client.java)

**Responsabilidades:**

* Interface de interação com o usuário
* Envio de URLs para indexação
* Realização de pesquisas

**Características Principais:**

* Menu interativo
* Indexação de novos URLs

## 4. Detalhes sobre o funcionamento da componente de replicação do índice

O algoritmo implementado no `ReliableMulticastServiceImpl` possui as seguintes características:

1. Registo de Clientes (Barrels)

* Método `registerClient()` permite que componentes (Barrels) se registem no serviço multicast
* Mantém um mapa `clients` com todos os Barrels registrados

2. Mecanismo de Envio Confiável

* Método `sendReliableMessage()` usa uma estratégia de envio com validação de disponibilidade

3. Verificação de Disponibilidade

* Método `todosClientesDisponiveis()` testa a conexão com cada cliente através do método `ping()`
* Se algum cliente não estiver disponível, o envio é interrompido

4. Tratamento de Mensagens Pendentes

* Fila `mensagensPendentes` armazena mensagens que não puderam ser enviadas(enquanto existem barrels indisponíveis)
* Iniciamos uma Thread de monitoramento (`iniciarMonitoramento()`) tenta reenviar mensagens pendentes a cada 5 segundos

5.  Recuperaçao dos dados nos barrels

* Os Barrels quando são desligados guardas as suas estruturas `index` e `linkCorr` em ficheiros .obj
* Deste modo quando estes barrels são ligados, recuperam rapidamente essa informação através da leituar desses ficheiros 


Deste modo garantimos assim a integridade dos barrels e que em ambos existe informação idêntica. Ao longo do desenvolvimento percebemos que caso encerrássemos um dos barrels mesmo que no método `sendReliableMessage()`  se verifique a disponibilidade existe uma gap entre o tempo que paramos um barrel e o tempo que o downloader sabe disso e portanto "perdíamos" algumas mensagens. Desse modo tivemos de ainda implementar um buffer que guarda as ultimas 200 mensagens e que portanto na reativação do barrel que estava down garante que a informação é consistente entre os barrels.

## 5. Componente RMI e callbacks 

O Software utiliza Java RMI (Remote Method Invocation) para implementar comunicação distribuída entre diferentes componentes:
- Gateway
- Barrels (indexadores)
- Cliente
- Serviço de Estatísticas
- Serviço de Multicast Confiável
- Fila de URLs

### Interfaces Remotas Principais

1. **IClientGateway**
   - Métodos remotos:
     - `addUrlToQueue(String url)`: Adiciona URL à fila de processamento
     - `request_index(String word, int page)`: Pesquisa palavras nos Barrels
     - `request_url_related(String link)`: Busca links relacionados a uma URL

2. **IBarrelGateway**
   - Métodos remotos:
     - `search(String word, int page)`: Busca indexada em um Barrel específico
     - `related_links(String link)`: Obtém links que apontam para uma URL
     - `getIndexSize()`: Retorna tamanho do índice do Barrel

3. **ReliableMulticastService**
   - Métodos remotos:
     - `sendReliableMessage(String message)`: Envia mensagem para todos os clientes
     - `registerClient(ReliableMulticastClient client, String name)`: Registra cliente para receber mensagens

## Mecanismos de Failover

### 1. Descoberta e Seleção Dinâmica de Barrels

No método `request_index()` do Gateway, há uma estratégia robusta de failover:

```java
while (true) {
    barrels = registry.list();
    if (barrels.length == 0) {
        Thread.sleep(2000);
    }

    List<String> filteredBarrels = new ArrayList<>();
    for (String barrel : barrels) {
        if (barrel.startsWith("Barrel")) {
            filteredBarrels.add(barrel);
        }
    }

    for (String selectedBarrel : filteredBarrels) {
        try {
            IBarrelGateway barrel = (IBarrelGateway) registry.lookup(selectedBarrel);
            results = barrel.search(word, page);
            break;
        } catch (Exception e) {
            System.out.println("Erro ao conectar ao Barrel " + selectedBarrel);
        }
    }

    if (results != null) break;
    Thread.sleep(2000);
}
```

#### Características do Failover:
- Ciclo de tentativas contínuas
- Filtragem de Barrels ativos
- Tentativa sequencial de conexão
- Espera entre tentativas para evitar sobrecarga

### 2. Multicast Confiável com Recuperação

No `ReliableMulticastServiceImpl`, implementa-se um mecanismo de entrega confiável:

```java
private void reenviarMensagensPendentes() {
    if (!todosClientesDisponiveis()) return;

    while (!mensagensPendentes.isEmpty()) {
        String mensagem = mensagensPendentes.poll();
        enviarParaTodos(mensagem);
    }
}
```

#### Características:
- Fila de mensagens pendentes
- Verificação de disponibilidade dos clientes
- Reenvio automático de mensagens não entregues
- Thread de monitoramento a cada 5 segundos

### 3. Callback de Estatísticas

O serviço de estatísticas usa um mecanismo de callback robusto:

```java
public void notifyStats(Statistics stats) {
    Iterator<StatisticsClient> iterator = subscribers.iterator();
    while (iterator.hasNext()) {
        try {
            StatisticsClient client = iterator.next();
            client.updateStats(stats);
        } catch (RemoteException e) {
            iterator.remove();
        }
    }
}
```

#### Características:
- Notificação automática de múltiplos clientes
- Remoção dinâmica de clientes inacessíveis
- Tolerância a falhas na comunicação


