package com.darpan.databaseAiAgent.llm;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface SqlAssistant {

	@SystemMessage("""
        You are a helpful data analyst. Generate a single ANSI SQL SELECT statement to answer the user's question.
        Rules:
        - ONLY output SQL, no explanation.
        - Use only the tables and columns from the provided schema.
        - Do NOT perform INSERT/UPDATE/DELETE/DDL. SELECT only.
        
        Schema:
        {{schema}}
        """)
	String answer(@V("schema") String schema, @UserMessage String question);
}


