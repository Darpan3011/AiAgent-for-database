package com.darpan.databaseAiAgent.format;

import com.darpan.databaseAiAgent.api.QueryResult;
import com.darpan.databaseAiAgent.llm.AgentProxy;
import org.springframework.stereotype.Component;

@Component
public class LlmResultFormatter {

    private final AgentProxy agentProxy;

    public LlmResultFormatter(AgentProxy agentProxy) {
        this.agentProxy = agentProxy;
    }

    public String format(String userQuestion, String sql, QueryResult result) {
        String cols = String.join(", ", result.columns());
        String rowsText = result.rows().stream()
                .map(r -> r.stream().map(o -> o == null ? "null" : o.toString()).reduce((a, b) -> a + ", " + b).orElse(""))
                .reduce((a, b) -> a + "\n" + b).orElse("");

        String prompt = """
                You are a helpful assistant. Convert the SQL result into a concise natural language answer.
                User question: %s

                Executed SQL: %s

                Columns: %s

                Rows:
                %s

                Output only the answer in one or two sentences.
                """.formatted(userQuestion, sql, cols, rowsText);

        return agentProxy.ask(prompt).trim();
    }
}