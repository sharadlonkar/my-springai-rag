# Spring AI RAG Demo

A demonstration project showcasing Retrieval Augmented Generation (RAG) implementation using Spring AI and OpenAI's GPT models. This application enables intelligent document querying by combining the power of Large Language Models (LLMs) with local document context.

## Overview

This project demonstrates how to:
- Ingest PDF documents into a vector database
- Perform semantic searches using Spring AI
- Augment LLM responses with relevant document context
- Create an API endpoint for document-aware chat interactions

## Project Requirements

- Java 23
- Maven
- Docker Desktop
- OpenAI API Key
- Dependencies: [Spring Initializer](https://start.spring.io/#!type=maven-project&language=java&platformVersion=3.3.4&packaging=jar&jvmVersion=23&groupId=dev.danvega&artifactId=markets&name=markets&description=Demo%20project%20for%20Spring%20Boot&packageName=dev.danvega.markets&dependencies=web,spring-ai-openai,spring-ai-pdf-document-reader,spring-ai-vectordb-pgvector,docker-compose)

## Dependencies

The project uses the following Spring Boot starters and dependencies:

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-pdf-document-reader</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-pgvector-store-spring-boot-starter</artifactId>
    </dependency>
</dependencies>
```

## Getting Started

1. Configure your environment variables:
2. You can generate Open AI API KEY from https://platform.openai.com/api-keys
```properties
OPENAI_API_KEY=your_api_key_here
```

2. Update `application.properties`:
```properties
spring.ai.openai.api-key=${OPENAI_API_KEY}
spring.ai.openai.chat.model=gpt-4
spring.ai.vectorstore.pgvector.initialize-schema=true
```

3. Place your PDF documents in the `src/main/resources/docs` directory

## Running the Application

1. Start Docker Desktop

2. Launch the application:
```bash
./mvnw spring-boot:run
```

The application will:
- Start a PostgreSQL database with PGVector extension
- Initialize the vector store schema
- Ingest documents from the configured location
- Start a web server on port 8080. This will accept chat input with request param = "prompt"
- Start a command line based chat bot and answers common questions related to USA (Data ingest for USA facts)
## Key Components

### IngestionService

The `IngestionService` handles document processing and vector store population:

```java
@Component
public class IngestionService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    public static final String ANSI_RESET = "\u001B[0m";

    public static final String ANSI_BOLD = "\u001B[1m";

    public static final String ANSI_YELLOW = "\u001B[33m";

    @Value("classpath:/docs/USAFacts.pdf")
    private Resource knowledgePDF;

    public IngestionService(ChatClient.Builder builder, VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        SearchRequest searchRequest = SearchRequest.defaults();

        this.chatClient = builder
                .defaultSystem("You are useful assistant, expert in USA facts. Be friendly")
                .defaultAdvisors(QuestionAnswerAdvisor.builder(this.vectorStore).withSearchRequest(searchRequest)
                        .build()) // Enable RAG
                .build();
    }
}
```

The `IngestionService` starts the command line chat:

```java
 @Override
    public void run(String... args) throws Exception {
        var pdfReader = new PagePdfDocumentReader(knowledgePDF);
        TextSplitter textSplitter = new TokenTextSplitter();
        vectorStore.accept(textSplitter.apply(pdfReader.get()));
        log.info("VectorStore Loaded with data!");
        /*******/
        System.out.println("\nI am your assistant. Ask me obvious questions about USA\n");
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("\n" + ANSI_YELLOW + "USER: " + ANSI_RESET);
                System.out.println("\n" + ANSI_YELLOW + "ASSISTANT: " + ANSI_RESET
                        + chatClient.prompt(scanner.nextLine()) // Get the user input
                        .call()
                        .content());
            }
        }
        /*******/

    }

    
```

Command line based chat bot looks like below:
```
USER: how many states are in america?

ASSISTANT: The United States of America has a total of 50 states. If you have any other questions about the U.S., feel free to ask!

USER: 
```

### ChatController

The `ChatController` provides the REST endpoint for querying documents:

```java
@RestController
public class ChatController {
    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder, VectorStore vectorStore) {
        this.chatClient = builder
                .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))
                .build();
    }

   @GetMapping("/")
   public String chat(@RequestParam(name = "prompt", required = false) String prompt) {
      log.info("Received chat input: text='{}'", prompt);
      prompt = (prompt == null || prompt.isBlank())
              ? "How did the Federal Reserve's recent interest rate cut impact various asset classes according to the analysis?"
              : prompt;

      // Log the final prompt value that will be sent to the model
      log.info("Using prompt: '{}'", prompt);

      return chatClient.prompt()
              .system(SYSTEM_PROMPT)
              .user(prompt)
              .call()
              .content();

   }
}
```

## Making Requests

Query the API using curl or your preferred HTTP client:

```bash
curl http://localhost:8080/
```

The response will include context from your documents along with the LLM's analysis.

## Architecture Highlights

- **Document Processing**: Uses Spring AI's PDF document reader to parse documents into manageable chunks
- **Vector Storage**: Utilizes PGVector for efficient similarity searches
- **Context Retrieval**: Automatically retrieves relevant document segments based on user queries
- **Response Generation**: Combines document context with GPT-4's capabilities for informed responses

## Best Practices

1. **Document Ingestion**
    - Consider implementing checks before reinitializing the vector store
    - Use scheduled tasks for document updates
    - Implement proper error handling for document processing

2. **Query Optimization**
    - Monitor token usage
    - Implement rate limiting
    - Cache frequently requested information

3. **Security**
    - Secure your API endpoints
    - Protect sensitive document content
    - Safely manage API keys