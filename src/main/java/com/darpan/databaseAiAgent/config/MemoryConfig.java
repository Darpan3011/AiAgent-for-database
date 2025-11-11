package com.darpan.databaseAiAgent.config;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
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
    public OpenAiChatModel chatLanguageModel() {
        String apiKey = Optional.ofNullable(openAiApiKey)
                .filter(key -> !key.isEmpty())
                .orElseThrow(() -> new IllegalStateException("OpenAI API key not found. Please set 'langchain4j.openai.api-key' in application.properties or 'OPENAI_API_KEY' environment variable."));

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }
}
