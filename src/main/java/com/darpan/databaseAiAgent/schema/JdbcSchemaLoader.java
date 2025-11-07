package com.darpan.databaseAiAgent.schema;

import com.darpan.databaseAiAgent.api.DbSchema;
import com.darpan.databaseAiAgent.api.TableSchema;
import com.darpan.databaseAiAgent.api.ColumnSchema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class JdbcSchemaLoader {

    private final DataSource dataSource;
    private final String databaseName;
    private final Set<String> excludedTables;          // lower-cased table names
    private final Set<String> excludedColumnSpecs;     // patterns: table.column or *.column, all lower-cased

    public JdbcSchemaLoader(DataSource dataSource,
                            @Value("${spring.datasource.name:}") String databaseName,
                            @Value("${ai.db.schema.exclude.tables:}") String excludeTablesCsv,
                            @Value("${ai.db.schema.exclude.columns:}") String excludeColumnsCsv) {
        this.dataSource = dataSource;
        this.databaseName = databaseName;
        this.excludedTables = splitCsvLower(excludeTablesCsv);
        this.excludedColumnSpecs = splitCsvLower(excludeColumnsCsv);
    }

    public DbSchema loadSchema() {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData md = conn.getMetaData();

            // If database name is not specified, use the current catalog
            String catalog = databaseName != null && !databaseName.isEmpty()
                    ? databaseName
                    : conn.getCatalog();

            try ( ResultSet tablesRs = md.getTables(catalog, null, "%", new String[]{"TABLE", "VIEW"})) {
                List<TableSchema> tables = new ArrayList<>();
                while (tablesRs.next()) {
                    String tableName = tablesRs.getString("TABLE_NAME");
                    if (shouldSkipTable(tableName)) {
                        continue;
                    }
                    List<ColumnSchema> columns = new ArrayList<>();
                    try ( ResultSet cols = md.getColumns(catalog, null, tableName, "%")) {
                        while (cols.next()) {
                            String columnName = cols.getString("COLUMN_NAME");
                            if (shouldSkipColumn(tableName, columnName)) {
                                continue;
                            }
                            columns.add(new ColumnSchema(columnName, cols.getString("TYPE_NAME")));
                        }
                    }
                    tables.add(new TableSchema(tableName, columns));
                }
                return new DbSchema(tables);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load DB schema", e);
        }
    }

    private boolean shouldSkipTable(String tableName) {
        if (tableName == null) return false;
        return excludedTables.contains(tableName.toLowerCase(Locale.ROOT));
    }

    private boolean shouldSkipColumn(String tableName, String columnName) {
        if (tableName == null || columnName == null) return false;
        String t = tableName.toLowerCase(Locale.ROOT);
        String c = columnName.toLowerCase(Locale.ROOT);
        return excludedColumnSpecs.contains(t + "." + c) || excludedColumnSpecs.contains("*." + c);
    }

    private static Set<String> splitCsvLower(String csv) {
        if (csv == null || csv.isBlank()) return Set.of();
        return List.of(csv.split(","))
                .stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }
}