package com.sti.accounting.controllers;

import com.sti.accounting.models.AccountingJournalRequest;
import com.sti.accounting.models.AccountingJournalResponse;
import com.sti.accounting.models.AccountingPeriodRequest;
import com.sti.accounting.models.AccountingPeriodResponse;
import com.sti.accounting.services.AccountingJournalService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounting-journal")
public class AccountingJournalController {

    private final AccountingJournalService accountingJournalService;

    public AccountingJournalController(AccountingJournalService accountingJournalService) {
        this.accountingJournalService = accountingJournalService;
    }

    @GetMapping()
    public List<AccountingJournalResponse> getAllAccountingJournal() {
        return accountingJournalService.getAllAccountingJournal();
    }

    @GetMapping("/{id}")
    public AccountingJournalResponse getAccountingJournalById(@PathVariable Long id) {
        return accountingJournalService.getAccountingJournalById(id);
    }

    @PostMapping
    public AccountingJournalResponse createAccountingJournal(@Validated @RequestBody AccountingJournalRequest accountingJournalRequest) {
        return accountingJournalService.createAccountingJournal(accountingJournalRequest);
    }

    @PutMapping("/{id}")
    public AccountingJournalResponse updateAccountingJournal(@PathVariable("id") Long id, @Validated @RequestBody AccountingJournalRequest accountingJournalRequest) {
        return accountingJournalService.updateAccountingJournal(id, accountingJournalRequest);
    }

}
