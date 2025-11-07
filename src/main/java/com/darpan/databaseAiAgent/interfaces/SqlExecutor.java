package com.darpan.databaseAiAgent.interfaces;

import com.darpan.databaseAiAgent.api.QueryResult;

// Executes validated SQL (read-only)
public interface SqlExecutor {
    QueryResult execute(String sql);
}
