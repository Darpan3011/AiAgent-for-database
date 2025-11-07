package com.darpan.databaseAiAgent.sql;

import com.darpan.databaseAiAgent.api.DbSchema;
import com.darpan.databaseAiAgent.api.TableSchema;
import com.darpan.databaseAiAgent.api.ValidationResult;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JsqlparserSqlValidator {

    // For simplicity, you can read whitelist from properties or constructor injection.
    private final Set<String> whitelistedTables = Set.of(); // empty = allow all schema tables

    public ValidationResult validate(String sql, DbSchema schema) {
        try {
            Statement stmt = CCJSqlParserUtil.parse(sql);
            if (!(stmt instanceof Select)) {
                return new ValidationResult(false, "Only SELECT statements allowed");
            }
            TablesNamesFinder finder = new TablesNamesFinder();
            List<String> tables = finder.getTableList((Statement) stmt);
            Set<String> schemaTables = schema.tables().stream().map(TableSchema::name).map(String::toLowerCase).collect(Collectors.toSet());
            for (String t : tables) {
                if (!schemaTables.contains(t.toLowerCase())) {
                    return new ValidationResult(false, "Unknown table: " + t);
                }
                if (!whitelistedTables.isEmpty() && !whitelistedTables.contains(t.toLowerCase())) {
                    return new ValidationResult(false, "Table not allowed: " + t);
                }
            }
            return new ValidationResult(true, "ok");
        } catch (Exception e) {
            return new ValidationResult(false, "SQL parse error: " + e.getMessage());
        }
    }
}