package dev.danvega.markets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.regex.Pattern;

@RestController
public class ChatController {

    private final ChatClient chatClient;
    private static final String SYSTEM_PROMPT = "You are a helpful financial research assistant. Use only the retrieved context to answer. If the answer isn't in the context, say you don't know. Be concise and clear.";
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

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
