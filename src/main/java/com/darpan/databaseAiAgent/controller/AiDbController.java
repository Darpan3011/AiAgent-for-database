package com.darpan.databaseAiAgent.controller;

import com.darpan.databaseAiAgent.service.SessionAgentService;
import com.darpan.databaseAiAgent.api.AgentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
        if (!resp.success()) return ResponseEntity.badRequest().body(java.util.Map.of("error", resp.error()));
        return ResponseEntity.ok(java.util.Map.of("answer", resp.answer(), "sql", resp.sql(), "rows", resp.rows()));
    }

    @GetMapping("/context")
    public ResponseEntity<?> getContext() {
        List<Map<String, String>> chatHistory = agentService.getChatHistory();
        return ResponseEntity.ok(Map.of("messages", chatHistory));
    }
}