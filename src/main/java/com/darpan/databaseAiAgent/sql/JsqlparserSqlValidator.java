package com.darpan.databaseAiAgent.sql;

import com.darpan.databaseAiAgent.api.DbSchema;
import com.darpan.databaseAiAgent.api.TableSchema;
import com.darpan.databaseAiAgent.api.ValidationResult;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

            // Build table -> columns map (lowercased)
            var tableToColumns = schema.tables().stream().collect(Collectors.toMap(
                    t -> t.name().toLowerCase(),
                    t -> t.columns().stream().map(c -> c.name().toLowerCase()).collect(Collectors.toSet())
            ));

            // Conservative column checks
            // 1) Qualified references table.column where table matches an actual table name
            Set<String> missingColumns = new HashSet<>();
            Pattern qualified = Pattern.compile("(?i)\\b([a-zA-Z_][\\w]*)\\.([a-zA-Z_][\\w]*)\\b");
            Matcher m = qualified.matcher(sql);
            while (m.find()) {
                String left = m.group(1);
                String right = m.group(2);
                String tableLc = left.toLowerCase();
                String colLc = right.toLowerCase();
                if (tableToColumns.containsKey(tableLc)) {
                    Set<String> cols = tableToColumns.get(tableLc);
                    if (!cols.contains(colLc)) {
                        missingColumns.add(left + "." + right);
                    }
                }
            }

            // 2) Single-table simple SELECT: validate unqualified columns against that table
            if (used != null && used.size() == 1) {
                String soleTable = used.get(0);
                String base = soleTable == null ? null : soleTable.replaceAll("^[a-zA-Z0-9_]+\\.", "").toLowerCase();
                Set<String> cols = base == null ? null : tableToColumns.get(base);
                if (cols != null) {
                    String upperSql = sql.toUpperCase();
                    int selIdx = upperSql.indexOf("SELECT");
                    int fromIdx = upperSql.indexOf(" FROM ");
                    if (selIdx >= 0 && fromIdx > selIdx) {
                        String selectList = sql.substring(selIdx + 6, fromIdx);
                        if (!selectList.contains("*")) {
                            for (String item : selectList.split(",")) {
                                String token = item.trim();
                                // remove common wrappers and aliases
                                token = token.replaceAll("(?i) AS \\w+", "");
                                token = token.replaceAll("[()]", " ");
                                token = token.replaceAll("(?i)\\b(CASE|WHEN|THEN|ELSE|END)\\b", " ");
                                token = token.replaceAll("[^a-zA-Z0-9_\\.]", " ");
                                String[] parts = token.split("\\s+");
                                for (String part : parts) {
                                    if (part.isBlank() || part.contains(".")) continue; // skip qualified, already handled
                                    String lc = part.toLowerCase();
                                    if (!lc.isBlank() && Character.isLetter(lc.charAt(0)) && !cols.contains(lc)) {
                                        // allow functions/keywords by requiring presence in column set; otherwise mark missing
                                        missingColumns.add(lc);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!missingColumns.isEmpty()) {
                return ValidationResult.error("Unknown column(s) referenced: " + String.join(", ", missingColumns));
            }

            return ValidationResult.ok();
        } catch (JSQLParserException e) {
            return ValidationResult.error("Invalid SQL: " + e.getMessage());
        }
    }
}