package com.darpan.databaseAiAgent.llm;


import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Component;

/**
 * Builds per-session AiServices proxy for AgentProxy.
 * This is a simple factory; its buildProxy(...) should be called in a session-scoped component (or injected there).
 */
@Component
public class AiProxyFactory {

    public AgentProxy buildProxy(ChatLanguageModel model, ChatMemory memory) {
        // AiServices will append user/assistant messages automatically in the provided memory.
        return AiServices.builder(AgentProxy.class)
                .chatLanguageModel(model)
                .chatMemory(memory)
                .build();
    }
}

