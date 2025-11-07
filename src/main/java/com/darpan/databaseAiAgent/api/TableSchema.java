package com.darpan.databaseAiAgent.api;

import java.util.List;

public record TableSchema(String name, List<ColumnSchema> columns) {
    public TableSchema {
    }
}