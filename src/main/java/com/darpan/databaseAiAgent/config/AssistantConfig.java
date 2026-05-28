package com.darpan.databaseAiAgent.config;

import com.darpan.databaseAiAgent.llm.SqlAssistant;
import com.darpan.databaseAiAgent.llm.ResultSummarizer;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.SessionScope;

@Configuration
public class AssistantConfig {

	@Bean
	@SessionScope
	public SqlAssistant sqlAssistant(ChatModel model, ChatMemory chatMemory) {
		return AiServices.builder(SqlAssistant.class)
				.chatModel(model)
				.chatMemory(chatMemory)
				.build();
	}

	@Bean
	@SessionScope
	public ResultSummarizer resultSummarizer(ChatModel model) {
		return AiServices.builder(ResultSummarizer.class)
				.chatModel(model)
				.build();
	}
}


