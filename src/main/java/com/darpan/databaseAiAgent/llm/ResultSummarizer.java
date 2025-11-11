package com.darpan.databaseAiAgent.llm;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface ResultSummarizer {

	@SystemMessage("You are a data analyst. Summarize the query result succinctly in 1-2 sentences, directly answering the user's question without extra context.")
	String summarize(@UserMessage String prompt);
}