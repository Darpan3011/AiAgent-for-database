package com.darpan.databaseAiAgent.llm;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface SqlAssistant {

	@SystemMessage("{{rules}}\n\nSchema:\n{{schema}}")
	String answer(@V("rules") String rules, @V("schema") String schema, @UserMessage String question);
}


