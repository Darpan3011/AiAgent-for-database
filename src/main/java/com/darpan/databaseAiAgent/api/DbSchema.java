package com.darpan.databaseAiAgent.api;

import java.util.List;

public record DbSchema(List<TableSchema> tables) {}