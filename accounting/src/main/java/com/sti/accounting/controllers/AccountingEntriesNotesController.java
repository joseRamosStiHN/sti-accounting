package com.sti.accounting.controllers;

import com.sti.accounting.models.AccountingEntriesNotesResponse;
import com.sti.accounting.services.AccountingEntriesNotesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounting-entries-notes")
public class AccountingEntriesNotesController {

    private final AccountingEntriesNotesService accountingEntriesNotesService;

    public AccountingEntriesNotesController(AccountingEntriesNotesService accountingEntriesNotesService) {
        this.accountingEntriesNotesService = accountingEntriesNotesService;
    }

    @GetMapping("")
    public ResponseEntity<AccountingEntriesNotesResponse> getAccountingEntriesNotes() {
        return ResponseEntity.ok(accountingEntriesNotesService.getAccountingEntriesNotes());
    }
}
