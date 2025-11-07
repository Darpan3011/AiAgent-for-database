package com.darpan.databaseAiAgent.controller;

import com.darpan.databaseAiAgent.api.AgentResponse;
import com.darpan.databaseAiAgent.service.SessionAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/aidb")
@Slf4j
public class AiDbController {

    private final SessionAgentService agentService;

    public AiDbController(SessionAgentService agentService) {
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
}