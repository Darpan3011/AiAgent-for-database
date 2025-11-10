package com.darpan.databaseAiAgent.api;

public record ForeignKey(String fromColumn, String toTable, String toColumn) {
    public static ForeignKey of(String fromColumn, String toTable, String toColumn) {
        return new ForeignKey(fromColumn, toTable, toColumn);
    }
}