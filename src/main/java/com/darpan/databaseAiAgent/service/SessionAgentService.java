package com.darpan.databaseAiAgent.service;

import com.darpan.databaseAiAgent.api.AgentResponse;
import com.darpan.databaseAiAgent.api.DbSchema;
import com.darpan.databaseAiAgent.api.QueryResult;
import com.darpan.databaseAiAgent.api.ValidationResult;
import com.darpan.databaseAiAgent.format.LlmResultFormatter;
import com.darpan.databaseAiAgent.schema.JdbcSchemaLoader;
import com.darpan.databaseAiAgent.sql.JsqlparserSqlValidator;
import com.darpan.databaseAiAgent.sql.LlmSqlGenerator;
import com.darpan.databaseAiAgent.sql.SafeJdbcExecutor;
import dev.langchain4j.memory.ChatMemory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@SessionScope
public class SessionAgentService {

    private final ChatMemory chatMemory;         // session-scoped memory
    private final JdbcSchemaLoader schemaLoader;
    private final LlmSqlGenerator sqlGenerator;
    private final JsqlparserSqlValidator sqlValidator;
    private final SafeJdbcExecutor sqlExecutor;
    private final LlmResultFormatter resultFormatter;

    public SessionAgentService(ChatMemory chatMemory,
                               JdbcSchemaLoader schemaLoader,
                               LlmSqlGenerator sqlGenerator,
                               JsqlparserSqlValidator sqlValidator,
                               SafeJdbcExecutor sqlExecutor,
                               LlmResultFormatter resultFormatter) {
        this.chatMemory = chatMemory;
        this.schemaLoader = schemaLoader;
        this.sqlGenerator = sqlGenerator;
        this.sqlValidator = sqlValidator;
        this.sqlExecutor = sqlExecutor;
        this.resultFormatter = resultFormatter;
    }

    /**
     * Main entry: caller (controller) only provides the question. The session's AiServices proxy
     * will automatically append user and assistant messages into the session ChatMemory.
     */
    public AgentResponse ask(String question) {
        // Load schema
        DbSchema schema = schemaLoader.loadSchema();

        // Build textual context for the SQL prompt from chatMemory.messages() if needed
        List<String> ctx = chatMemory.messages().stream()
                .map(m -> m.type() + ": " + m.text())
                .collect(Collectors.toList());

        // Generate SQL by calling the agent proxy (AiServices will append user message and assistant reply)
        String sql = sqlGenerator.generateSql(question, schema, ctx);

        // Validate
        ValidationResult vr = sqlValidator.validate(sql, schema);
        if (!vr.ok()) {
            // Note: AiServices already stored user/assistant messages at the time of generation
            return AgentResponse.error(vr.message());
        }

        // Execute
        QueryResult rows = sqlExecutor.execute(sql);

        // Format natural-language answer
        String answer = resultFormatter.format(question, sql, rows);

        // Return structured response
        return AgentResponse.ok(answer, sql, rows);
    }

    public List<Map<String, String>> getChatHistory() {
        return chatMemory.messages().stream()
                .map(message -> Map.of(
                        "role", message.type().name(),
                        "content", message.text()
                ))
                .collect(Collectors.toList());
    }
}