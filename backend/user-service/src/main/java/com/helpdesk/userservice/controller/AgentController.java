package com.helpdesk.userservice.controller;

import com.helpdesk.userservice.dto.UserResponse;
import com.helpdesk.userservice.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
public class AgentController {

    private final UserService userService;

    public AgentController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAgents() {
        return ResponseEntity.ok(userService.getAgents());
    }
}
