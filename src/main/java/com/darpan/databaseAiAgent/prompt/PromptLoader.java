package com.darpan.databaseAiAgent.prompt;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Component
public class PromptLoader {

    private static final String DEFAULT_SQL_PROMPT = String.join("\n",
            "You are a helpful data analyst. Generate a single ANSI SQL SELECT statement to answer the user's question.",
            "Rules:",
            "- ONLY output SQL, no explanation.",
            "- Use only the tables and columns from the provided schema.",
            "- Do NOT perform INSERT/UPDATE/DELETE/DDL. SELECT only.",
            "",
            "Schema:",
            "{{SCHEMA}}",
            "",
            "{{CONTEXT}}",
            "Question: {{QUESTION}}",
            "SQL:");

    private volatile String cachedSqlPrompt;

    public String getSqlGeneratorTemplate() {
        if (cachedSqlPrompt != null) return cachedSqlPrompt;
        synchronized (this) {
            if (cachedSqlPrompt != null) return cachedSqlPrompt;
            try {
                ClassPathResource res = new ClassPathResource("prompts/sql_generator_prompt.txt");
                if (!res.exists()) {
                    cachedSqlPrompt = DEFAULT_SQL_PROMPT;
                    return cachedSqlPrompt;
                }
                try (InputStream is = res.getInputStream();
                     BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    cachedSqlPrompt = br.lines().collect(Collectors.joining("\n"));
                    if (cachedSqlPrompt == null || cachedSqlPrompt.isBlank()) {
                        cachedSqlPrompt = DEFAULT_SQL_PROMPT;
                    }
                    return cachedSqlPrompt;
                }
            } catch (IOException e) {
                cachedSqlPrompt = DEFAULT_SQL_PROMPT;
                return cachedSqlPrompt;
            }
        }
    }
}
