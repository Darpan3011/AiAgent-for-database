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
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
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
     * Main entry: caller provides the question. We persist both the user question and assistant answer
     * into the session ChatMemory so that old context is available for subsequent turns.
     */
    public AgentResponse ask(String question) {
        // Load schema
        DbSchema schema = schemaLoader.loadSchema();

        // Build textual context for the SQL prompt from chatMemory.messages()
        List<String> ctx = serializeMessages();

        // Add current user turn to memory
        chatMemory.add(UserMessage.from(question));

        // Generate SQL
        String sql = sqlGenerator.generateSql(question, schema, ctx);

        // Validate
        ValidationResult vr = sqlValidator.validate(sql, schema);
        if (!vr.isOk()) {
            chatMemory.add(AiMessage.from("Validation failed: " + vr.getMessage()));
            return AgentResponse.error(vr.getMessage());
        }

        // Execute
        QueryResult rows = sqlExecutor.execute(sql);

        // Format natural-language answer
        String answer = resultFormatter.format(question, sql, rows);

        // Persist assistant answer
        chatMemory.add(AiMessage.from(answer));

        // Return structured response
        return AgentResponse.ok(answer, sql, rows);
    }

    // Exact serialization used for LLM context
    private List<String> serializeMessages() {
        return chatMemory.messages().stream()
                .map(m -> m.type() + ": " + m.text())
                .collect(Collectors.toList());
    }

    // Expose the exact same lines used for the LLM context
    public List<String> getChatHistory() {
        return serializeMessages();
    }
}