package com.sti.accounting.controllers;

import com.sti.accounting.models.LedgerAccountsResponse;
import com.sti.accounting.services.AccountingPeriodService;
import com.sti.accounting.services.LedgerAccountsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ledger-accounts")
public class LedgerAccountsController {

    private final LedgerAccountsService ledgerAccountsService;

    public LedgerAccountsController(LedgerAccountsService ledgerAccountsService) {
        this.ledgerAccountsService = ledgerAccountsService;
    }

    @GetMapping()
    public List<LedgerAccountsResponse> getLedgerAccountsDetail() {
        return ledgerAccountsService.getLedgerAccountsDetail();
    }
}
