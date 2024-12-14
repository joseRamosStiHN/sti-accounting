package com.sti.accounting.controllers;

import com.sti.accounting.models.TrialBalanceResponse;
import com.sti.accounting.services.TrialBalanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/trial-balance")
public class TrialBalanceController {

    private final TrialBalanceService trialBalanceService;

    public TrialBalanceController(TrialBalanceService trialBalanceService) {
        this.trialBalanceService = trialBalanceService;
    }

    @GetMapping()
    public TrialBalanceResponse getTrialBalance() {
        return trialBalanceService.getTrialBalance();
    }

}
