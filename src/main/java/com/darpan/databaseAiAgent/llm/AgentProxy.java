package com.darpan.databaseAiAgent.llm;

public interface AgentProxy {
    /**
     * Generic method used to send a textual prompt and receive a textual reply.
     * AiServices will be used to create a runtime proxy that maps this method to the LLM.
     */
    String ask(String prompt);
}