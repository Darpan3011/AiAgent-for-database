package com.darpan.databaseAiAgent.config;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.context.annotation.SessionScope;

import java.util.Optional;

@Configuration
public class MemoryConfig {

    @Value("${langchain4j.openai.api-key:thisisthedummykey}")
    private String openAiApiKey;

    @Value("${langchain4j.openai.chat-model.model-name:gpt-4o}")
    private String modelName;

    @Value("${ai.db.agent.model-provider:openai}")
    private String modelProvider;

    @Value("${langchain4j.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${langchain4j.ollama.chat-model.model-name:llama3.2}")
    private String ollamaModelName;

    @Value("${langchain4j.google-ai-gemini.api-key:}")
    private String geminiApiKey;

    @Value("${langchain4j.google-ai-gemini.chat-model.model-name:gemini-2.0-flash-001}")
    private String geminiModelName;

    @Bean
    @SessionScope
    public ChatMemory chatMemory(@Value("${ai.db.agent.memory.max-messages:30}") int maxMessages) {
        return MessageWindowChatMemory.withMaxMessages(maxMessages);
    }

    @Bean
    @Lazy
    public ChatModel chatLanguageModel() {
        String provider = Optional.ofNullable(modelProvider)
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .orElse("openai");

        if ("ollama".equals(provider)) {
            return createOllamaChatModel();
        }

        if ("openai".equals(provider)) {
            return createOpenAiChatModel();
        }

        if ("gemini".equals(provider)) {
            return createGeminiChatModel();
        }

        throw new IllegalStateException(
                "Unsupported AI provider '%s'. Supported values are 'openai', 'ollama', or 'gemini'.".formatted(modelProvider));
    }

    private ChatModel createOpenAiChatModel() {
        String apiKey = Optional.ofNullable(openAiApiKey)
                .filter(key -> !key.isEmpty())
                .orElseThrow(() -> new IllegalStateException("OpenAI API key not found. Please set 'langchain4j.openai.api-key' in application.properties or 'OPENAI_API_KEY' environment variable."));

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }

    private ChatModel createOllamaChatModel() {
        return OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(ollamaModelName)
                .build();
    }

    private ChatModel createGeminiChatModel() {
        String apiKey = Optional.ofNullable(geminiApiKey)
                .filter(key -> !key.isEmpty())
                .orElseThrow(() -> new IllegalStateException("Gemini API key not found. Please set 'langchain4j.google-ai-gemini.api-key' in application.properties or environment variable."));

        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(geminiModelName)
                .build();
    }
}
