package com.sti.accounting.controllers;

import com.sti.accounting.models.AccountingPeriodDataResponse;
import com.sti.accounting.services.JournalEntryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/journal-entry")
public class JournalEntryController {

    private final JournalEntryService journalEntryService;

    public JournalEntryController(JournalEntryService journalEntryService) {
        this.journalEntryService = journalEntryService;
    }

    @GetMapping("")
    public ResponseEntity<AccountingPeriodDataResponse> getJournalEntry() {
        AccountingPeriodDataResponse response = journalEntryService.getJournalEntry();
        return ResponseEntity.ok(response);
    }
}
