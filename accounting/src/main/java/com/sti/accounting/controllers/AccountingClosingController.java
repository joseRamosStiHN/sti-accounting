package com.sti.accounting.controllers;

import com.sti.accounting.models.AccountingClosingResponse;
import com.sti.accounting.services.AccountingClosingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/v1/accounting-closing")
public class AccountingClosingController {

    private final AccountingClosingService accountingClosingService;

    public AccountingClosingController(AccountingClosingService accountingClosingService) {
        this.accountingClosingService = accountingClosingService;
    }

    @GetMapping()
    public List<AccountingClosingResponse> getAllAccountingClosing() {
        return accountingClosingService.getAllAccountingClosing();
    }

    @GetMapping("/detail")
    public AccountingClosingResponse getDetailAccountingClosing() {
        return accountingClosingService.getDetailAccountingClosing();
    }

    @PostMapping("/close")
    public ResponseEntity<String> closeAccountingPeriod() {
        try {
            accountingClosingService.closeAccountingPeriod();
            return ResponseEntity.ok("Accounting period closed successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error closing accounting period: " + e.getMessage());
        }
    }
}
