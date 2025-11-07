package com.darpan.databaseAiAgent.interfaces;

import com.darpan.databaseAiAgent.api.DbSchema;
import com.darpan.databaseAiAgent.api.ValidationResult;

// Validates SQL (only SELECT, allowed tables, row limit)
public interface SqlValidator {
    ValidationResult validate(String sql, DbSchema schema);
}
