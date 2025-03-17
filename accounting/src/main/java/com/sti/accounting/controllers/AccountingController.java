package com.sti.accounting.controllers;


import com.sti.accounting.entities.AccountEntity;
import com.sti.accounting.models.AccountCategory;
import com.sti.accounting.models.AccountRequest;
import com.sti.accounting.models.AccountResponse;
import com.sti.accounting.models.AccountType;
import com.sti.accounting.services.AccountService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/v1/accounts")

public class AccountingController {


    private final AccountService accountService;


    public AccountingController(AccountService accountService) {
        this.accountService = accountService;

    }

    @GetMapping()
    public List<AccountResponse> getAccounts() {
        return accountService.getAllAccount();
    }

    @GetMapping("/categories")
    public List<AccountCategory> getAllCategories() {
        return accountService.getAllCategories();
    }

    @GetMapping("/account-type")
    public List<AccountType> getAllAccountType() {
        return accountService.getAllAccountType();
    }

    @GetMapping("/{id}")
    public AccountResponse getAccountById(@PathVariable Long id) {
        return accountService.getById(id);
    }

    @PostMapping
    public AccountResponse createAccount(@Validated @RequestBody AccountRequest accountRequest) {
        return accountService.createAccount(accountRequest);
    }

    @PutMapping("/{id}")
    public AccountResponse updateAccount(@PathVariable("id") Long id, @Validated @RequestBody AccountRequest accountRequest) {
        return accountService.updateAccount(id, accountRequest);
    }

    @PostMapping("/clone")
    public void cloneCatalog(@RequestParam(required = false) String sourceTenantId) {
        accountService.cloneCatalog(sourceTenantId);
    }

}
