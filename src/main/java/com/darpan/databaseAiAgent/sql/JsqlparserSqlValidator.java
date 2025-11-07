package com.darpan.databaseAiAgent.sql;

import com.darpan.databaseAiAgent.api.DbSchema;
import com.darpan.databaseAiAgent.api.TableSchema;
import com.darpan.databaseAiAgent.api.ValidationResult;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JsqlparserSqlValidator {

    public ValidationResult validate(String sql, DbSchema schema) {
        if (sql == null || sql.isBlank()) {
            return ValidationResult.error("Empty SQL generated");
        }
        try {
            Statement stmt = CCJSqlParserUtil.parse(sql);
            if (!(stmt instanceof Select select)) {
                return ValidationResult.error("Only SELECT statements are allowed");
            }

            // Validate referenced tables exist in schema
            Set<String> allowedTables = schema.tables().stream()
                    .map(TableSchema::name)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());

            TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
            List<String> used = tablesNamesFinder.getTableList((Statement) select);
            Set<String> missing = new HashSet<>();
            for (String t : used) {
                String norm = t == null ? null : t.replaceAll("^[a-zA-Z0-9_]+\\.", "").toLowerCase();
                if (norm != null && !allowedTables.contains(norm)) {
                    missing.add(t);
                }
            }
            if (!missing.isEmpty()) {
                return ValidationResult.error("Unknown table(s) referenced: " + String.join(", ", missing));
            }

            return ValidationResult.ok();
        } catch (JSQLParserException e) {
            return ValidationResult.error("Invalid SQL: " + e.getMessage());
        }
    }
}