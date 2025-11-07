package com.darpan.databaseAiAgent.config;

import com.darpan.databaseAiAgent.llm.AgentProxy;
import com.darpan.databaseAiAgent.llm.AiProxyFactory;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.context.annotation.SessionScope;

import java.util.Optional;

@Configuration
public class MemoryConfig {

    @Value("${langchain4j.openai.api-key:}")
    private String openAiApiKey;

    @Value("${langchain4j.openai.chat-model.model-name:gpt-4o}")
    private String modelName;

    @Bean
    @SessionScope
    public ChatMemory chatMemory(@Value("${ai.db.agent.memory.max-messages:30}") int maxMessages) {
        return MessageWindowChatMemory.withMaxMessages(maxMessages);
    }

    @Bean
    @Lazy
    public ChatLanguageModel chatLanguageModel() {
        String apiKey = Optional.ofNullable(openAiApiKey)
                .filter(key -> !key.isEmpty())
                .or(() -> Optional.ofNullable(System.getenv("OPENAI_API_KEY")))
                .orElseThrow(() -> new IllegalStateException("OpenAI API key not found. Please set 'langchain4j.openai.api-key' in application.properties or 'OPENAI_API_KEY' environment variable."));

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }

    @Bean
    @SessionScope
    public AgentProxy agentProxy(ChatLanguageModel model, ChatMemory sessionChatMemory, AiProxyFactory factory) {
        return factory.buildProxy(model, sessionChatMemory);
    }
}
