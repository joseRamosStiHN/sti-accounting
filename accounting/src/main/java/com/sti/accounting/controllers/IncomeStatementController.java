package com.sti.accounting.controllers;

import com.sti.accounting.models.IncomeStatementResponse;
import com.sti.accounting.services.IncomeStatementService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/v1/income-statement")
public class IncomeStatementController {

    private final IncomeStatementService incomeStatementService;

    public IncomeStatementController(IncomeStatementService incomeStatementService) {
        this.incomeStatementService = incomeStatementService;
    }

    @GetMapping()
    public IncomeStatementResponse getIncomeStatement() {
        return incomeStatementService.getIncomeStatement();
    }
}
