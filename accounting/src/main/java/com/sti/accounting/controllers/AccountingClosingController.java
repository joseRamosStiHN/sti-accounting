package com.sti.accounting.controllers;

import com.sti.accounting.models.AccountingClosingResponse;
import com.sti.accounting.services.AccountingClosingService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/v1/accounting-closing")
public class AccountingClosingController {

    private final AccountingClosingService accountingClosingService;

    public AccountingClosingController(AccountingClosingService accountingClosingService) {
        this.accountingClosingService = accountingClosingService;
    }

    @GetMapping()
    public AccountingClosingResponse getAccountingClosing() {
        return accountingClosingService.getAccountingClosing();
    }
}
