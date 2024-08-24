package com.sti.accounting.controllers;

import com.sti.accounting.models.AccountRequest;
import com.sti.accounting.models.AccountResponse;
import com.sti.accounting.models.AccountingPeriodRequest;
import com.sti.accounting.models.AccountingPeriodResponse;
import com.sti.accounting.services.AccountingPeriodService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounting-periods")
public class AccountingPeriodController {

    private final AccountingPeriodService accountingPeriodService;

    public AccountingPeriodController(AccountingPeriodService accountingPeriodService) {
        this.accountingPeriodService = accountingPeriodService;
    }

    @GetMapping()
    public List<AccountingPeriodResponse> getAllAccountingPeriod() {
        return accountingPeriodService.getAllAccountingPeriod();
    }

    @GetMapping("/{id}")
    public AccountingPeriodResponse getAccountingPeriodById(@PathVariable Long id) {
        return accountingPeriodService.getById(id);
    }

    @PostMapping
    public AccountingPeriodResponse createAccountingPeriod(@Validated @RequestBody AccountingPeriodRequest accountingPeriodRequest) {
        return accountingPeriodService.createAccountingPeriod(accountingPeriodRequest);
    }

    @PutMapping("/{id}")
    public AccountingPeriodResponse updateAccountingPeriod(@PathVariable("id") Long id, @Validated @RequestBody AccountingPeriodRequest accountingPeriodRequest) {
        return accountingPeriodService.updateAccountingPeriod(id, accountingPeriodRequest);
    }

    @DeleteMapping("/{id}")
    public AccountingPeriodResponse deleteAccountingPeriod(@PathVariable("id") Long id) {
        return accountingPeriodService.deleteAccountingPeriod(id);
    }
}
