package com.darpan.databaseAiAgent.config;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import com.darpan.databaseAiAgent.service.ModelFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.context.annotation.SessionScope;

@Configuration
public class MemoryConfig {

    private final ModelFactory modelFactory;

    public MemoryConfig(ModelFactory modelFactory) {
        this.modelFactory = modelFactory;
    }

    @Bean
    @SessionScope
    public ChatMemory chatMemory(@Value("${ai.db.agent.memory.max-messages:30}") int maxMessages) {
        return MessageWindowChatMemory.withMaxMessages(maxMessages);
    }

    @Bean
    @Lazy
    public ChatModel chatLanguageModel() {
        return modelFactory.createChatModel(null);
    }
}
