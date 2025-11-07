package com.darpan.databaseAiAgent.interfaces;

import com.darpan.databaseAiAgent.api.QueryResult;

// Formats raw query result into natural language via LLM or template
public interface ResultFormatter {
    String format(String userQuestion, String sql, QueryResult result);
}
