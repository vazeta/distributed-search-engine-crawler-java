# Guia de Instalação e Execução do Projeto

## Requisitos

- Java Development Kit (JDK) instalado
- Maven instalado (Em linux `sudo apt install maven` ou em windows seguir as instruçoẽs -> `https://maven.apache.org/download.cgi`)
- API key do openAI

## Passos para Compilar e Executar

### Todos os terminais deverão estar na raiz do projeto ou seja em `/proj`

### 2. Compilar o Projeto

No Windows:

```sh
mvn clean compile
```

No Linux:

```sh
mvn clean compile
```

### 3. Configurar o IP para o Server RMI e a API key do openAI

N diretório `proj/data` abrir e criar/alterar o ficheiro `config.properties`

```sh
ip=127.0.0.1
key=your_key
```

No ficheiro de configuração `pom.xml` alterar tambem o ip na linha

```sh
`<jvmArguments>`-Djava.rmi.server.hostname=127.0.0.1 `</jvmArguments>`
```

### 4. Executar o Projeto

Nota-> Executar em terminais distintos e pela ordem abaixo apresentada

No Windows:

```powershell
mvn exec:java "-Dexec.mainClass=search.Gateway"
mvn exec:java "-Dexec.mainClass=search.URLQueueImpl"
mvn exec:java "-Dexec.mainClass=search.Downloader"
mvn exec:java "-Dexec.mainClass=search.Barrel" "-Dexec.args=Barrel1"
mvn spring-boot:run
```

No Linux:

```sh
mvn exec:java -Dexec.mainClass="search.Gateway"
mvn exec:java -Dexec.mainClass="search.URLQueueImpl"
mvn exec:java -Dexec.mainClass="search.Downloader"
mvn exec:java -Dexec.mainClass="search.Barrel" -Dexec.args="Barrel1"
mvn spring-boot:run
```

### 4. Acessar o software

1. Abrir um browser
2. Colocal o ip e a porta (http://127.0.0.1:8080/) na barra de pesquisa
3. O Software irá ser apresentado

### Nota sobre o Sistema Distribuído

Caso o sistema distribuído seja executado em máquinas diferentes, é necessário alterar o endereço IP no ficheiro de configuração tal como dito em cima.

### Como Obter o Endereço IP

- **Windows**:

  - Abrir o terminal (CMD ou PowerShell) e executar:
    ```sh
    ipconfig
    ```
  - O IP está listado em "Endereço IPv4".
- **Linux/macOS**:

  - Abrir o terminal e executar:

    ```sh
    ifconfig
    ```

    ou, se o comando `ifconfig` não estiver disponível:

    ```sh
    ip addr show
    ```
  - O IP está listado junto da interface de rede ativa (ex: `eth0` ou `wlan0`).

Certifique-se de atualizar os ficheiros de configuração com o IP correto antes de iniciar a execução.
