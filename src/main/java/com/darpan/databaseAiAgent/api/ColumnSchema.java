package com.darpan.databaseAiAgent.api;

public record ColumnSchema(String name, String type) {
    public static ColumnSchema of(String name, String type) {
        return new ColumnSchema(name, type);
    }
}