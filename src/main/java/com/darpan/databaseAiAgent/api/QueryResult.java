package com.darpan.databaseAiAgent.api;

import java.util.List;
import java.util.Map;

public record QueryResult(List<String> columns, List<Map<String, Object>> rows) {
    public static QueryResult of(List<String> columns, List<Map<String, Object>> rows) {
        return new QueryResult(columns, rows);
    }
}