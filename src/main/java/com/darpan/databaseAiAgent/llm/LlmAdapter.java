package com.darpan.databaseAiAgent.llm;

// LLM adapter used to generate SQL and format responses
public interface LlmAdapter {
    // returns LLM text response
    String generate(String prompt);
    // optionally: structured responses, streaming, or function-calling variants
}

