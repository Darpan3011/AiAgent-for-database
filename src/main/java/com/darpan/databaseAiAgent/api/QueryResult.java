package com.darpan.databaseAiAgent.api;

import java.util.List;

public record QueryResult(List<String> columns, List<List<Object>> rows) {}
