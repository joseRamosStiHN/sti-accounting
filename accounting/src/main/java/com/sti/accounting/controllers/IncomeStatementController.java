package com.sti.accounting.controllers;

import com.sti.accounting.models.IncomeStatementResponse;
import com.sti.accounting.services.IncomeStatementService;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/v1/income-statement")
public class IncomeStatementController {

    private final IncomeStatementService incomeStatementService;

    public IncomeStatementController(IncomeStatementService incomeStatementService) {
        this.incomeStatementService = incomeStatementService;
    }

    @GetMapping()
    public List<IncomeStatementResponse> getIncomeStatement(@RequestParam(required = false) Long periodId) {
        return incomeStatementService.getIncomeStatement(periodId);
    }
}
