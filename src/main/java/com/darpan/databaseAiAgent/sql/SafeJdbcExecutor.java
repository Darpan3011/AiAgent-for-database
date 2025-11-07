package com.darpan.databaseAiAgent.sql;

import com.darpan.databaseAiAgent.api.QueryResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class SafeJdbcExecutor {

    private final JdbcTemplate jdbcTemplate;

    public SafeJdbcExecutor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public QueryResult execute(String sql) {
        String trimmed = sql == null ? "" : sql.trim();
        if (!trimmed.regionMatches(true, 0, "SELECT", 0, 6)) {
            throw new IllegalArgumentException("Only SELECT queries are allowed");
        }

        return jdbcTemplate.query(trimmed, (ResultSetExtractor<QueryResult>) this::extract);
    }

    private QueryResult extract(ResultSet rs) throws SQLException {
        List<String> columns = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();

        ResultSetMetaData md = rs.getMetaData();
        int colCount = md.getColumnCount();
        for (int i = 1; i <= colCount; i++) {
            columns.add(md.getColumnLabel(i));
        }

        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= colCount; i++) {
                row.put(columns.get(i - 1), rs.getObject(i));
            }
            rows.add(row);
        }
        return QueryResult.of(columns, rows);
    }
}