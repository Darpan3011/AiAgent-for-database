package com.darpan.databaseAiAgent.sql;

import com.darpan.databaseAiAgent.api.DbSchema;
import com.darpan.databaseAiAgent.api.TableSchema;
import com.darpan.databaseAiAgent.llm.SqlAssistant;
import com.darpan.databaseAiAgent.prompt.PromptService;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@SessionScope
@Slf4j
public class LlmSqlGenerator {

    private static final Pattern FENCED_BACKTICK_CAPTURE = Pattern.compile("(?is)```[a-zA-Z]*\\s*([\\s\\S]*?)\\s*```");
    private static final Pattern FENCED_TILDE_CAPTURE = Pattern.compile("(?is)~~~[a-zA-Z]*\\s*([\\s\\S]*?)\\s*~~~");
    private static final Pattern INVISIBLE_CHARS = Pattern.compile("[\\uFEFF\\u200B-\\u200F\\u00A0]");
    private static final Pattern SELECT_BLOCK = Pattern.compile("(?is).*?(SELECT\\b[\\s\\S]*?)(;|$)");

    private final SqlAssistant assistant;
    private final PromptService promptService;

	public LlmSqlGenerator(SqlAssistant assistant, PromptService promptService) {
        this.assistant = assistant;
        this.promptService = promptService;
    }

    public String generateSql(String question, DbSchema schema) {
        String schemaText = schema.tables().stream()
                .map(this::formatTable)
                .collect(Collectors.joining("\n"));

        String rules = promptService != null ? promptService.getSqlAssistantRules() : """
            You are a helpful data analyst.
            Generate a single ANSI SQL SELECT statement to answer the user's question.
            Rules:
            - ONLY output a valid SQL SELECT statement, no explanation, no semicolon, no backticks.
            - Always include SELECT and FROM clauses in your query.
            - Use only the tables and columns from the provided schema.
            - Do NOT perform INSERT/UPDATE/DELETE/DDL. Only generate SELECT queries.
            - The query must be a complete, valid SQL statement that can be executed directly.
            - Don't include words like SQL
            """;

        String version = promptService != null ? promptService.getSqlAssistantRulesVersion() : "unknown";
        if (log.isDebugEnabled()) {
            log.debug("Using SQL assistant rules version {}", version);
        }

        String raw = assistant.answer(rules, schemaText, question);
        if (raw == null) raw = "";
        raw = raw.trim();

        // Show raw with newlines escaped so logs reveal invisible structure
        log.error("Raw SQL candidate from assistant: {}", raw.replaceAll("\\r?\\n", "\\\\n"));

        String cleaned = cleanSqlCandidateRobust(raw);
        if (cleaned == null || cleaned.isBlank()) {
            log.error("Failed to extract SQL from assistant output.");
            throw new IllegalArgumentException("Assistant did not return a SQL SELECT statement");
        }

        // Ensure SELECT begins the cleaned string
        String leading = cleaned.stripLeading();
        if (!leading.regionMatches(true, 0, "SELECT", 0, 6)) {
            log.error("Cleaned SQL does not start with SELECT: {}", cleaned.replaceAll("\\r?\\n", "\\\\n"));
            throw new IllegalArgumentException("Cleaned SQL does not start with SELECT");
        }

        // Normalize whitespace: collapse all consecutive whitespace (including newlines) into single spaces
        // This helps JSqlParser handle multi-line SQL that some LLMs may generate
        String normalized = cleaned.replaceAll("\\s+", " ").trim();
        log.error("SQL after whitespace normalization: {}", normalized);

        // Validate with JSqlParser (append semicolon for parsing)
        try {
            Statement stmt = CCJSqlParserUtil.parse(normalized + ";");
            if (!(stmt instanceof Select)) {
                log.error("Parsed statement is not a SELECT: {}", stmt.getClass().getSimpleName());
                throw new IllegalArgumentException("Generated SQL is not a SELECT statement");
            }
        } catch (NoClassDefFoundError | Exception e) {
            // If parser not on classpath or parse fails, log and rethrow parse errors
            log.warn("SQL validation skipped or failed: {}", e.getMessage());
            if (e instanceof net.sf.jsqlparser.JSQLParserException) {
                throw new IllegalArgumentException("Generated SQL failed validation: " + e.getMessage(), e);
            }
        }

        log.error("SQL after cleaning: {}", normalized.replaceAll("\\r?\\n", "\\\\n"));
        return normalized;
    }

    /**
     * Robust extractor:
     * - If a fenced block exists (``` or ~~~), extract its inner content first.
     * - Remove BOM / invisible characters and inline backticks.
     * - Extract the first SELECT block (up to semicolon or end).
     * - Return SELECT statement WITHOUT trailing semicolon.
     */
    private String cleanSqlCandidateRobust(String raw) {
        if (raw == null) return null;
        String s = raw;

        // Normalize newlines
        s = s.replace("\r\n", "\n").replace("\r", "\n");
        log.error("S initial (escaped) {}", s.replaceAll("\\r?\\n", "\\\\n"));

        // 1) If there's a fenced backtick block, prefer its inner content
        Matcher mFence = FENCED_BACKTICK_CAPTURE.matcher(s);
        if (mFence.find()) {
            s = mFence.group(1).trim();
            log.error("Extracted from backtick fence (escaped) {}", s.replaceAll("\\r?\\n", "\\\\n"));
        } else {
            // 1b) try tilde fences
            Matcher mTilde = FENCED_TILDE_CAPTURE.matcher(s);
            if (mTilde.find()) {
                s = mTilde.group(1).trim();
                log.error("Extracted from tilde fence (escaped) {}", s.replaceAll("\\r?\\n", "\\\\n"));
            }
        }

        // 2) Remove BOM / invisible characters
        s = INVISIBLE_CHARS.matcher(s).replaceAll("");
        log.error("After removing invisible chars (escaped) {}", s.replaceAll("\\r?\\n", "\\\\n"));

        // 3) Keep backticks for MySQL identifier quoting - do NOT remove them
        // s = s.replace("`", "");  // COMMENTED OUT - backticks are needed for MySQL
        log.error("After backtick processing (escaped) {}", s.replaceAll("\\r?\\n", "\\\\n"));

        // 4) Try to extract the first SELECT block (up to semicolon or end)
        Matcher mSelect = SELECT_BLOCK.matcher(s);
        if (mSelect.find()) {
            String selectBlock = mSelect.group(1).trim();
            if (selectBlock.endsWith(";")) selectBlock = selectBlock.substring(0, selectBlock.length() - 1).trim();
            // strip control chars except newline and tab
            selectBlock = selectBlock.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "").trim();
            log.error("Extracted SELECT block (escaped) {}", selectBlock.replaceAll("\\r?\\n", "\\\\n"));
            return selectBlock;
        }

        // 5) Fallback: find the first "SELECT" substring and take everything from there
        int idx = s.toUpperCase().indexOf("SELECT");
        if (idx >= 0) {
            String fallback = s.substring(idx).trim();
            if (fallback.endsWith(";")) fallback = fallback.substring(0, fallback.length() - 1).trim();
            fallback = fallback.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "").trim();
            log.error("Fallback extracted from SELECT index (escaped) {}", fallback.replaceAll("\\r?\\n", "\\\\n"));
            return fallback;
        }

        log.error("No SELECT found after cleaning (escaped) {}", s.replaceAll("\\r?\\n", "\\\\n"));
        return null;
    }

    private String formatTable(TableSchema t) {
        String cols = t.columns().stream()
                .map(c -> "`" + c.name() + "` " + c.type())
                .collect(Collectors.joining(", "));
        StringBuilder sb = new StringBuilder();
        sb.append("- `").append(t.name()).append("`(").append(cols).append(")");
        if (t.primaryKeys() != null && !t.primaryKeys().isEmpty()) {
            String quotedPKs = t.primaryKeys().stream()
                    .map(pk -> "`" + pk + "`")
                    .collect(Collectors.joining(", "));
            sb.append("\n  PK(").append(quotedPKs).append(")");
        }
        if (t.foreignKeys() != null && !t.foreignKeys().isEmpty()) {
            String fkText = t.foreignKeys().stream()
                    .map(f -> "`" + f.fromColumn() + "` -> `" + f.toTable() + "`.`" + f.toColumn() + "`")
                    .collect(Collectors.joining(", "));
            sb.append("\n  FK(").append(fkText).append(")");
        }
        return sb.toString();
    }
}