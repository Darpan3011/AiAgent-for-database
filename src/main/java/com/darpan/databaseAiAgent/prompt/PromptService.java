package com.darpan.databaseAiAgent.prompt;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.stream.Collectors;

@Component
public class PromptService {

	private volatile String cachedSqlAssistantRules;
	private volatile String cachedSqlAssistantRulesHash;

	public String getSqlAssistantRules() {
		if (cachedSqlAssistantRules != null) return cachedSqlAssistantRules;
		synchronized (this) {
			if (cachedSqlAssistantRules != null) return cachedSqlAssistantRules;
			cachedSqlAssistantRules = loadFromClasspath("prompts/sql_assistant_rules.txt");
			cachedSqlAssistantRulesHash = sha1Hex(cachedSqlAssistantRules);
			return cachedSqlAssistantRules;
		}
	}

	public String getSqlAssistantRulesVersion() {
		if (cachedSqlAssistantRulesHash == null) {
			getSqlAssistantRules();
		}
		return cachedSqlAssistantRulesHash;
	}

	private String loadFromClasspath(String path) {
		try {
			ClassPathResource res = new ClassPathResource(path);
			if (!res.exists()) {
				return defaultSqlAssistantRules();
			}
			try (InputStream is = res.getInputStream();
			     BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
				String content = br.lines().collect(Collectors.joining("\n"));
				return (content == null || content.isBlank()) ? defaultSqlAssistantRules() : content;
			}
		} catch (IOException e) {
			return defaultSqlAssistantRules();
		}
	}

	private String defaultSqlAssistantRules() {
		return String.join("\n",
				"You are a helpful data analyst.",
				"Generate a single ANSI SQL SELECT statement to answer the user's question.",
				"Rules:",
				"- ONLY output SQL, no explanation.",
				"- Use only the tables and columns from the provided schema.",
				"- Do NOT perform INSERT/UPDATE/DELETE/DDL. SELECT only.");
	}

	private String sha1Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			return "unknown";
		}
	}
}


