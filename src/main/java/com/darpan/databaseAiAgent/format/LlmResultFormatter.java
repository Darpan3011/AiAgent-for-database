package com.darpan.databaseAiAgent.format;

import com.darpan.databaseAiAgent.api.QueryResult;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class LlmResultFormatter {

    private final ChatLanguageModel model;

    public LlmResultFormatter(ChatLanguageModel model) {
        this.model = model;
    }

    public String format(String question, String sql, QueryResult result) {
        if (result == null || result.rows() == null || result.rows().isEmpty()) {
            return "I didn't find any matching rows for: " + question;
        }
        if (result.columns().size() == 1 && result.rows().size() == 1) {
            Object val = result.rows().get(0).get(result.columns().get(0));
            return "Answer: " + (val == null ? "null" : val.toString());
        }

        List<Map<String, Object>> previewRows = result.rows().stream().limit(10).toList();
        String header = String.join(" | ", result.columns());
        String body = previewRows.stream()
                .map(r -> result.columns().stream()
                        .map(c -> String.valueOf(r.get(c)))
                        .collect(Collectors.joining(" | ")))
                .collect(Collectors.joining("\n"));

        String prompt = "You are a data analyst. Summarize the query result succinctly in 1-2 sentences, directly answering the question.\n" +
                "Question: " + question + "\n" +
                "SQL: " + sql + "\n" +
                "Preview (" + previewRows.size() + " rows shown):\n" +
                header + "\n" + body + "\n" +
                "Answer succinctly without adding extra context.";

        try {
            return model.generate(prompt).trim();
        } catch (Exception e) {
            return "Found " + result.rows().size() + " rows. Showing first few: \n" + header + "\n" + body;
        }
    }
}