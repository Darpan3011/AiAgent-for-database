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

@Component
public class JdbcSchemaLoader {

    private final DataSource dataSource;
    private final String databaseName;

    public JdbcSchemaLoader(DataSource dataSource,
                           @Value("${spring.datasource.name:}") String databaseName) {
        this.dataSource = dataSource;
        this.databaseName = databaseName;
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
                    List<ColumnSchema> columns = new ArrayList<>();
                    try ( ResultSet cols = md.getColumns(catalog, null, tableName, "%")) {
                        while (cols.next()) {
                            columns.add(new ColumnSchema(cols.getString("COLUMN_NAME"), cols.getString("TYPE_NAME")));
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
}