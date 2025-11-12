package com.darpan.databaseAiAgent.sql;

import com.darpan.databaseAiAgent.api.DbSchema;
import com.darpan.databaseAiAgent.api.TableSchema;
import com.darpan.databaseAiAgent.llm.SqlAssistant;
import com.darpan.databaseAiAgent.prompt.PromptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.stream.Collectors;

@Component
@SessionScope
@Slf4j
public class LlmSqlGenerator {

    private final SqlAssistant assistant;
    private final PromptService promptService;

	public LlmSqlGenerator(SqlAssistant assistant, PromptService promptService) {
        this.assistant = assistant;
        this.promptService = promptService;
    }

    public String generateSql(String question, DbSchema schema) {
        String schemaText = schema.tables().stream()
                .map(this::formatTable)
                .collect(Collectors.joining("\n"));

        String rules = promptService != null ? promptService.getSqlAssistantRules() : "You are a helpful data analyst.\nGenerate a single ANSI SQL SELECT statement to answer the user's question.\nRules:\n- ONLY output SQL, no explanation.\n- Use only the tables and columns from the provided schema.\n- Do NOT perform INSERT/UPDATE/DELETE/DDL. SELECT only.";
        String version = promptService != null ? promptService.getSqlAssistantRulesVersion() : "unknown";
        if (log.isDebugEnabled()) {
            log.debug("Using SQL assistant rules version {}", version);
        }

        String sql = assistant.answer(rules, schemaText, question).trim();
        if (sql.startsWith("```")) {
            sql = sql.replaceAll("```[a-zA-Z]*", "").replace("```", "").trim();
        }
        if (sql.endsWith(";")) {
            sql = sql.substring(0, sql.length() - 1).trim();
        }
        return sql;
    }

    private String formatTable(TableSchema t) {
        String cols = t.columns().stream()
                .map(c -> c.name() + " " + c.type())
                .collect(Collectors.joining(", "));
        StringBuilder sb = new StringBuilder();
        sb.append("- ").append(t.name()).append("(").append(cols).append(")");
        if (t.primaryKeys() != null && !t.primaryKeys().isEmpty()) {
            sb.append("\n  PK(").append(String.join(", ", t.primaryKeys())).append(")");
        }
        if (t.foreignKeys() != null && !t.foreignKeys().isEmpty()) {
            String fkText = t.foreignKeys().stream()
                    .map(f -> f.fromColumn() + " -> " + f.toTable() + "." + f.toColumn())
                    .collect(Collectors.joining(", "));
            sb.append("\n  FK(").append(fkText).append(")");
        }
        return sb.toString();
    }
}