package com.darpan.databaseAiAgent.sql;

import com.darpan.databaseAiAgent.api.DbSchema;
import com.darpan.databaseAiAgent.api.TableSchema;
import com.darpan.databaseAiAgent.prompt.PromptLoader;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.List;
import java.util.stream.Collectors;

@Component
@SessionScope
@Slf4j
public class LlmSqlGenerator {

    private final ChatLanguageModel model;
    private final ChatMemory chatMemory;
    private final PromptLoader promptLoader;

    public LlmSqlGenerator(ChatLanguageModel model, ChatMemory chatMemory, PromptLoader promptLoader) {
        this.model = model;
        this.chatMemory = chatMemory;
        this.promptLoader = promptLoader;
    }

    public String generateSql(String question, DbSchema schema, List<String> chatContext) {
        String prompt = renderPrompt(question, schema, chatContext);

        String sql = model.generate(prompt).trim();
        if (sql.startsWith("```")) {
            sql = sql.replaceAll("```[a-zA-Z]*", "").replace("```", "").trim();
        }
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }
        return sql;
    }
    private String renderPrompt(String question, DbSchema schema, List<String> chatContext) {
        String schemaText = schema.tables().stream()
                .map(this::formatTable)
                .collect(Collectors.joining("\n"));

        String ctxBlock = (chatContext == null || chatContext.isEmpty())
                ? ""
                : ("Conversation context:\n" + String.join("\n", chatContext) + "\n");

        String template = promptLoader.getSqlGeneratorTemplate();
        template = template
                .replace("{{SCHEMA}}", schemaText)
                .replace("{{CONTEXT}}", ctxBlock)
                .replace("{{QUESTION}}", question);
        log.error("Final prompt {}", template);
        return template;
    }

    private String formatTable(TableSchema t) {
        String cols = t.columns().stream()
                .map(c -> c.name() + " " + c.type())
                .collect(Collectors.joining(", "));
        return "- " + t.name() + "(" + cols + ")";
    }
}