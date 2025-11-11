package com.darpan.databaseAiAgent.config;

import com.darpan.databaseAiAgent.llm.SqlAssistant;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.annotation.SessionScope;

@Configuration
public class AssistantConfig {

	@Bean
	@SessionScope
	public SqlAssistant sqlAssistant(ChatLanguageModel model, ChatMemory chatMemory) {
		return AiServices.builder(SqlAssistant.class)
				.chatLanguageModel(model)
				.chatMemory(chatMemory)
				.build();
	}
}


