# databaseAiAgent — README for Developers

**One-line:** A Spring Boot reusable module that provides a session-scoped AI assistant for natural-language → safe SQL generation, execution (read-only), and natural-language result formatting.

---

## What this module does

- Accepts natural-language questions about a database.
- Uses a configurable LLM (OpenAI, Ollama, or Gemini via langchain4j) to generate SQL queries.
- Validates SQL with JSQLParser-based validator and only allows `SELECT` queries.
- Executes queries using `JdbcTemplate` and returns structured `QueryResult` plus a natural language answer.
- Keeps a per-HTTP-session conversation memory so follow-ups and clarifications are possible.

This module is built to be dropped into any Spring Boot application as a dependency (jar) and to auto-configure itself when on the classpath.

---

## Quick integration steps

### 1) Add the module to your project

**Option A — Install locally and add as Maven dependency**
If you received the module as a source jar or project:

```bash
# from inside the module project
mvn clean install
```

Then add to the host project's `pom.xml`:

```xml
<dependency>
  <groupId>com.darpan</groupId>
  <artifactId>databaseAiAgent</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</dependency>
```

**Option B — Include as a git submodule or multi-module**
Add the module as a Maven module or move the module's source into your project and ensure the module is built with your parent pom.

**Option C — Add compiled jar to classpath**
Put `databaseAiAgent-0.0.1-SNAPSHOT.jar` on the classpath — the module uses Spring Boot `@AutoConfiguration` and will auto-configure when present.


### 2) Required host application setup

The host application must provide:

- A configured `DataSource` and `JdbcTemplate`. The module uses Spring `JdbcTemplate` internally. Add `spring-boot-starter-jdbc` or `spring-boot-starter-data-jpa` and provide standard Spring `spring.datasource.*` properties.
- A web context (the module uses `@SessionScope` for the assistant). If your app is not a web application, either change scope or provide a session-like scope.

Example minimal `pom.xml` dependencies for host app (besides this module):

```xml
<dependency> <!-- data access -->
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
<dependency> <!-- web (for session scope) -->
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```


### 3) Add required configuration properties

The module ships with commented example properties for LLM configuration. Add any LLM provider credentials and DB configuration in your host application's `application.properties` or `application.yml`.

**Important properties (examples)**

```properties
# LLM provider selection (optional; default is OpenAI if configured via langchain4j)
# ai.db.agent.model-provider=openai

# OpenAI (langchain4j) — provide your API key
langchain4j.openai.api-key=YOUR_OPENAI_API_KEY
langchain4j.openai.chat-model.model-name=gpt-4o

# Ollama (if you choose provider=ollama)
#langchain4j.ollama.base-url=http://localhost:11434
#langchain4j.ollama.chat-model.model-name=llama3.2:latest

# Gemini (if you choose provider=gemini)
#langchain4j.google-ai-gemini.api-key=your-gemini-api-key
#langchain4j.google-ai-gemini.chat-model.model-name=gemini-2.0-flash-001

# Standard Spring datasource (example)
spring.datasource.url=jdbc:postgresql://db-host:5432/yourdb
spring.datasource.username=dbuser
spring.datasource.password=secret
spring.datasource.driver-class-name=org.postgresql.Driver

# (Optional) application name used by the module
spring.application.name=databaseAiAgent
```

> **Security note:** Keep API keys and DB credentials out of version control. Use environment variables, Spring Cloud Config, Vault, or platform secrets.


### 4) How to call the module from your app code

The module exposes a session-scoped service `SessionAgentService` (bean name: `sessionAgentService` — inject by type). Typical usage in one of your controllers or services:

```java
import com.darpan.databaseAiAgent.service.SessionAgentService;
import com.darpan.databaseAiAgent.api.AgentResponse;

@RestController
@RequestMapping("/ai")
public class MyAiController {
    private final SessionAgentService agentService;

    public MyAiController(SessionAgentService agentService) {
        this.agentService = agentService;
    }

    @GetMapping("/ask")
    public AgentResponse ask(@RequestParam String q) {
        return agentService.ask(q); // returns structured AgentResponse (answer, sql, rows)
    }

    @DeleteMapping("/context")
    public void clear() {
        agentService.clearContext(); // Clears session memory
    }
}
```

`AgentResponse` contains at least: natural language `answer`, the `sql` generated, and the `QueryResult` rows.


## Behavior & safety rules (important)

- The module **only allows SELECT** queries. `SafeJdbcExecutor` enforces this and throws `IllegalArgumentException` if non-`SELECT` queries are attempted.
- The SQL validator uses JSQLParser to parse and (basic) validate SQL before executing.
- The module stores conversation context in session-scoped memory (so that follow-up questions are possible). If your app uses stateless sessions, memory will be lost across requests.
- The module uses `langchain4j` model wrappers; the active provider must be configured (OpenAI, Ollama, or Gemini).


## Configurable / override points

If you want to replace or customize behavior, the module is written with Spring components and beans that can be overridden by the application:

- `SqlAssistant` / LLM related beans (defined in `AssistantConfig`) — provide your own bean definitions to swap model or behavior.
- `LlmSqlGenerator` and `JsqlparserSqlValidator` — you can implement and provide your own beans if you need different SQL generation or validation logic.
- `SafeJdbcExecutor` — component class responsible for actual execution; replace if you need different execution semantics (pagination, timeouts, read-replicas, etc.).
- `PromptService` — loads prompt templates from `classpath:/prompts/`. Modify templates or override this bean to change prompts.


## Files & important classes (map)

- `src/main/java/com/darpan/databaseAiAgent/config/DatabaseAiAgentAutoConfiguration.java`
  - Enables auto-configuration when the jar is on classpath.
- `src/main/java/com/darpan/databaseAiAgent/config/AssistantConfig.java`
  - Beans to wire LLM assistant and summarizer (session-scoped).
- `src/main/java/com/darpan/databaseAiAgent/service/SessionAgentService.java`
  - Main entry point. Session-scoped service that accepts questions and returns `AgentResponse`.
- `src/main/java/com/darpan/databaseAiAgent/sql/SafeJdbcExecutor.java`
  - Executes only `SELECT` queries through `JdbcTemplate`.
- `src/main/java/com/darpan/databaseAiAgent/sql/LlmSqlGenerator.java`
  - Generates SQL from natural language using the `SqlAssistant`.
- `src/main/java/com/darpan/databaseAiAgent/sql/JsqlparserSqlValidator.java`
  - Validates SQL using JSQLParser.
- `src/main/resources/prompts/` — prompt templates used to build LLM context.
- `src/main/resources/META-INF/spring.factories` — registers auto-configuration (so adding the jar is sufficient to enable module).


## Building & testing

- Build jar:

```bash
mvn clean package
# or
mvn clean install
```

- Run module unit tests (if any):

```bash
mvn test
```


## Troubleshooting

**`NoSuchBeanDefinitionException: JdbcTemplate`**
- Ensure you added `spring-boot-starter-jdbc` and configured `spring.datasource.*` in your host application.

**`IllegalArgumentException: Only SELECT queries are allowed`**
- The assistant generated or produced a non-SELECT query; this is intentionally blocked. Check the generated SQL in the `AgentResponse` and adjust prompts or permissions.

**Session-scoped beans not created (when running non-web)**
- The module uses `@SessionScope`. For non-web testing or CLI usage, either change scope to prototype or provide a `RequestContext`/`Session` simulation for bean creation.


## Extending & customizing prompts

Prompts are under `src/main/resources/prompts/`. They are loaded by `PromptService`. To customize how SQL is generated and how the LLM must behave, edit these files or override the `PromptService` bean.


## Security & production considerations

- Sanitize or limit returned result sizes to avoid OOMs — the module returns raw `QueryResult` rows; your API should paginate or limit columns if necessary.
- Limit the LLM capability / tokens — set model names and max tokens according to your quota and cost policy.
- Ensure DB user has read-only access if you want the assistant to never modify data.


## Example `application.properties` snippet (final)

```properties
spring.datasource.url=jdbc:postgresql://db-host:5432/yourdb
spring.datasource.username=dbuser
spring.datasource.password=secret
spring.datasource.driver-class-name=org.postgresql.Driver

# Example: Using OpenAI
langchain4j.openai.api-key=${OPENAI_API_KEY}
langchain4j.openai.chat-model.model-name=gpt-4o

# Example: Using Gemini (uncomment and set provider)
#ai.db.agent.model-provider=gemini
#langchain4j.google-ai-gemini.api-key=${GEMINI_API_KEY}
#langchain4j.google-ai-gemini.chat-model.model-name=gemini-2.0-flash-001

spring.application.name=databaseAiAgent
```

---

If you'd like, I can also:

- produce a short integration checklist you can paste into your project's README,
- generate an `example` module demonstrating end-to-end integration (controller + properties), or
- produce trimmed usage examples for `curl`/REST calls.

Tell me which of these you want next and I will add it directly to this document.

