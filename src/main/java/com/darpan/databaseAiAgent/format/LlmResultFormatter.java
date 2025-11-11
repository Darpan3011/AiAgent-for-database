package com.darpan.databaseAiAgent.format;

import com.darpan.databaseAiAgent.api.QueryResult;
import com.darpan.databaseAiAgent.llm.ResultSummarizer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class LlmResultFormatter {

    private final ResultSummarizer summarizer;

    public LlmResultFormatter(ResultSummarizer summarizer) {
        this.summarizer = summarizer;
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

        String prompt = "Question: " + question + "\n" +
                "SQL: " + sql + "\n" +
                "Preview (" + previewRows.size() + " rows shown):\n" +
                header + "\n" + body;

        try {
            return summarizer.summarize(prompt).trim();
        } catch (Exception e) {
            return "Found " + result.rows().size() + " rows. Showing first few: \n" + header + "\n" + body;
        }
    }
}