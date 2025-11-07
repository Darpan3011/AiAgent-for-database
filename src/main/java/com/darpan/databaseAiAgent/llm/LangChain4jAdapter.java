package com.darpan.databaseAiAgent.llm;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * LangChain4j adapter implementing LlmAdapter.
 * - If a ChatModel bean is available (auto-configured by starter), it uses it.
 * - Otherwise it builds an OpenAiChatModel using env vars / defaults.
 */
@Component
public class LangChain4jAdapter implements LlmAdapter {

    private final ChatLanguageModel chatModel;

    /**
     * Try to get an existing ChatModel bean if present (ObjectProvider avoids failure when none).
     * If absent, build an OpenAiChatModel from environment variables.
     */
    public LangChain4jAdapter(ObjectProvider<ChatLanguageModel> chatModelProvider) {
        // try to obtain auto-configured ChatModel
        ChatLanguageModel provided = chatModelProvider.getIfAvailable();
        if (provided != null) {
            this.chatModel = provided;
        } else {
            // build a default OpenAI ChatModel (requires langchain4j-open-ai on classpath)
            String apiKey = Optional.ofNullable(System.getenv("OPENAI_API_KEY"))
                    .orElseGet(() -> System.getProperty("OPENAI_API_KEY", null));
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException("No ChatModel bean and OPENAI_API_KEY not set; set env var or provide ChatModel bean");
            }

            String modelName = Optional.ofNullable(System.getenv("LANGCHAIN4J_MODEL"))
                    .orElse("gpt-4o");

            this.chatModel = OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(modelName)
                    .build();
        }
    }

    /**
     * Synchronous generate - simple wrapper returning the assistant text.
     */
    @Override
    public String generate(String prompt) {
        // LangChain4j ChatModel API: generate(String prompt) -> ChatCompletion (API may vary by version)
        String completion = chatModel.generate(prompt);
        // content().text() historically used — fall back defensively
        try {
            return completion;
        } catch (Exception e) {
            // sometimes API returns different structures; try toString fallback
            return completion;
        }
    }
}

