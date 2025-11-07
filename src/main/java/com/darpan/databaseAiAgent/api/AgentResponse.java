package com.darpan.databaseAiAgent.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AgentResponse {
    private final boolean ok;
    private final String answer;      // natural language answer
    private final String sql;         // generated SQL (if ok)
    private final QueryResult rows;   // query result (if ok)
    private final String error;       // error message (if !ok)

    public static AgentResponse ok(String answer, String sql, QueryResult rows) {
        return new AgentResponse(true, answer, sql, rows, null);
    }

    public static AgentResponse error(String errorMessage) {
        return new AgentResponse(false, null, null, null, errorMessage);
    }
}