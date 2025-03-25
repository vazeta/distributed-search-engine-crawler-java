# Guia de Instalação e Execução do Projeto

## Requisitos

- Java Development Kit (JDK) instalado
- Biblioteca `jsoup-1.18.3.jar` localizada em `target/lib/`

## Passos para Compilar e Executar

### 1. Navegar até ao Diretório de Código-Fonte

```sh
cd src/main/java
```

### 2. Compilar o Projeto

No Windows:

```sh
javac -d ..\..\..\target\ -cp ..\..\..\target\lib\jsoup-1.18.3.jar search\*.java
```

No Linux:

```sh
javac -d ../../../target/ -cp "../../../target/lib/jsoup-1.18.3.jar" search/*.java
```

### 3. Navegar até ao Diretório de Saída

```sh
cd ../../../target
```

### 4. Executar o Projeto

No Windows:

```sh
java -cp ".;lib/jsoup-1.18.3.jar" search.Client
```

No Linux:

```sh
java -cp ".:lib/jsoup-1.18.3.jar" search.Client
```

A ordem de execucao recomendada é : Gateway -> URLQueueImpl -> Downloader -> Barrel -> Cliente
Esta ordem é flexivel sendo que a Gateway deve ser sempre a primeira a ser executada, visto que é a que cria os servidores RMI que vao ser usados pelas outras estruturas.

## Nota sobre o Sistema Distribuído

Caso o sistema distribuído seja executado em máquinas diferentes, é necessário alterar o endereço IP em alguns ficheiros de configuração.

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
