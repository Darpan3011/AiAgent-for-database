# Database AI Agent Module

## Overview
The Database AI Agent Module is a powerful Spring Boot starter that enables natural language interaction with your database. It uses Large Language Models (LLMs) to convert user questions into SQL queries, executes them, and returns the results in a human-readable format.

## Features
- **Natural Language to SQL:** Automatically converts English questions into valid SQL queries.
- **Multi-Provider Support:** Supports OpenAI, Google Gemini, and local Ollama models.
- **Schema Awareness:** intelligently creates prompts based on your database schema (with exclusion options).
- **Conversation Memory:** Maintains chat history for context-aware follow-up questions.
- **Safety Mechanisms:** Read-only transaction support and configurable exclusions for sensitive tables/columns.

How to use this as a dependency in the project?

First download the GitHub repository and do mvn clean install (so jar will be created in the .m2 location)

Add in the parent project’s pom.xml file:

```xml
<dependency>
    <groupId>com.darpan</groupId>
    <artifactId>darpan-ai-database-agent</artifactId>
    <version>2.0.0</version>
</dependency>
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>8.0.33</version>
    <scope>runtime</scope>
</dependency>
```

Allow `@ComponentScan` in parent project’s main class

```java
@ComponentScan(basePackages = {
        "com.darpan.databaseAiAgent"
})
```

Add in properties or yml file in the parent project:

Setup database connectivity with spring datasource (url, username, password etc.)

Mention the provider that we want to use like openai, gemini or ollama local and provide key and model-name as mentioned in the below code

```properties
# MySQL Database Configuration
spring.datasource.url=
spring.datasource.username=
spring.datasource.password=
spring.datasource.driver-class-name=

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=
spring.jpa.show-sql=
spring.jpa.properties.hibernate.format_sql=
spring.jpa.properties.hibernate.dialect=
spring.jpa.properties.hibernate.jdbc.time_zone=

#AI provider configuration
#Supported values: openai (default) or ollama or gemini
ai.db.agent.model-provider=gemini

#OpenAI configuration (used when provider=openai)
#langchain4j.openai.api-key=your-openai-api-key
#langchain4j.openai.chat-model.model-name=gpt-4o
#
#Ollama configuration (used when provider=ollama)
#langchain4j.ollama.base-url=http://localhost:11434
#langchain4j.ollama.chat-model.model-name=llama3.2:latest
#
#Gemini configuration (used when provider=gemini)
langchain4j.google-ai-gemini.api-key=
langchain4j.google-ai-gemini.chat-model.model-name=

# --- Agent memory ---
ai.db.agent.memory.max-messages=
ai.db.schema.exclude.tables=
ai.db.schema.exclude.columns=
```

Use in the controller in the parents’ project

```java
@RestController
@Slf4j
@RequestMapping("/ai")
public class AiController {
    private final SessionAgentService agentService;

    public AiController(SessionAgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/ask")
    public ResponseEntity<?> ask(@RequestBody String question) {
        AgentResponse resp = agentService.ask(question);
        if (!resp.isOk()) return ResponseEntity.badRequest().body(Map.of("error", resp.getError()));
        return ResponseEntity.ok(Map.of(
                "answer", resp.getAnswer(),
                "sql", resp.getSql(),
                "rows", resp.getRows()
        ));
    }

    @GetMapping("/context")
    public ResponseEntity<?> getContext() {
        return ResponseEntity.ok(Map.of("messages", agentService.getChatHistory()));
    }

    @DeleteMapping("/context")
    public ResponseEntity<?> clearContext() {
        agentService.clearContext();
        return ResponseEntity.ok(Map.of("message", "Context cleared"));
    }
}
```
