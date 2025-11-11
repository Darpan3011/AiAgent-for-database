package com.darpan.databaseAiAgent.config;

import com.darpan.databaseAiAgent.llm.SqlAssistant;
import com.darpan.databaseAiAgent.llm.ResultSummarizer;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.SessionScope;

@Configuration
public class AssistantConfig {

	@Bean
	@SessionScope
	public SqlAssistant sqlAssistant(OpenAiChatModel model, ChatMemory chatMemory) {
		return AiServices.builder(SqlAssistant.class)
				.chatModel(model)
				.chatMemory(chatMemory)
				.build();
	}

	@Bean
	@SessionScope
	public ResultSummarizer resultSummarizer(OpenAiChatModel model, ChatMemory chatMemory) {
		return AiServices.builder(ResultSummarizer.class)
				.chatModel(model)
				.chatMemory(chatMemory)
				.build();
	}
}


