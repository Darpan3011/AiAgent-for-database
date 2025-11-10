package com.darpan.databaseAiAgent.api;

import java.util.List;

public record TableSchema(String name,
                          List<ColumnSchema> columns,
                          List<String> primaryKeys,
                          List<ForeignKey> foreignKeys) {
    public TableSchema {
    }
}