package com.sti.accounting.controllers;

import com.sti.accounting.models.SeniorAccountantsResponse;
import com.sti.accounting.services.SeniorAccountantsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/senior-accountants")
public class SeniorAccountantsController {

    private final SeniorAccountantsService seniorAccountantsService;

    public SeniorAccountantsController(SeniorAccountantsService seniorAccountantsService) {
        this.seniorAccountantsService = seniorAccountantsService;
    }

    @GetMapping("")
    public ResponseEntity<SeniorAccountantsResponse> getSeniorAccountants() {
        SeniorAccountantsResponse response = seniorAccountantsService.getSeniorAccountants();
        return ResponseEntity.ok(response);
    }
}
