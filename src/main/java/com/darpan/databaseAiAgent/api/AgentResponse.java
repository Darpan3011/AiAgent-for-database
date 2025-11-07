package com.darpan.databaseAiAgent.api;

public record AgentResponse(boolean success, String answer, String sql, QueryResult rows, String error) {
    public static AgentResponse ok(String answer, String sql, QueryResult rows) { return new AgentResponse(true, answer, sql, rows, null); }
    public static AgentResponse error(String error) { return new AgentResponse(false, null, null, null, error); }
}

