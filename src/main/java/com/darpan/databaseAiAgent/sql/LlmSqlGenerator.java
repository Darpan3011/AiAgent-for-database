package com.darpan.databaseAiAgent.sql;

import com.darpan.databaseAiAgent.api.DbSchema;
import com.darpan.databaseAiAgent.api.TableSchema;
import com.darpan.databaseAiAgent.llm.SqlAssistant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.stream.Collectors;

@Component
@SessionScope
@Slf4j
public class LlmSqlGenerator {

    private final SqlAssistant assistant;

    public LlmSqlGenerator(SqlAssistant assistant) {
        this.assistant = assistant;
    }

    public String generateSql(String question, DbSchema schema) {
        String schemaText = schema.tables().stream()
                .map(this::formatTable)
                .collect(Collectors.joining("\n"));

        String sql = assistant.answer(schemaText, question).trim();
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