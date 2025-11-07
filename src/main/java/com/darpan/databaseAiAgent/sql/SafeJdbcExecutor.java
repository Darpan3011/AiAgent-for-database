package com.darpan.databaseAiAgent.sql;

import com.darpan.databaseAiAgent.api.QueryResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class SafeJdbcExecutor {

    private final JdbcTemplate jdbc;

    public SafeJdbcExecutor(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public QueryResult execute(String sql) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql);
        if (rows.isEmpty()) return new QueryResult(List.of(), List.of());
        List<String> cols = new ArrayList<>(rows.get(0).keySet());
        List<List<Object>> data = rows.stream()
                .map(r -> cols.stream().map(r::get).toList())
                .toList();
        return new QueryResult(cols, data);
    }
}