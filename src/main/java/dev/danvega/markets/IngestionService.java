package dev.danvega.markets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
public class IngestionService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    public static final String ANSI_RESET = "\u001B[0m";

    public static final String ANSI_YELLOW = "\u001B[33m";

    //@Value("classpath:/docs/article_thebeatoct2024.pdf")
    @Value("classpath:/docs/USAFacts.pdf")
    private Resource knowledgePDF;

    /**
     * Constructs an IngestionService with a configured ChatClient and VectorStore.
     * Initializes the service by configuring a ChatClient with a default system prompt
     * for USA facts assistance and enabling Retrieval-Augmented Generation (RAG) through
     * a QuestionAnswerAdvisor. The advisor uses the provided VectorStore with default
     * search settings to retrieve relevant context for answering user queries.
     *
     * @param builder the ChatClient builder used to configure and create the chat client instance
     * @param vectorStore the vector store repository that contains embedded document data for RAG operations
     */
    public IngestionService(ChatClient.Builder builder, VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        SearchRequest searchRequest = SearchRequest.defaults();

        this.chatClient = builder
                .defaultSystem("You are useful assistant, expert in USA facts. Be friendly")
                .defaultAdvisors(QuestionAnswerAdvisor.builder(this.vectorStore).withSearchRequest(searchRequest)
                        .build()) // Enable RAG
                .build();
    }

    /**
     * Executes the command line runner to load PDF documents into the vector store and start an interactive chat session.
     * This method reads a PDF document, splits its text into chunks using a token-based text splitter, and loads
     * the chunks into the vector store for retrieval-augmented generation. After loading the data, it starts an
     * interactive console-based chat loop where users can ask questions about USA facts. The assistant responds
     * using the chat client configured with RAG capabilities, retrieving relevant context from the vector store
     * to answer user queries. The chat loop continues indefinitely until the application is terminated.
     *
     * @param args the command line arguments passed to the application
     * @throws Exception if an error occurs during PDF reading, vector store loading, or chat interaction
     */
    @Override
    public void run(String... args)  {
        var pdfReader = new PagePdfDocumentReader(knowledgePDF);
        TextSplitter textSplitter = new TokenTextSplitter();
        vectorStore.accept(textSplitter.apply(pdfReader.get()));
        log.info("VectorStore Loaded with data!");
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
    }
}
