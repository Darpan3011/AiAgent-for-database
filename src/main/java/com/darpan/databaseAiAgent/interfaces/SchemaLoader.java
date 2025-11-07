package com.darpan.databaseAiAgent.interfaces;

import com.darpan.databaseAiAgent.api.DbSchema;

// Loads schema metadata for a datasource
public interface SchemaLoader {
    DbSchema loadSchema();
}
