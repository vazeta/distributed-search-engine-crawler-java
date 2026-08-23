# Googol - Distributed Web Search Engine

## Project Overview

**Googol** is a distributed web search engine implemented using Java with RMI (Remote Method Invocation) architecture and integrated with Spring Boot for the web interface. The system follows an MVC (Model-View-Controller) pattern with clear separation between front-end and back-end components.

The architecture is designed as a distributed system where multiple "barrels" (search index nodes) store and index web pages, allowing horizontal scaling and fault tolerance.

## Requirements

- **Java Development Kit (JDK)** 17 or higher installed
- **Maven** installed (Linux: `sudo apt install maven`; Windows: follow instructions at https://maven.apache.org/download.cgi)
- **API key** for OpenAI (stored in `data/config.properties`)
- Optional: API key for Hacker News integration (configured automatically)

## Project Structure

The project is organized into the following packages:

### `com.example.googol` - Main Application

- `ServingWebContentApplication.java` - Main Spring Boot application entry point
- `config/` - Configuration classes for RMI, WebSockets, and services
- `controllers/` - Spring MVC controllers handling HTTP requests
- `service/` - Business logic services (Hacker News, OpenAI analysis)

### `search/` - Distributed Search Core

- `Barrel.java` - Search index node storing word-to-URL mappings
- `Gateway.java` - RMI gateway service managing URL queues and barrel registration
- `URLQueue.java` / `URLQueueImpl.java` - Distributed URL queue for indexing
- `IClientGateway.java` - Remote interface for client-gateway communication
- `IBarrelGateway.java` - Remote interface for barrel-client communication
- `Statistics.java` - Statistics data model
- `StatisticsService.java` / `StatisticsServiceImpl.java` - RMI statistics service
- `StorageUtil.java` - Utility for loading/saving data and configuration
- `ReliableMulticastService.java` / `ReliableMulticastServiceImpl.java` - Reliable multicast for cluster communication
- `ReliableMulticastClient.java` / `ReliableMulticastClientImpl.java` - Client interface for multicast
- `StatisticsClient.java` / `StatisticsClientImpl.java` - Statistics client for WebSocket updates

## Architecture

The system uses a **distributed index architecture** with the following key components:

1. **RMI Gateway** (`Gateway`) - Main entry point that registers RMI services and manages URL indexing queue
2. **Multiple Barrels** (`Barrel`) - Distributed search indices that store word-document mappings
3. **Statistics Service** - Tracks search metrics, active barrels, and response times
4. **WebSocket Integration** - Real-time statistics updates to connected clients
5. **External Services** - Hacker News API integration and OpenAI analysis

### Communication Flow

```
Clients → RMI Gateway → Barrels (distributed indices) → Statistics → WebSocket → UI
                                                   ↑
                                           Hacker News / OpenAI APIs
```

## Installation and Execution

### 1. Compile the Project

```bash
# From the project root directory (/proj)
mvn clean compile
```

### 2. Configure the System

Edit the configuration file `data/config.properties`:

```properties
ip=127.0.0.1
key=your_openai_api_key_here
```

**Important:** The `ip` value must match the server's IP address if running on multiple machines. For local single-machine execution, use `127.0.0.1`.

Also, check the `pom.xml` JVM arguments section to ensure the RMI hostname is set correctly:

```xml
<jvmArguments>-Djava.rmi.server.hostname=127.0.0.1</jvmArguments>
```

### 3. Run the System

Execute the components in order across separate terminals:

**Linux:**

```sh
# Terminal 1 - Gateway
mvn exec:java -Dexec.mainClass="search.Gateway"

# Terminal 2 - URL Queue
mvn exec:java -Dexec.mainClass="search.URLQueueImpl"

# Terminal 3 - Downloader (Barrel)
mvn exec:java -Dexec.mainClass="search.Downloader"

# Terminal 4 - Barrel (with name argument)
mvn exec:java -Dexec.mainClass="search.Barrel" -Dexec.args="Barrel1"

# Terminal 5 - Spring Boot Web Application
mvn spring-boot:run
```

**Windows (PowerShell):**

```powershell
# Terminal 1 - Gateway
mvn exec:java "-Dexec.mainClass=search.Gateway"

# Terminal 2 - URL Queue
mvn exec:java "-Dexec.mainClass=search.URLQueueImpl"

# Terminal 3 - Downloader
mvn exec:java "-Dexec.mainClass=search.Downloader"

# Terminal 4 - Barrel
mvn exec:java "-Dexec.mainClass=search.Barrel" "-Dexec.args=Barrel1"

# Terminal 5 - Spring Boot Web Application
mvn spring-boot:run
```

### 4. Access the Software

1. Open a web browser
2. Navigate to `http://127.0.0.1:8080/`
3. The application will be displayed

## Configuration Details

### RMI Configuration

- **Gateway Registry Port:** 1099
- **Multicast Registry Port:** 1097
- **Statistics Registry Port:** 1100
- The RMI hostname is set via JVM argument `-Djava.rmi.server.hostname`

### Config Properties (`data/config.properties`)

| Property | Description                             | Default       |
| -------- | --------------------------------------- | ------------- |
| `ip`   | Server IP address for RMI communication | `127.0.0.1` |
| `key`  | OpenAI API key for result analysis      | (required)    |

### External Service Integration

- **Hacker News API:** Automatically fetches top stories matching search queries
- **OpenAI API:** Generates summaries of search results via GPT-3.5-turbo

## Usage

### Web Interface

Access the application at `http://localhost:8080/` and you'll find:

- **Home Page** (`/`): Main entry point with search functionality
- **Search:** Submit queries to search indexed pages
- **Add URL:** Submit new URLs for indexing via the Hacker News integration or manually
- **Hacker News Integration:** Use `/hn-index` endpoint to fetch and index Hacker News stories matching your query
- **Results:** View search results with snippets and citations

### API Endpoints

| Endpoint                      | Method | Description                                      |
| ----------------------------- | ------ | ------------------------------------------------ |
| `/`                         | GET    | Home page                                        |
| `/search`                   | GET    | Search indexed terms (returns paginated results) |
| `/add-url`                  | POST   | Add a URL to the indexing queue                  |
| `/hn-index`                 | POST   | Index Hacker News stories matching a query       |
| `/related-links`            | GET    | Find pages linking to a specific URL             |
| `/requestStats` (WebSocket) | GET    | Real-time statistics updates                     |

### Command-Line Interface

The system also supports CLI operation:

```bash
# Start a barrel with a specific name
java search.Barrel Barrel1

# Start the gateway
java search.Gateway

# Start the URL queue
java search.URLQueueImpl
```

## Distribution Notes

When running on **multiple machines**:

1. Update the `ip` field in `data/config.properties` on each machine to the machine's actual IP address
2. Ensure all machines can communicate via RMI (firewall rules for ports 1099, 1097, 1100)
3. Each machine should run its own Barrel instance
4. The Gateway should be running on one machine or distributed across machines

## Technical Details

### Technologies Used

- **Spring Boot 2.7.x** - Application framework and web server
- **RMI (Java)** - Remote method invocation for distributed communication
- **WebSocket (STOMP)** - Real-time bi-directional communication
- **Hacker News API** - External content source
- **OpenAI API** - AI-powered result summarization
- **Maven** - Dependency management and build tool
- **Jsoup** - HTML parsing (in dependencies)
- **Jackson** - JSON processing (via Spring Boot)

### Key Design Patterns

- **MVC Pattern** - Separation of concerns between controllers, services, and views
- **Remote Method Invocation** - Distributed component communication
- **Observer Pattern** - Statistics updates via WebSocket
- **Factory Pattern** - Service configuration and creation
- **Async Processing** - Non-blocking operations with CompletableFuture

## Development

### Adding New Features

1. **New Search Index**: Create a new class implementing `IBarrelGateway`
2. **New Controller**: Add a `@Controller` class with appropriate `@RequestMapping`
3. **Service Integration**: Add new services under `com.example.googol.service`
4. **Configuration**: Update `data/config.properties` as needed

### Testing

The project includes unit tests under `src/test/java/`. Run tests with:

```bash
mvn test
```
