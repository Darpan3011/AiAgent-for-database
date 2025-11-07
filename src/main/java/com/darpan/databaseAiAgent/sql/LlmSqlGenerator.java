package com.darpan.databaseAiAgent.sql;

import com.darpan.databaseAiAgent.api.DbSchema;
import com.darpan.databaseAiAgent.llm.AgentProxy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class LlmSqlGenerator {

    private final AgentProxy agentProxy;

    public LlmSqlGenerator(AgentProxy agentProxy) {
        this.agentProxy = agentProxy;
    }

    /**
     * Build prompt including schema and question, call agentProxy.ask prompt,
     * and return the SQL string (strip semicolon).
     */
    public String generateSql(String question, DbSchema schema, List<String> previousContext) {
        String schemaText = schema.tables().stream()
                .map(t -> t.name() + "(" + t.columns().stream().map(c -> c.name() + " " + c.type()).collect(Collectors.joining(", ")) + ")")
                .collect(Collectors.joining("\n"));

        String prompt = """
                You are a SQL assistant. ONLY output a single SQL SELECT statement and nothing else.
                Schema:
                %s

                Conversation context (for reference):
                %s

                Question:
                %s

                Constraints:
                - Only SELECT statements allowed.
                - Use table/column names exactly as in Schema.
                - If returning many rows, add LIMIT 1000.
                Output the SQL only (no explanation).
                """.formatted(schemaText, String.join("\n", previousContext), question);

        String reply = agentProxy.ask(prompt).trim();
        if (reply.endsWith(";")) reply = reply.substring(0, reply.length() - 1);
        return reply;
    }
}
