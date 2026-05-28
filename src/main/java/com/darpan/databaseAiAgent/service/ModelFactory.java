package com.darpan.databaseAiAgent.service;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ModelFactory {

    @Value("${langchain4j.openai.api-key:thisisthedummykey}")
    private String openAiApiKey;

    @Value("${langchain4j.openai.chat-model.model-name:gpt-4o}")
    private String defaultOpenAiModelName;

    @Value("${ai.db.agent.model-provider:openai}")
    private String defaultModelProvider;

    @Value("${langchain4j.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${langchain4j.ollama.chat-model.model-name:llama3.2}")
    private String defaultOllamaModelName;

    @Value("${langchain4j.google-ai-gemini.api-key:}")
    private String geminiApiKey;

    @Value("${langchain4j.google-ai-gemini.chat-model.model-name:gemini-2.0-flash-001}")
    private String defaultGeminiModelName;

    public ChatModel createChatModel(String modelName) {
        String provider = Optional.ofNullable(defaultModelProvider)
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .orElse("openai");

        if ("ollama".equals(provider)) {
            return createOllamaChatModel(modelName);
        }

        if ("openai".equals(provider)) {
            return createOpenAiChatModel(modelName);
        }

        if ("gemini".equals(provider)) {
            return createGeminiChatModel(modelName);
        }

        throw new IllegalStateException(
                "Unsupported AI provider '%s'. Supported values are 'openai', 'ollama', or 'gemini'.".formatted(provider));
    }
    
    public String getDefaultProvider() {
        return defaultModelProvider;
    }

    private ChatModel createOpenAiChatModel(String modelName) {
        String apiKey = Optional.ofNullable(openAiApiKey)
                .filter(key -> !key.isEmpty())
                .orElseThrow(() -> new IllegalStateException("OpenAI API key not found. Please set 'langchain4j.openai.api-key' in application.properties or 'OPENAI_API_KEY' environment variable."));

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName != null && !modelName.isEmpty() ? modelName : defaultOpenAiModelName)
                .build();
    }

    private ChatModel createOllamaChatModel(String modelName) {
        return OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(modelName != null && !modelName.isEmpty() ? modelName : defaultOllamaModelName)
                .build();
    }

    private ChatModel createGeminiChatModel(String modelName) {
        String apiKey = Optional.ofNullable(geminiApiKey)
                .filter(key -> !key.isEmpty())
                .orElseThrow(() -> new IllegalStateException("Gemini API key not found. Please set 'langchain4j.google-ai-gemini.api-key' in application.properties or environment variable."));

        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName != null && !modelName.isEmpty() ? modelName : defaultGeminiModelName)
                .build();
    }
}
